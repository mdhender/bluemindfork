/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.api;

import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailboxAnnotation {

	public String mailbox;
	public String userId;
	public String entry;
	public String value;

	/**
	 * <pre>
	 * {
	 * "MBOXNAME" : "test1509123605722.lab!user.user1509123605722",
	 * "ENTRY" : "/vendor/blue-mind/replication/id",
	 * "USERID" : "user1509123605722@test1509123605722.lab",
	 * "VALUE" : "42"
	 * }
	 * </pre>
	 * 
	 * @param annotation
	 * @return
	 */
	public static MailboxAnnotation of(JsonObject annotation) {
		MailboxAnnotation ma = new MailboxAnnotation();
		ma.mailbox = annotation.getString("MBOXNAME");
		ma.userId = annotation.getString("USERID", "");
		ma.entry = annotation.getString("ENTRY");
		ma.value = annotation.getString("VALUE");
		return ma;
	}

	public String toString() {
		return "Annot{mbox: " + mailbox + ", u: " + userId + ", k: " + entry + " => '" + value + "'}";
	}

	/**
	 * %(MBOXNAME ex2016.vmw!user.tom ENTRY /vendor/blue-mind/replication/id USERID
	 * tom@ex2016.vmw VALUE 43)
	 * 
	 * @return
	 */
	public String toParenObjectString() {
		return "%(MBOXNAME " + mailbox + " ENTRY " + entry + " USERID " + (userId.isEmpty() ? "\"\"" : userId)
				+ " VALUE " + (value == null ? "NIL" : value) + ")";
	}

}
