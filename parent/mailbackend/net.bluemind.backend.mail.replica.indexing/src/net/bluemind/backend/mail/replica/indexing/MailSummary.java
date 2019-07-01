/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012
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
 * END LICENSE
 */

package net.bluemind.backend.mail.replica.indexing;

import java.util.Set;

public class MailSummary {
	public int uid;
	public String parentId;
	public Set<String> flags;
}