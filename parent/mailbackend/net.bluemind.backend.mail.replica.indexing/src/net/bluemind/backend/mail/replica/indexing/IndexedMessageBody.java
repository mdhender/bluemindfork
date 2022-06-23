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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor.MessageBodyData;
import net.bluemind.backend.mail.parsing.Keyword;
import net.bluemind.core.api.Stream;

public class IndexedMessageBody {

	public final String content;
	public final Map<String, Object> data;
	private final Map<String, Keyword> headers;
	public final Keyword subject;
	public final Keyword messageId;
	public final List<Keyword> references;
	public final String uid;
	public final String preview;

	private static final Logger logger = LoggerFactory.getLogger(IndexedMessageBody.class);

	private IndexedMessageBody(String uid, String content, Map<String, Object> data, Map<String, Keyword> headers,
			Keyword subject, String preview, Keyword messageId, List<Keyword> references) {
		this.uid = uid;
		this.content = content;
		this.data = data;
		this.headers = headers;
		this.subject = subject;
		this.preview = preview;
		this.messageId = messageId;
		this.references = references;
	}

	public String toString() {
		return MoreObjects.toStringHelper(IndexedMessageBody.class)//
				.add("uid", uid)//
				.add("subject", subject)//
				.add("data", data)//
				.add("messageId", messageId.value)//
				.add("references", references)//
				.add("headers", headers)//
				.toString();
	}

	public static class IndexedMessageBodyBuilder {
		public String content = "";
		public Map<String, Object> data = new HashMap<>();
		public Map<String, Keyword> headers = new HashMap<>();
		public Keyword subject = new Keyword(null);
		public Keyword messageId = new Keyword(null);
		public List<Keyword> references = Collections.emptyList();
		public String preview = "";
		public final String uid;

		public IndexedMessageBodyBuilder(String uid) {
			this.uid = uid;
		}

		public IndexedMessageBodyBuilder subject(Keyword subject) {
			this.subject = subject;
			return this;
		}

		public IndexedMessageBodyBuilder content(String content) {
			this.content = content;
			return this;
		}

		public IndexedMessageBodyBuilder headers(Map<String, Keyword> headers) {
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

		public IndexedMessageBodyBuilder messageId(Keyword messageId) {
			this.messageId = messageId;
			return this;
		}

		public IndexedMessageBodyBuilder references(List<Keyword> references) {
			this.references = references;
			return this;
		}

		public IndexedMessageBody build() {
			return new IndexedMessageBody(uid, content, data, headers, subject, preview, messageId, references);
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

		Map<String, Keyword> headers = new HashMap<>();
		Set<String> hasProps = new HashSet<>();
		headers.putAll(bodyData.headers);
		headers.put("from", new Keyword(body.recipients.stream().filter(r -> r.kind == RecipientKind.Originator)
				.map(Object::toString).collect(Collectors.joining(", "))));
		headers.put("to", new Keyword(body.recipients.stream().filter(r -> r.kind == RecipientKind.Primary)
				.map(Object::toString).collect(Collectors.joining(", "))));
		headers.put("cc", new Keyword(body.recipients.stream().filter(r -> r.kind == RecipientKind.CarbonCopy)
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

		if (!body.structure.nonInlineAttachments().isEmpty()) {
			hasProps.add("attachments");
		}
		data.put("has", hasProps);

		return new IndexedMessageBody.IndexedMessageBodyBuilder(uid) //
				.content(bodyData.text) //
				.data(data) //
				.headers(headers).subject(new Keyword(body.subject))//
				.preview(preview)//
				.messageId(new Keyword(body.messageId))//
				.references(body.references == null ? Collections.emptyList()
						: body.references.stream().map(Keyword::new).collect(Collectors.toList()))//
				.build();
	}

	public Map<String, String> headers() {
		return headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
			String s = e.getValue().toString();
			return s.substring(0, Math.min(s.length(), 1024));
		}));
	}

}
