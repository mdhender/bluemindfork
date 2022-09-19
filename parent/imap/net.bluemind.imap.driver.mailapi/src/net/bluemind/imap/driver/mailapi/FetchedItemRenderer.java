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
package net.bluemind.imap.driver.mailapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.SingleBody;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.lib.vertx.utils.MmapWriteStream;
import net.bluemind.mime4j.common.Mime4JHelper;

public class FetchedItemRenderer {

	private static final Logger logger = LoggerFactory.getLogger(FetchedItemRenderer.class);

	private final IDbMailboxRecords recApi;
	private final List<MailPart> fields;
	private final IDbMessageBodies bodyApi;

	public FetchedItemRenderer(IDbMessageBodies bodyApi, IDbMailboxRecords recApi, List<MailPart> fields) {
		this.recApi = recApi;
		this.fields = fields;
		this.bodyApi = bodyApi;
	}

	public Map<String, ByteBuf> renderFields(ItemValue<MailboxRecord> rec) {
		Map<String, ByteBuf> ret = new HashMap<>();

		Supplier<MessageBody> body = Suppliers.memoize(() -> getBody(rec));

		for (MailPart f : fields) {
			String upField = f.name.toUpperCase();
			switch (upField) {
			case "FLAGS":
				String flags = rec.value.flags.stream().map(mif -> mif.flag).collect(Collectors.toSet()).stream()
						.collect(Collectors.joining(" ", "(", ")"));
				ret.put(f.toString(), Unpooled.wrappedBuffer(flags.getBytes()));
				break;
			case "UID":
				// uid is auto-added by UidFetchProcessor
				break;
			case "INTERNALDATE":
				ret.put(f.toString(), Unpooled
						.wrappedBuffer(("\"" + DateUtil.toImapDateTime(rec.value.internalDate) + "\"").getBytes()));
				break;
			case "BODYSTRUCTURE":
				BodyStructureRenderer bsr = new BodyStructureRenderer();
				String bs = bsr.from(body.get().structure);
				ByteBuf bsBuf = Unpooled.wrappedBuffer(bs.getBytes());
				ret.put(f.toString(), bsBuf);
				break;
			case "RFC822.SIZE":
				String size = Integer.toString(body.get().size);
				ret.put(f.toString(), Unpooled.wrappedBuffer(size.getBytes()));
				break;
			case "ENVELOPE":
				ret.put(f.toString(), envelope(body, rec));
				break;
			case "BODY", "BODY.PEEK":
				ByteBuf bodyPeek = bodyPeek(recApi, body, f, rec);
				if (bodyPeek != null) {
					ret.put(f.toString(), literalize(bodyPeek));
				} else {
					logger.warn("body.peek of {} returned null", f);
				}
				break;
			default:
				logger.warn("Unsupported fetch field {}", f);
				break;
			}
		}

		return ret;
	}

	private ByteBuf literalize(ByteBuf bodyPeek) {
		int len = bodyPeek.readableBytes();
		ByteBuf lenBuf = Unpooled.wrappedBuffer(("{" + len + "}\r\n").getBytes());
		return Unpooled.wrappedBuffer(lenBuf, bodyPeek);
	}

	private MessageBody getBody(ItemValue<MailboxRecord> rec) {
		return Optional.ofNullable(rec.value.body).orElseGet(() -> bodyApi.getComplete(rec.value.messageBody));
	}

	private ByteBuf bodyPeek(IDbMailboxRecords recApi, Supplier<MessageBody> body, MailPart f,
			ItemValue<MailboxRecord> rec) {
		String section = f.section == null ? "" : f.section;

		if (section.equalsIgnoreCase("header.fields")) {
			return headers(body, f.options, rec);
		} else if (section.equalsIgnoreCase("header")) {
			return headers(body, Sets.newHashSet("From", "To", "Cc", "Subject", "Message-ID", "X-Bm-Event"), rec);
		} else if (section.endsWith(".MIME") && partAddr(section.replace(".MIME", ""))) {
			String part = f.section.replace(".MIME", "");
			logger.info("Fetch mime headers of {}", part);
			return body.get().structure.parts().stream().filter(p -> p.address.equalsIgnoreCase(part)).findAny()
					.map(p -> {
						StringBuilder b = new StringBuilder();
						b.append("Content-Type: " + p.mime + "\r\n");
						// core returns a decoded version
						b.append("Content-Transfer-Encoding: 8bit\r\n");
						if (p.contentId != null) {
							b.append("Content-ID: " + p.contentId + "\r\n");
						}
						b.append("\r\n");
						return Unpooled.wrappedBuffer(b.toString().getBytes());
					}).orElse(null);
		} else if (partAddr(section)) {
			// load eml part
			try {
				Stream fullMsg = recApi.fetchComplete(rec.value.imapUid);
				int len = body.get().size;
				ByteBuf emlBuffer = readMmap(fullMsg, len).join();
				return getPart(emlBuffer, f.section);
			} catch (ServerFault sf) {
				logger.error("could not fetch part  {}[{}]: {}", rec.value.imapUid, f.section, sf.getMessage());
				return null;
			}
		} else if (section.isEmpty()) {
			// full eml
			try {
				Stream fullMsg = recApi.fetchComplete(rec.value.imapUid);
				int len = body.get().size;
				return readMmap(fullMsg, len).join();
			} catch (ServerFault sf) {
				logger.error("could not fetch {}: {}", rec.value.imapUid, sf.getMessage());
				return null;
			}
		} else {
			logger.warn("unknown section '{}'", f.section);
		}

		return null;
	}

	private boolean partAddr(String s) {
		return !Strings.isNullOrEmpty(s) && CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(s);
	}

	private ByteBuf getPart(ByteBuf emlBuffer, String section) {
		try (InputStream in = new ByteBufInputStream(emlBuffer, true); Message parsed = Mime4JHelper.parse(in)) {
			SingleBody body = null;
			if (parsed.isMultipart()) {
				Multipart mp = (Multipart) parsed.getBody();
				body = Mime4JHelper.expandTree(mp.getBodyParts()).stream()
						.filter(ae -> section.equals(ae.getMimeAddress())).findAny()
						.map(ae -> (SingleBody) ae.getBody()).orElseGet(() -> {
							logger.warn("Part {} not found", section);
							return null;
						});
			} else if (section.equals("1") || section.equals("TEXT")) {
				body = (SingleBody) parsed.getBody();
			}
			if (body == null) {
				return Unpooled.buffer();
			} else {
				return buffer(body);
			}
		} catch (Exception e) {
			logger.error("getPart({})", section, e);
			return null;
		}
	}

	private ByteBuf buffer(SingleBody body) throws IOException {
		try (InputStream in = body.getInputStream()) {
			return Unpooled.wrappedBuffer(ByteStreams.toByteArray(in));
		}
	}

	private static String quoted(String s) {
		return '"' + s + '"';
	}

	/*
	 * Enveloppe structure: The fields of the envelope structure are in the
	 * following order: date, subject, from, sender, reply-to, to, cc, bcc,
	 * in-reply-to, and message-id. The date, subject, in-reply-to, and message-id
	 * fields are strings. The from, sender, reply-to, to, cc, and bcc fields are
	 * parenthesized lists of address structures.
	 */
	private ByteBuf envelope(Supplier<MessageBody> body, ItemValue<MailboxRecord> rec) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		// LC: TODO MimeUtils.fold() ?

		// Date
		if (rec.value.internalDate != null) {
			DateTimeField dateField = Fields.date("Date", rec.value.internalDate);
			sb.append(quoted(dateField.getBody()));
		} else {
			sb.append("NIL");
		}
		sb.append(" ");

		// Subject
		var subject = Fields.subject(body.get().subject).getBody();
		if (!Strings.isNullOrEmpty(subject)) {
			sb.append("{" + subject.length() + "}\r\n").append(subject);
		} else {
			sb.append("NIL");
		}
		sb.append(" ");

		Consumer<List<Recipient>> toMail = rcpts -> {
			sb.append("(");
			int idx = 0;
			for (Recipient rcpt : rcpts) {
				if (idx++ > 0) {
					sb.append(" ");
				}
				var loginatdom = rcpt.address.split("@");
				sb.append("(");
				sb.append(rcpt.dn != null ? (quoted(rcpt.dn)) : "NIL");
				sb.append(" ");

				sb.append("NIL "); // Don't understand what it should be
				sb.append(quoted(loginatdom[0]));
				sb.append(" ");

				sb.append(quoted(loginatdom[1]));
				sb.append(")");
			}
			sb.append(")");
		};
		Consumer<Mailbox> mailboxToMail = mbox -> {
			sb.append("((");
			sb.append(mbox.getName() != null ? (quoted(mbox.getName())) : "NIL");
			sb.append(" ");

			sb.append("NIL "); // Don't understand what it should be
			sb.append(quoted(mbox.getLocalPart()));
			sb.append(" ");

			sb.append(quoted(mbox.getDomain()));
			sb.append("))");
		};

		// From
		body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Originator).findFirst()
				.ifPresentOrElse(r -> {
					mailboxToMail.accept(fromRecipient(r));
					sb.append(" ");
				}, () -> sb.append("NIL"));
		sb.append(" ");

		// Sender
		body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Sender).findFirst()
				.ifPresentOrElse(r -> mailboxToMail.accept(fromRecipient(r)), () -> sb.append("NIL"));
		sb.append(" ");

		// Reply-To
		sb.append("NIL ");

		// To
		var to = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Primary).toList();
		if (to.isEmpty()) {
			sb.append("NIL");
		} else {
			toMail.accept(to);
		}
		sb.append(" ");

		// Cc
		var cc = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.CarbonCopy).toList();
		if (cc.isEmpty()) {
			sb.append("NIL");
		} else {
			toMail.accept(cc);
		}
		sb.append(" ");

		// Bcc
		var bcc = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.BlindCarbonCopy).toList();
		if (bcc.isEmpty()) {
			sb.append("NIL");
		} else {
			toMail.accept(bcc);
		}
		sb.append(" ");

		// In-Reply-To
		sb.append("NIL ");

		// Message-Id
		sb.append("\"").append(body.get().messageId).append("\"");
		sb.append(") ");
		return Unpooled.wrappedBuffer(sb.toString().getBytes());
	}

	private ByteBuf headers(Supplier<MessageBody> body, Set<String> options, ItemValue<MailboxRecord> rec) {
		StringBuilder sb = new StringBuilder();
		for (String h : options) {
			switch (h.toLowerCase()) {
			case "message-id":
				if (!Strings.isNullOrEmpty(body.get().messageId)) {
					sb.append("Message-ID: " + body.get().messageId + "\r\n");
				}
				break;
			case "content-type":
				String ct = body.get().structure.mime;
				if (ct != null) {
					sb.append("Content-Type: " + ct + "\r\n");
				}
				break;
			case "subject":
				Optional.ofNullable(body.get().subject).ifPresent(s -> {
					Field sub = Fields.subject(body.get().subject);
					sb.append(writeField(sub));
				});
				break;
			case "from":
				body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Originator).findFirst()
						.ifPresent(r -> {
							Field from = Fields.from(fromRecipient(r));
							sb.append(writeField(from));
						});
				break;
			case "to":
				List<Address> toList = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.Primary)
						.map(this::fromRecipient).collect(Collectors.toList());
				if (!toList.isEmpty()) {
					AddressListField toField = Fields.to(toList);
					sb.append(writeField(toField));
				}
				break;
			case "cc":
				List<Address> ccList = body.get().recipients.stream().filter(r -> r.kind == RecipientKind.CarbonCopy)
						.map(this::fromRecipient).collect(Collectors.toList());
				if (!ccList.isEmpty()) {
					AddressListField ccField = Fields.cc(ccList);
					sb.append(writeField(ccField));
				}
				break;
			case "date":
				sb.append(DateUtil.toDateHeader(rec.value.internalDate));
				break;
			default:
				body.get().headers.stream().filter(head -> head.name.equalsIgnoreCase(h)).findAny().ifPresent(head -> {
					RawField rf = new RawField(head.name, head.firstValue());
					UnstructuredField field = UnstructuredFieldImpl.PARSER.parse(rf, DecodeMonitor.SILENT);
					sb.append(writeField(field));
				});
				break;
			}
		}
		sb.append(")\r\n");
		return Unpooled.wrappedBuffer(sb.toString().getBytes());
	}

	private Mailbox fromRecipient(Recipient r) {
		int idx = r.address.indexOf('@');
		if (idx > 0) {
			return new Mailbox(r.dn, r.address.substring(0, idx), r.address.substring(idx + 1));
		} else {
			return new Mailbox(r.dn, r.address, "invalid.email.domain");
		}
	}

	public String writeField(Field field) {
		StringBuilder buf = new StringBuilder();
		buf.append(field.getName());
		buf.append(": ");
		String body = field.getBody();
		if (body != null) {
			buf.append(body);
		}
		return MimeUtil.fold(buf.toString(), 0) + "\r\n";
	}

	private static final Path TMP = Paths.get(System.getProperty("java.io.tmpdir"));

	public static CompletableFuture<ByteBuf> readMmap(Stream s, int sizeHint) {
		try {
			MmapWriteStream out = new MmapWriteStream(TMP, sizeHint);
			ReadStream<Buffer> toRead = VertxStream.read(s);
			toRead.pipeTo(out);
			toRead.resume();
			return out.mmap();
		} catch (IOException e) {
			CompletableFuture<ByteBuf> ex = new CompletableFuture<>();
			ex.completeExceptionally(e);
			return ex;
		}
	}

}
