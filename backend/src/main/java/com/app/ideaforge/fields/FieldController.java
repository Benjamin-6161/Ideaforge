package com.app.ideaforge.fields;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.ideaforge.fields.FieldService.GenerationOptionsResponse;

@RestController
@RequestMapping("/api")
public class FieldController {

	private final FieldService fieldService;

	public FieldController(FieldService fieldService) {
		this.fieldService = fieldService;
	}

	@GetMapping("/fields")
	public List<FieldResponse> fields() {
		return fieldService.listFieldsWithNiches();
	}

	@GetMapping("/generation-options")
	public GenerationOptionsResponse generationOptions() {
		return fieldService.generationOptions();
	}
}
