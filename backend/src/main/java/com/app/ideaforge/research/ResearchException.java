package com.app.ideaforge.research;

/** Raised when a research fetch cannot be completed. */
public class ResearchException extends RuntimeException {

	public ResearchException(String message) {
		super(message);
	}

	public ResearchException(String message, Throwable cause) {
		super(message, cause);
	}
}
