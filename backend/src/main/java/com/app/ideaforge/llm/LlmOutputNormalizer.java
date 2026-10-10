package com.app.ideaforge.llm;

import java.util.Set;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

/**
 * Last-resort normalizer for model output that still violates range
 * constraints after one repair attempt. Only range/limit problems are fixed
 * (truncate strings and trim arrays to the declared {@code max}); anything
 * structural is left for the caller to reject.
 */
@Component
public class LlmOutputNormalizer {

	private final ObjectMapper objectMapper;

	public LlmOutputNormalizer(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public <T> T normalize(T value, Set<ConstraintViolation<T>> violations) {
		JsonNode root = objectMapper.valueToTree(value);
		boolean changed = false;
		for (ConstraintViolation<T> violation : violations) {
			Integer max = maxLimit(violation);
			if (max == null) {
				continue;
			}
			changed |= applyLimit(root, violation.getPropertyPath(), max);
		}
		if (!changed) {
			return value;
		}
		@SuppressWarnings("unchecked")
		Class<T> type = (Class<T>) value.getClass();
		return objectMapper.treeToValue(root, type);
	}

	private Integer maxLimit(ConstraintViolation<?> violation) {
		Object max = violation.getConstraintDescriptor().getAttributes().get("max");
		if (max instanceof Number number) {
			return number.intValue();
		}
		return null;
	}

	/** Truncates a textual node or trims an array node at the violation path. */
	private boolean applyLimit(JsonNode root, Path path, int max) {
		JsonNode parent = root;
		JsonNode last = null;
		int lastIndex = -1;
		String lastName = null;
		for (Path.Node node : path) {
			ElementKind kind = node.getKind();
			if (kind == ElementKind.BEAN) {
				continue;
			}
			parent = last == null ? root : last;
			if (kind == ElementKind.PROPERTY) {
				lastName = node.getName();
				lastIndex = -1;
			} else if (kind == ElementKind.CONTAINER_ELEMENT) {
				lastIndex = node.getIndex() == null ? -1 : node.getIndex();
				lastName = null;
			} else {
				return false;
			}
			last = lastIndex >= 0 ? parent.get(lastIndex) : parent.get(lastName);
			if (last == null) {
				return false;
			}
		}
		if (last == null) {
			return false;
		}
		if (last.isTextual() && last.asString().length() > max) {
			String truncated = truncate(last.asString(), max);
			if (lastIndex >= 0 && parent instanceof ArrayNode array) {
				array.set(lastIndex, StringNode.valueOf(truncated));
			} else if (lastName != null && parent instanceof ObjectNode object) {
				object.put(lastName, truncated);
			} else {
				return false;
			}
			return true;
		}
		if (last.isArray() && last.size() > max) {
			ArrayNode array = (ArrayNode) last;
			while (array.size() > max) {
				array.remove(array.size() - 1);
			}
			return true;
		}
		return false;
	}

	/** Char-boundary-safe truncation; survives surrogate pairs and trims spaces. */
	static String truncate(String value, int max) {
		if (value == null || value.length() <= max) {
			return value;
		}
		int end = max;
		if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
			end--;
		}
		return value.substring(0, end).stripTrailing();
	}
}
