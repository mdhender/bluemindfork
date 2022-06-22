/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

public class MailApiAnnotations {

	private MailApiAnnotations() {
		// ok
	}

	public static final String FOLDER_META = "/vendor/bluemind/folder/meta";

	public static final String MSG_META = "/vendor/bluemind/msg/meta";

	public static final String MSG_ANNOTATION_BUS_TOPIC = "annotation.bluemind.msg.meta";

}
