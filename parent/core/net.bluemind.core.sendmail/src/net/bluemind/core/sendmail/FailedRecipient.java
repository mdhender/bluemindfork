package net.bluemind.core.sendmail;

public class FailedRecipient {
	public final String recipient;
	public final String message;

	public FailedRecipient(String recipient, String message) {
		this.recipient = recipient;
		this.message = message;
	}

	@Override
	public String toString() {
		return String.format("%s (%s)", recipient, message);
	}

}
