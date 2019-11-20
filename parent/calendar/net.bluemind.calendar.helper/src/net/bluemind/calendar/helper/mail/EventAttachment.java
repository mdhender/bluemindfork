/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.calendar.helper.mail;

import java.util.Optional;

import org.apache.james.mime4j.message.BodyPart;

public class EventAttachment {

	public final String uri;
	public final String name;
	public final String contentType;
	public final Optional<BodyPart> part;

	public EventAttachment(String uri, String name, String contentType, BodyPart part) {
		this.uri = uri;
		this.name = name;
		this.contentType = contentType;
		this.part = Optional.of(part);
	}

	public EventAttachment(String uri, String name, String contentType) {
		this.uri = uri;
		this.name = name;
		this.contentType = contentType;
		this.part = Optional.empty();
	}

	public boolean isBinaryAttachment() {
		return part.isPresent();
	}

	public String getUri() {
		return uri;
	}

	public String getName() {
		return name;
	}

}
