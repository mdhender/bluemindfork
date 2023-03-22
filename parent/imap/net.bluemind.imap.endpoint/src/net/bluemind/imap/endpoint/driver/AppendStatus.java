package net.bluemind.imap.endpoint.driver;

import net.bluemind.imap.endpoint.driver.AppendStatus.WriteStatus;

public record AppendStatus(WriteStatus status, long uidValidity, long imapUid) {

	public enum WriteStatus {

		WRITTEN(" OK [APPENDUID %d %d] APPEND completed"),

		OVERQUOTA_REJECTED(" NO Over quota"),

		EXCEPTIONNALY_REJECTED(" NO Rejected");

		private String message;

		private WriteStatus(String message) {
			this.message = message;
		}

	}

	public String statusName() {
		return (imapUid != 0L ? String.format(status.message, uidValidity, imapUid) : status.message);
	}

}
