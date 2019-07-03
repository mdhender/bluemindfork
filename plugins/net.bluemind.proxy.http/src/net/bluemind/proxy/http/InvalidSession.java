package net.bluemind.proxy.http;

@SuppressWarnings("serial")
public class InvalidSession extends RuntimeException {

	public InvalidSession(String msg) {
		super(msg);
	}
}
