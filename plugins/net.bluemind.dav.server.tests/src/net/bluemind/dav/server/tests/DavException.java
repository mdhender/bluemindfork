package net.bluemind.dav.server.tests;

@SuppressWarnings("serial")
public class DavException extends RuntimeException {

	private final int statusCode;
	private final String statusText;

	public DavException(int statusCode, String statusText) {
		super("Code: " + statusCode + ": " + statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusText() {
		return statusText;
	}

}
