/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.mail.replica.indexing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor.MessageBodyData;
import net.bluemind.core.api.Stream;

public class IndexedMessageBody {

	public final List<String> content;
	public final Map<String, Object> data;
	public final Map<String, Object> headers;
	public final String subject;
	public final String uid;
	public final String preview;

	private static final Logger logger = LoggerFactory.getLogger(IndexedMessageBody.class);

	private IndexedMessageBody(String uid, List<String> content, Map<String, Object> data, Map<String, Object> headers,
			String subject, String preview) {
		this.uid = uid;
		this.content = content;
		this.data = data;
		this.headers = headers;
		this.subject = subject;
		this.preview = preview;
	}

	public String toString() {
		return MoreObjects.toStringHelper(IndexedMessageBody.class)//
				.add("uid", uid)//
				.add("subject", subject)//
				.add("data", data)//
				.add("headers", headers)//
				.toString();
	}

	public static class IndexedMessageBodyBuilder {
		public List<String> content = new ArrayList<>();
		public Map<String, Object> data = new HashMap<>();
		public Map<String, Object> headers = new HashMap<>();
		public String subject = "";
		public String preview = "";
		public final String uid;

		public IndexedMessageBodyBuilder(String uid) {
			this.uid = uid;
		}

		public IndexedMessageBodyBuilder subject(String subject) {
			this.subject = subject;
			return this;
		}

		public IndexedMessageBodyBuilder content(List<String> content) {
			this.content = content;
			return this;
		}

		public IndexedMessageBodyBuilder headers(Map<String, Object> headers) {
			this.headers = headers;
			return this;
		}

		public IndexedMessageBodyBuilder data(Map<String, Object> data) {
			this.data = data;
			return this;
		}

		public IndexedMessageBodyBuilder preview(String preview) {
			this.preview = preview;
			return this;
		}

		public IndexedMessageBody build() {
			return new IndexedMessageBody(uid, content, data, headers, subject, preview);
		}
	}

	public static IndexedMessageBody createIndexBody(String uid, Stream eml)
			throws InterruptedException, ExecutionException, TimeoutException {
		return BodyStreamProcessor.processBody(eml).thenApply(bodyData -> createIndexBody(uid, bodyData)).get(15,
				TimeUnit.SECONDS);
	}

	private static final String EPOCH_STRING = new Date(0L).toInstant().toString();

	public static IndexedMessageBody createIndexBody(String uid, MessageBodyData bodyData) {
		Objects.requireNonNull(bodyData, "Can't create IndexMessageBody from null MessageBodyData");
		logger.debug("Extracted body data {}", bodyData);
		MessageBody body = bodyData.body;

		List<String> content = new ArrayList<>();
		content.add(body.subject);
		content.add(bodyData.text);
		content.addAll(bodyData.filenames);
		content.addAll(bodyData.with);

		String preview = bodyData.body.preview;

		Map<String, Object> data = new HashMap<>();
		data.put("preview", preview);
		data.put("with", bodyData.with);
		data.put("size", body.size);
		data.put("content-type", body.structure.mime);
		data.put("date", body.date != null ? body.date.toInstant().toString() : EPOCH_STRING);

		data.put("from",
				body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator)
						.map(r -> Arrays.asList(r.dn, r.address)).flatMap(Collection::stream).filter(Objects::nonNull)
						.collect(Collectors.toList()));
		data.put("to",
				body.recipients.stream().filter(r -> r.kind == RecipientKind.Primary)
						.map(r -> Arrays.asList(r.dn, r.address)).flatMap(Collection::stream).filter(Objects::nonNull)
						.collect(Collectors.toList()));
		data.put("cc",
				body.recipients.stream().filter(r -> r.kind == RecipientKind.CarbonCopy)
						.map(r -> Arrays.asList(r.dn, r.address)).flatMap(Collection::stream).filter(Objects::nonNull)
						.collect(Collectors.toList()));

		Map<String, Object> headers = new HashMap<>();
		Set<String> hasProps = new HashSet<>();
		headers.putAll(bodyData.headers);
		headers.put("from", Strings.emptyToNull(body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator)
				.map(Object::toString).collect(Collectors.joining(", "))));
		headers.put("to", Strings.emptyToNull(body.recipients.stream().filter(r -> r.kind == RecipientKind.Primary)
				.map(Object::toString).collect(Collectors.joining(", "))));
		headers.put("cc", Strings.emptyToNull(body.recipients.stream().filter(r -> r.kind == RecipientKind.CarbonCopy)
				.map(Object::toString).collect(Collectors.joining(", "))));
		if (bodyData.headers.containsKey("x-bm-event") && bodyData.headers.containsKey("x-bm-rsvp")) {
			hasProps.add("invitation");
		}
		if (bodyData.headers.containsKey("x-asterisk-callerid")) {
			hasProps.add("voicemail");
		}

		if (!bodyData.filenames.isEmpty()) {
			data.put("filename", bodyData.filenames);
		}

		if (body.structure.hasRealAttachments()) {
			hasProps.add("attachments");
		}
		data.put("has", hasProps);

		return new IndexedMessageBody.IndexedMessageBodyBuilder(uid) //
				.content(content) //
				.data(data) //
				.headers(headers).subject(body.subject)//
				.preview(preview)//
				.build();
	}

}
