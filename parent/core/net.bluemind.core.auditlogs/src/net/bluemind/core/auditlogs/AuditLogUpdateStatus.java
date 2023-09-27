/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.core.auditlogs;

public class AuditLogUpdateStatus {

	public String updateMessage;
	public MessageCriticity crit = MessageCriticity.MAJOR;

	public enum MessageCriticity {
		MINOR("minor"), MAJOR("major");

		private String criticity;

		private MessageCriticity(String c) {
			this.criticity = c;
		}

		@Override
		public String toString() {
			return criticity;
		}
	}

	public AuditLogUpdateStatus() {
	}

	public AuditLogUpdateStatus(String updateMessage) {
		this.updateMessage = updateMessage;
	}

	public AuditLogUpdateStatus(String updateMessage, MessageCriticity crit) {
		this.updateMessage = updateMessage;
		this.crit = crit;
	}
}
