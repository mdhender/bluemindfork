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
package net.bluemind.milter.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.stream.RawField;

public class UpdatedMailMessage {
	public final Map<String, Collection<String>> properties;
	private Message message;

	public Optional<String> envelopSender = Optional.empty();
	public final Set<String> addRcpt = new HashSet<>();
	public final Set<String> removeRcpt = new HashSet<>();

	public final List<RawField> newHeaders = new ArrayList<RawField>();
	public final List<String> bodyChangedBy = new ArrayList<String>();
	public final List<String> headerChangedBy = new ArrayList<String>();
	public final Set<String> removeHeaders = new HashSet<>();

	public UpdatedMailMessage(Map<String, Collection<String>> properties, Message message) {
		this.properties = properties;
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}

	public void updateBody(Message updatedMessage, String updateBy) {
		setBody(updatedMessage, updateBy);
	}

	private void setBody(Message updatedMessage, String updateBy) {
		message.getHeader().setField(updatedMessage.getHeader().getField(FieldName.CONTENT_TYPE));
		updatedMessage.setHeader(message.getHeader());
		message = updatedMessage;

		bodyChangedBy.add(updateBy);
	}

	public void addHeader(String name, String value, String addedBy) {
		RawField rf = new RawField(name, value);
		message.getHeader().addField(rf);
		newHeaders.add(rf);

		headerChangedBy.add(addedBy);
	}

	public void removeHeader(String name) {
		removeHeaders.add(name);
	}

	public Body getBody() {
		return message.getBody();
	}
}
