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
package net.bluemind.backend.mail.parsing;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.mail.internet.MimeUtility;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.stream.Field;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.CharStreams;
import com.google.common.io.CountingInputStream;

import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.content.analysis.ContentAnalyzerFactory;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.OffloadedBodyFactory;
import net.bluemind.mime4j.common.OffloadedBodyFactory.IStreamTransfer;
import net.bluemind.mime4j.common.OffloadedBodyFactory.SizedBody;

public class BodyStreamProcessor {

	private static final Logger logger = LoggerFactory.getLogger(BodyStreamProcessor.class);

	/**
	 * The version of the DB body this {@link BodyStreamProcessor} produces from an
	 * IMAP message.
	 */
	public static int BODY_VERSION = 4;

	static {
		System.setProperty("mail.mime.decodetext.strict", "false");
	}

	public static CompletableFuture<MessageBodyData> processBody(Stream eml) {
		return EZInputStreamAdapter.consume(eml, emlInput -> {
			logger.debug("Consuming wrapped stream {}", emlInput);
			return parseBody(emlInput);
		});
	}

	public static MessageBodyData parseBodyGetFullContent(CountingInputStream emlInput) {
		return parseBody(emlInput);
	}

	private static MessageBodyData parseBody(CountingInputStream emlInput) {
		long time = System.currentTimeMillis();

		MessageBody mb = new MessageBody();
		mb.bodyVersion = BODY_VERSION;
		IStreamTransfer transfer = OffloadedBodyFactory.sharedBufferTransfer();
		try (Message parsed = Mime4JHelper.parse(emlInput, new OffloadedBodyFactory(transfer))) {
			parseSubject(mb, parsed);

			mb.date = parsed.getDate();
			mb.size = (int) emlInput.getCount();
			Multimap<String, String> mmapHeaders = MultimapBuilder.hashKeys().linkedListValues().build();
			parsed.getHeader().forEach(field -> mmapHeaders.put(field.getName(), field.getBody()));
			mb.headers = processHeaders(mmapHeaders);
			mb.messageId = parsed.getMessageId();
			mb.references = processReferences(mmapHeaders);

			processRecipients(mb, parsed);

			if (logger.isDebugEnabled()) {
				logger.debug("Got {} unique header(s)", mb.headers.size());
			}
			List<String> filenames = new ArrayList<>();
			StringBuilder bodyTxt = new StringBuilder();
			if (!parsed.isMultipart()) {
				Part p = new Part();
				p.mime = parsed.getMimeType();
				p.address = "1";
				p.size = mb.size;
				mb.structure = p;
				p.charset = p.mime.startsWith("text/") ? parsed.getCharset() : null;
				p.encoding = parsed.getContentTransferEncoding();
			} else {
				Multipart mpBody = (Multipart) parsed.getBody();
				processMultipart(mb, mpBody, filenames, bodyTxt);
			}

			BodyAndDom bodyWithDom = extractBody(parsed);
			String extractedBody = bodyWithDom.text;
			extractedBody = extractedBody.replace("\u0000", "");
			bodyTxt.append(extractedBody);
			mb.preview = CharMatcher.whitespace()
					.collapseFrom(extractedBody.substring(0, Math.min(160, extractedBody.length())), ' ').trim();

			List<String> with = new LinkedList<>();
			if (parsed.getFrom() != null && !parsed.getFrom().isEmpty()) {
				with.add(toString(parsed.getFrom().get(0)));
			}
			with.addAll(toString(parsed.getTo()));
			with.addAll(toString(parsed.getCc()));

			mb.structure.size = mb.size;
			time = System.currentTimeMillis() - time;
			if (time > 10) {
				logger.info("Body ({} byte(s)) processed in {}ms.", mb.size, time);
			}

			cleanUnreferencedInlineAttachments(bodyWithDom.jsoup, mb, parsed);

			MessageBodyData bodyData = new MessageBodyData(mb, bodyTxt.toString(), filenames, with,
					mapHeaders(mb.headers));
			logger.debug("Processed {}", bodyData);
			return bodyData;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static void parseSubject(MessageBody mb, Message parsed) {
		String subject = parsed.getSubject();
		if (subject != null) {
			mb.subject = subject.replace("\u0000", "");
			try {
				mb.subject = MimeUtility.decodeWord(mb.subject);
			} catch (Exception e) {
				try {
					mb.subject = MimeUtility.decodeText(mb.subject);
				} catch (UnsupportedEncodingException | UnsupportedCharsetException e1) {
					logger.warn("Cannot decode subject {}", e1.getMessage());
				}
			}
		}
	}

	private static void cleanUnreferencedInlineAttachments(Optional<Document> jsoup, MessageBody mb, Message parsed) {
		List<Part> withContentIds = partsWithContentIds(mb.structure, null, new LinkedList<>());
		if (!withContentIds.isEmpty()) {
			Optional<AddressableEntity> ae = htmlBody(parsed);
			Set<String> refCids = findCIDs(jsoup.orElseGet(() -> {
				String body = ae.map(BodyStreamProcessor::getBodyContent).orElse("");
				return Jsoup.parse(body);
			}));
			for (Part p : withContentIds) {
				String cid = CharMatcher.anyOf("<>").trimFrom(p.contentId);
				if (!identifiesAsBody(ae, p) && !refCids.contains(cid)) {
					p.dispositionType = DispositionType.ATTACHMENT;
					p.contentId = null;
				} else {
					p.dispositionType = DispositionType.INLINE;
				}
			}
		}
	}

	private static boolean identifiesAsBody(Optional<AddressableEntity> ae, Part p) {
		if (!ae.isPresent()) {
			return false;
		}
		Field field = ae.get().getHeader().getField(FieldName.CONTENT_ID);
		if (field == null) {
			return false;
		}
		return field.getBody().equals(p.contentId);
	}

	private static List<Part> partsWithContentIds(Part structure, Part parent, List<Part> attach) {
		if (parent != null && structure.contentId != null && parent.mime.startsWith(Mime4JHelper.M_RELATED)) {
			attach.add(structure);
		}
		for (Part p : structure.children) {
			partsWithContentIds(p, structure, attach);
		}
		return attach;
	}

	private static Optional<AddressableEntity> htmlBody(Message message) {
		Body body = message.getBody();

		if (body instanceof Multipart) {
			Multipart mp = (Multipart) body;
			List<AddressableEntity> parts = Mime4JHelper.expandTree(mp.getBodyParts());

			for (AddressableEntity ae : parts) {
				String mime = ae.getMimeType();
				if (Mime4JHelper.TEXT_HTML.equals(mime) && !Mime4JHelper.isAttachment(ae)) {
					return Optional.of(ae);
				}
			}
		}
		return Optional.empty();
	}

	private static Set<String> findCIDs(Document doc) {
		return doc.select("[src^=cid:]").stream().map(e -> {
			String src = e.attr("src");
			return src.substring(4);
		}).collect(Collectors.toSet());
	}

	private static List<String> processReferences(Multimap<String, String> mmapHeaders) {
		for (String headerName : mmapHeaders.keySet()) {
			if ("references".equalsIgnoreCase(headerName)) {
				return Arrays.asList(mmapHeaders.get(headerName).iterator().next().split(" "));
			}
		}
		return null;
	}

	private static String toString(Mailbox m) {
		StringBuilder sb = new StringBuilder();
		if (m.getName() != null) {
			sb.append(m.getName());
			sb.append(" ");
		}
		sb.append(m.getAddress());
		return sb.toString();
	}

	private static List<String> toString(AddressList to) {
		if (to == null) {
			return Lists.newArrayList();
		}
		ArrayList<String> r = new ArrayList<>(to.size());
		for (Address a : to) {
			if (a instanceof Mailbox) {
				r.add(toString((Mailbox) a));
			}
		}
		return r;
	}

	private static Map<String, Keyword> mapHeaders(List<net.bluemind.backend.mail.api.MessageBody.Header> headers) {
		return headers.stream()
				.collect(Collectors.toMap(h -> h.name.toLowerCase(), h -> new Keyword(h.values.get(0)), (u, v) -> v));
	}

	public static class MessageBodyData {
		public final MessageBody body;
		public final String text;
		public final List<String> filenames;
		public final List<String> with;
		public final Map<String, Keyword> headers;

		public MessageBodyData(MessageBody body, String text, List<String> filenames, List<String> with,
				Map<String, Keyword> headers) {
			this.body = body;
			this.text = text;
			this.filenames = filenames;
			this.with = with;
			this.headers = headers;
		}

		public String toString() {
			return MoreObjects.toStringHelper(MessageBodyData.class)//
					.add("body", body)//
					.add("with", with)//
					.add("headers", headers)//
					.add("filenames", filenames)//
					.add("textSize", Strings.nullToEmpty(text).length())//
					.toString();
		}
	}

	private static class BodyAndDom {
		String text;
		Optional<Document> jsoup;

		public static BodyAndDom plainText(String txt) {
			BodyAndDom bd = new BodyAndDom();
			bd.text = txt;
			bd.jsoup = Optional.empty();
			return bd;
		}

		public static BodyAndDom html(String txt, Document parsed) {
			BodyAndDom bd = new BodyAndDom();
			bd.text = txt;
			bd.jsoup = Optional.of(parsed);
			return bd;
		}
	}

	private static BodyAndDom extractBody(Message message) {
		Body body = message.getBody();

		if (body instanceof Multipart) {
			Multipart mp = (Multipart) body;
			List<AddressableEntity> parts = Mime4JHelper.expandTree(mp.getBodyParts());

			Optional<AddressableEntity> htmlPart = parts.stream().filter(
					part -> Mime4JHelper.TEXT_HTML.equals(part.getMimeType()) && !Mime4JHelper.isAttachment(part))
					.findFirst();

			if (htmlPart.isPresent()) {
				return htmlToText(getBodyContent(htmlPart.get()));
			}

			Optional<AddressableEntity> txtPart = parts.stream().filter(
					part -> Mime4JHelper.TEXT_PLAIN.equals(part.getMimeType()) && !Mime4JHelper.isAttachment(part))
					.findFirst();

			if (txtPart.isPresent()) {
				return BodyAndDom
						.plainText(CharMatcher.whitespace().collapseFrom(getBodyContent(txtPart.get()), ' ').trim());
			}

		} else {
			if (body instanceof TextBody) {
				return htmlToText(getBodyContent(message));
			}
		}

		return BodyAndDom.plainText("");
	}

	private static BodyAndDom htmlToText(String html) {
		Document parsed = Jsoup.parse(html);
		return BodyAndDom.html(parsed.body().text(), parsed);
	}

	private static String getBodyContent(Entity e) {
		String encoding = "UTF-8";
		Field field = e.getHeader().getField("Content-Type");
		if (null != field) {
			ContentTypeField ctField = (ContentTypeField) field;
			String cs = ctField.getCharset();
			if (null != cs) {
				encoding = cs;
			}
		}

		Charset charset = null;
		try {
			charset = Charset.forName(encoding);
		} catch (UnsupportedCharsetException | IllegalCharsetNameException ex) {
			logger.warn("**** unsupported charset: {}", encoding);
			charset = StandardCharsets.UTF_8;
		}

		TextBody tb = (TextBody) e.getBody();
		String partContent = null;
		try (InputStream in = tb.getInputStream()) {
			partContent = CharStreams.toString(new InputStreamReader(in, charset));
		} catch (IOException io) {
			throw new ServerFault(io);
		}
		return partContent;
	}

	private static void processRecipients(MessageBody mb, Message parsed) {
		List<Recipient> output = new LinkedList<>();
		addRecips(output, MessageBody.RecipientKind.Originator, parsed.getFrom());
		addRecips(output, MessageBody.RecipientKind.Sender, parsed.getSender());
		addRecips(output, MessageBody.RecipientKind.Primary, parsed.getTo());
		addRecips(output, MessageBody.RecipientKind.CarbonCopy, parsed.getCc());
		addRecips(output, MessageBody.RecipientKind.BlindCarbonCopy, parsed.getBcc());
		logger.debug("Parsed {} recipient(s)", output.size());
		mb.recipients = output;
	}

	private static final Pattern STILL_ENCODED = Pattern.compile("=\\?[^\\?]+\\?[Qq]\\?[^\\?]+\\?=");

	private static void addRecips(List<Recipient> output, RecipientKind kind, MailboxList mailboxes) {
		if (mailboxes == null) {
			return;
		}
		mailboxes.forEach(mailbox -> {
			if (!">".equals(mailbox.getAddress())) {
				String dn = mailbox.getName();
				if (dn != null && STILL_ENCODED.matcher(dn).matches()) {
					logger.warn("Email name part is still encoded '{}'", dn);
					try {
						dn = MimeUtility.decodeText(dn.replace(" ", "_"));
					} catch (UnsupportedEncodingException e) {
						logger.warn("Failed to decode '{}': {}", dn, e.getMessage());
					}
				}

				Recipient recip = Recipient.create(kind, dn, mailbox.getAddress());
				output.add(recip);
			}
		});
	}

	private static void addRecips(List<Recipient> output, RecipientKind kind, Mailbox mailbox) {
		if (mailbox == null || ">".equals(mailbox.getAddress())) {
			return;
		}
		Recipient recip = Recipient.create(kind, mailbox.getName(), mailbox.getAddress());
		output.add(recip);
	}

	private static void addRecips(List<Recipient> output, RecipientKind kind, AddressList mailboxes) {
		if (mailboxes == null) {
			return;
		}
		addRecips(output, kind, mailboxes.flatten());
	}

	private static void processMultipart(MessageBody mb, Multipart mpBody, List<String> filenames,
			StringBuilder bodyTxt) {
		Part root = new Part();
		root.mime = "multipart/" + mpBody.getSubType();
		root.address = "TEXT";
		List<Entity> subParts = mpBody.getBodyParts();
		int idx = 1;
		for (Entity sub : subParts) {
			Part child = subPart(root, idx++, sub, filenames, bodyTxt);
			root.children.add(child);
		}
		mb.structure = root;
	}

	private static Part subPart(Part parent, int i, Entity sub, List<String> filenames, StringBuilder bodyTxt) {
		String curAddr = "TEXT".equals(parent.address) ? "" + i : parent.address + "." + i;
		Part p = new Part();
		p.address = curAddr;
		try {
			p.mime = MimeUtility.decodeText(sub.getMimeType());
		} catch (UnsupportedEncodingException e1) {
			p.mime = sub.getMimeType();
		}
		sub.getHeader().forEach(e -> {
			if (FieldName.CONTENT_ID.equalsIgnoreCase(e.getName())) {
				p.contentId = Strings.emptyToNull(e.getBody());
			}
		});

		if (sub.isMultipart()) {
			Multipart mult = (Multipart) sub.getBody();
			List<Entity> subParts = mult.getBodyParts();
			int idx = 1;
			for (Entity subsub : subParts) {
				Part child = subPart(p, idx++, subsub, filenames, bodyTxt);
				p.children.add(child);
			}
		} else if (sub.getBody() instanceof SingleBody) {
			Multimap<String, String> mmapHeaders = MultimapBuilder.hashKeys().linkedListValues().build();
			sub.getHeader().forEach(field -> mmapHeaders.put(field.getName(), field.getBody()));
			p.headers = processHeaders(mmapHeaders);

			// fix fetch filename
			p.fileName = AddressableEntity.getFileName(sub);
			if (p.fileName != null) {
				filenames.add(p.fileName);
				indexAttachment(sub, bodyTxt);
			}

			p.charset = sub.getCharset();
			p.encoding = sub.getContentTransferEncoding();
			SizedBody sized = (SizedBody) sub.getBody();
			p.size = sized.size();

			try {
				p.dispositionType = DispositionType.valueOfNullSafeIgnoreCase(sub.getDispositionType());
			} catch (IllegalArgumentException ie) {
				logger.warn("Invalid disposition type, using {}: {}", DispositionType.ATTACHMENT, ie.getMessage());
				p.dispositionType = DispositionType.ATTACHMENT;
			}

			// Apple Mail sends PDFs as inline stuff
			// --Apple-Mail=_597C093C-5BA5-4C97-8C3A-FE774541930B
			// Content-Disposition: inline; filename="Pack Sponsor - Red Hat Forum Paris
			// 2019.pdf"
			// Content-Type: application/pdf; x-unix-mode=0644; name="Pack Sponsor - Red Hat
			// Forum Paris 2019.pdf"
			// Content-Transfer-Encoding: base64
			if (p.dispositionType == DispositionType.INLINE && p.contentId == null && p.fileName != null) {
				p.dispositionType = DispositionType.ATTACHMENT;
			}
			if ("multipart/report".equals(parent.mime) && p.dispositionType == null && p.fileName == null) {
				handleReportPart(sub, filenames, bodyTxt, p);
			}
		} else {
			logger.warn("Don't know how to process {}", p.mime);
		}

		return p;
	}

	private static void handleReportPart(Entity sub, List<String> filenames, StringBuilder bodyTxt, Part part) {
		Field field = sub.getHeader().getField("Content-Type");
		if (null != field) {
			ContentTypeField ctField = (ContentTypeField) field;
			String mimeType = ctField.getMimeType();
			switch (mimeType) {
			case "message/delivery-status":
				part.dispositionType = DispositionType.ATTACHMENT;
				part.fileName = "details.txt";
				break;
			case "text/rfc822-headers":
				part.dispositionType = DispositionType.ATTACHMENT;
				part.fileName = "Undelivered Message Headers.txt";
				break;
			case ContentTypeField.TYPE_MESSAGE_RFC822:
				part.dispositionType = DispositionType.ATTACHMENT;
				part.fileName = "Forwarded message.eml";
				break;
			}

			if (part.fileName != null) {
				filenames.add(part.fileName);
				indexAttachment(sub, bodyTxt);
			}
		}
	}

	private static void indexAttachment(Entity ae, StringBuilder bodyTxt) {
		SingleBody body = (SingleBody) ae.getBody();
		try (InputStream in = body.getInputStream()) {
			if (canAnalyzeAttachment(ae)) {
				ContentAnalyzerFactory.get().ifPresent(analyzer -> {
					CompletableFuture<Optional<String>> ret = analyzer.extractText(in);
					try {
						Optional<String> extractedText = ret.get(5, TimeUnit.SECONDS);
						extractedText.ifPresent(content -> bodyTxt.append(" " + content + " "));
					} catch (Exception e) {

					}
				});
			}
		} catch (Exception e) {
			logger.warn("Cannot retrieve attachment part", e);
		}
	}

	private static boolean canAnalyzeAttachment(Entity ae) {
		String encoding = ae.getContentTransferEncoding();
		if (encoding != null && encoding.toLowerCase().contains("uuencode")) {
			return false;
		}
		String mimeType = ae.getMimeType();
		if (mimeType != null) {
			return !(mimeType.startsWith("image/") || mimeType.startsWith("audio/") || mimeType.startsWith("video/"));
		}

		return true;
	}

	private static List<net.bluemind.backend.mail.api.MessageBody.Header> processHeaders(
			Multimap<String, String> mmapHeaders) {
		List<MessageBody.Header> headers = new LinkedList<>();
		Set<String> whitelist = HeaderWhitelist.getInstance().whitelist;
		for (String h : mmapHeaders.keySet()) {
			String lowerCase = h.toLowerCase();
			if (lowerCase.startsWith("x-bm") || whitelist.contains(lowerCase)) {
				headers.add(MessageBody.Header.create(h, mmapHeaders.get(h).stream()
						.map(val -> CharMatcher.whitespace().trimLeadingFrom(val)).collect(Collectors.toList())));
			}
		}
		return headers;
	}

}
