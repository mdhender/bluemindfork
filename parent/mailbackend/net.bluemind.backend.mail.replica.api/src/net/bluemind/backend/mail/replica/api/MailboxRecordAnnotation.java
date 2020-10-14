/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailboxRecordAnnotation {

	public String entry;
	public String value;

	/**
	 * <pre>
	 * {
	 * "ENTRY" : "/vendor/cmu/cyrus-imapd/thrid",
	 * "USERID" : "NIL",
	 * "VALUE" : "555fb6a47816a480"
	 * }
	 * </pre>
	 */
	public static MailboxRecordAnnotation of(JsonObject annotation) {
		MailboxRecordAnnotation ma = new MailboxRecordAnnotation();
		ma.entry = annotation.getString("ENTRY");
		ma.value = annotation.getString("VALUE");
		return ma;
	}

	public String toString() {
		return "Annot{u: NIL, k: " + entry + " => '" + value + "'}";
	}

	/**
	 * @return a string like %(ENTRY /vendor/cmu/cyrus-imapd/thrid USERID NIL VALUE
	 *         555fb6a47816a480)
	 */
	public String toParenObjectString() {
		return "%(ENTRY " + entry + " USERID NIL VALUE " + (value == null ? "NIL" : value) + ")";
	}

}
