package net.bluemind.core.sendmail;

import org.columba.ristretto.smtp.SMTPException;

public class FailedRecipient {
	private static final String SMTP_ERROR_STATUS = "5.0.0";

	public final String recipient;
	public final int code;
	public final String message;
	public final String smtpStatus;

	private FailedRecipient(int code, String recipient, String message, String smtpStatus) {
		this.code = code;
		this.recipient = recipient;
		this.message = message;
		this.smtpStatus = smtpStatus;
	}

	private static FailedRecipient create(int code, String recipient, String message) {
		String status = message.split(" ")[0];
		// rfc3464: status-code = DIGIT "." 1*3DIGIT "." 1*3DIGIT
		if (status.matches("[2,4,5].\\d{1,3}.\\d{1,3}")) {
			message = message.substring(status.length());
		} else {
			status = SMTP_ERROR_STATUS;
		}

		return new FailedRecipient(code, recipient, message, status);
	}

	public static FailedRecipient create(SendmailResponse response, String recipient) {
		return FailedRecipient.create(response.code, recipient, response.message);
	}

	public static FailedRecipient create(SMTPException e, String recipient) {
		return FailedRecipient.create(e.getCode(), recipient, e.getMessage());
	}

	@Override
	public String toString() {
		return String.format("%s (%s)", recipient, message);
	}

}
