package com.glassfitgames.glassfitplatform.gpstracker;

public class TargetNotSetException extends RuntimeException {

	public TargetNotSetException() {
		super();
	}

	public TargetNotSetException(String detailMessage) {
		super(detailMessage);
	}

	public TargetNotSetException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public TargetNotSetException(Throwable throwable) {
		super(throwable);
	}

}