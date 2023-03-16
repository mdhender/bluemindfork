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

import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditLogEntry {

	public SecurityContextElement securityContext;

	public ContainerElement container;

	public ItemElement item;

	public ContentElement content;

	@JsonProperty("updatemessage")
	public String updatemessage;

	public String logtype;

	public String action;

	@JsonProperty("@timestamp")
	public Date timestamp = Date.from(Instant.now());

	public AuditLogEntry() {
	}

}
