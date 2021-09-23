package net.bluemind.core.sendmail;

public class FailedRecipient {
	private String recipient;
	private String message;

	public FailedRecipient(String recipient, String message) {
		this.recipient = recipient;
		this.message = message;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return String.format("%s (%s)", recipient, message);
	}

}
