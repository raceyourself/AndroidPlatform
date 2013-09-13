package com.glassfitgames.glassfitplatform.gpstracker;

public class LocationNotAvailableException extends RuntimeException {

	public LocationNotAvailableException() {
		super();
	}

	public LocationNotAvailableException(String detailMessage) {
		super(detailMessage);
	}

	public LocationNotAvailableException(String detailMessage,
			Throwable throwable) {
		super(detailMessage, throwable);
	}

	public LocationNotAvailableException(Throwable throwable) {
		super(throwable);
	}

}