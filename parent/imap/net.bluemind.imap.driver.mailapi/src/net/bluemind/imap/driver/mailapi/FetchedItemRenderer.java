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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.WithId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.lib.vertx.utils.MmapWriteStream;
import net.bluemind.mime4j.common.Mime4JHelper;

public class FetchedItemRenderer {

	private static final Logger logger = LoggerFactory.getLogger(FetchedItemRenderer.class);

	private static final ByteBuf DEFAULT_NIL_BYTE_BUF = Unpooled.wrappedBuffer("NIL".getBytes());
	private static final ByteBuf DEFAULT_EMPTY_BYTE_BUF = Unpooled.wrappedBuffer("".getBytes());
	private static final String SECTION_MIME_PATTERN = ".MIME";
	private static final String SECTION_HEADER_PATTERN = ".HEADER";
	private static final ByteBuf TWO_CRLF = Unpooled.wrappedBuffer("\r\n\r\n".getBytes());
	private final IDbMailboxRecords recApi;
	private final List<MailPart> fields;
	private final IDbMessageBodies bodyApi;
	private final IMailboxItems itemsApi;
	private static final Set<String> DEFAULT_HEADERS = Sets.newHashSet("From", "To", "Cc", "Subject", "Message-ID",
			"Date", "Content-Type", "X-Bm-Event", "X-Bm-Todo", "X-BM-ResourceBooking", "X-BM-Event-Countered",
			"X-BM-Event-Canceled", "X-BM-Event-Replied", "X-BM-FOLDERSHARING", "X-ASTERISK-CALLERID");

	public FetchedItemRenderer(IDbMessageBodies bodyApi, IDbMailboxRecords recApi, IMailboxItems itemsApi,
			List<MailPart> fields) {
		this.recApi = recApi;
		this.fields = fields;
		this.bodyApi = bodyApi;
		this.itemsApi = itemsApi;
	}

	public IDbMailboxRecords recApi() {
		return recApi;
	}

	public Map<String, ByteBuf> renderFields(WithId<MailboxRecord> rec) {
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
			case "RFC822.HEADER":
				ret.put(f.toString(), literalize(headers(body, DEFAULT_HEADERS, rec)));
				break;
			case "ENVELOPE":
				ret.put(f.toString(), EnvelopeRenderer.render(body, rec));
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
				logger.warn("Unsupported fetch field `{}'", f);
				break;
			}
		}
		return ret;
	}

	private ByteBuf literalize(ByteBuf bodyPeek) {
		int len = bodyPeek.readableBytes();
		if (Buffer.buffer(bodyPeek).toString().equals("NIL")) {
			return Unpooled.wrappedBuffer(bodyPeek);
		}
		if (bodyPeek.capacity() == 0) {
			ByteBuf doubleQuoteBuffer = Unpooled.wrappedBuffer(("\"\"").getBytes());
			return Unpooled.wrappedBuffer(doubleQuoteBuffer, bodyPeek);
		}
		ByteBuf lenBuf = Unpooled.wrappedBuffer(("{" + len + "}\r\n").getBytes());
		return Unpooled.wrappedBuffer(lenBuf, bodyPeek);
	}

	private MessageBody getBody(WithId<MailboxRecord> rec) {
		return Optional.ofNullable(rec.value.body).orElseGet(() -> bodyApi.getComplete(rec.value.messageBody));
	}

	private ByteBuf bodyPeek(IDbMailboxRecords recApi, Supplier<MessageBody> body, MailPart f,
			WithId<MailboxRecord> rec) {
		String section = f.section == null ? "" : f.section;
		String partialString = f.partial == null ? "" : f.partial;
		Partial partial = new Partial(partialString);

		if (!partialString.isEmpty() && !partial.isValid) {
			return null;
		}
		if (section.equalsIgnoreCase("header.fields")) {
			return headers(body, f.options != null ? f.options : DEFAULT_HEADERS, rec);
		} else if (section.equalsIgnoreCase("header")) {
			return headers(body, DEFAULT_HEADERS, rec);
		} else if (section.equalsIgnoreCase("text")) {
			return bodyText(recApi, body, rec, partial);
		} else if (section.endsWith(SECTION_MIME_PATTERN) && partAddr(section.replace(SECTION_MIME_PATTERN, ""))) {
			return mimePattern(body, f, partial);
		} else if (section.endsWith(SECTION_HEADER_PATTERN) && partAddr(section.replace(SECTION_HEADER_PATTERN, ""))) {
			return sectionHeaderPattern(body, f, rec, partial);
		} else if (partAddr(section)) {
			return bodyPart(recApi, body, f, rec);
		} else if (section.isEmpty()) {
			return fullEml(recApi, body, rec, partial);
		} else {
			logger.warn("unknown section '{}'", f.section);
		}

		return null;
	}

	private ByteBuf bodyPart(IDbMailboxRecords recApi, Supplier<MessageBody> body, MailPart f,
			WithId<MailboxRecord> rec) {
		// load eml part
		try {
			Stream fullMsg = recApi.fetchComplete(rec.value.imapUid);
			int len = body.get().size;
			ByteBuf emlBuffer = readMmap(fullMsg, len * 2).join();
			return getPart(emlBuffer, f.section);
		} catch (ServerFault sf) {
			logger.error("could not fetch part  {}[{}]: {}", rec.value.imapUid, f.section, sf.getMessage());
			return null;
		}
	}

	private ByteBuf fullEml(IDbMailboxRecords recApi, Supplier<MessageBody> body, WithId<MailboxRecord> rec,
			Partial partial) {
		// full eml
		try {
			Stream fullMsg = recApi.fetchComplete(rec.value.imapUid);
			int len = body.get().size;
			ByteBuf byteBuf = readMmap(fullMsg, len * 2).join();
			if (partial.isValid) {
				return partial.getByteBufSlice(byteBuf);
			}
			return byteBuf;
		} catch (ServerFault sf) {
			logger.error("could not fetch {}: {}", rec.value.imapUid, sf.getMessage());
			return null;
		}
	}

	private ByteBuf sectionHeaderPattern(Supplier<MessageBody> body, MailPart f, WithId<MailboxRecord> rec,
			Partial partial) {
		String part = f.section.replace(SECTION_HEADER_PATTERN, "");
		logger.info("Fetch headers of {}", part);
		return body.get().structure.parts().stream().filter(p -> p.address.equalsIgnoreCase(part))
				.filter(p -> p.mime.equals("message/rfc822")).findAny().map(p -> {
					Stream messagePart = itemsApi.fetch(rec.value.imapUid, p.address, p.encoding, p.mime, p.charset,
							p.fileName);
					int len = p.size;
					ByteBuf emlBuffer = readMmap(messagePart, len * 2).join();
					emlBuffer.markReaderIndex();
					int headerEndIndex = ByteBufUtil.indexOf(TWO_CRLF.duplicate(), emlBuffer);
					if (headerEndIndex < 0) {
						return DEFAULT_NIL_BYTE_BUF;
					}
					emlBuffer.markReaderIndex();
					if (partial.isValid) {
						return partial.getByteBufSlice(emlBuffer);
					}
					return emlBuffer.slice(0, headerEndIndex);
				}).orElse(DEFAULT_NIL_BYTE_BUF);
	}

	private ByteBuf mimePattern(Supplier<MessageBody> body, MailPart f, Partial partial) {
		String part = f.section.replace(SECTION_MIME_PATTERN, "");
		logger.info("Fetch mime headers of {}", part);
		return body.get().structure.parts().stream().filter(p -> p.address.equalsIgnoreCase(part)).findAny().map(p -> {
			StringBuilder b = new StringBuilder();
			b.append("Content-Type: " + p.mime + "\r\n");
			// core returns a decoded version
			b.append("Content-Transfer-Encoding: 8bit\r\n");
			if (p.contentId != null) {
				b.append("Content-ID: " + p.contentId + "\r\n");
			}
			b.append("\r\n");
			if (partial.isValid) {
				return partial.getByteBufSlice(Unpooled.wrappedBuffer(b.toString().getBytes()));
			}
			return Unpooled.wrappedBuffer(b.toString().getBytes());
		}).orElse(DEFAULT_NIL_BYTE_BUF);
	}

	private ByteBuf bodyText(IDbMailboxRecords recApi, Supplier<MessageBody> body, WithId<MailboxRecord> rec,
			Partial partial) {
		Stream fullMsg = recApi.fetchComplete(rec.value.imapUid);
		int len = body.get().size;
		ByteBuf emlBuffer = readMmap(fullMsg, len * 2).join();
		emlBuffer.markReaderIndex();
		int headerEndIndex = ByteBufUtil.indexOf(TWO_CRLF.duplicate(), emlBuffer);
		if (headerEndIndex < 0) {
			return null;
		}
		emlBuffer.readerIndex(headerEndIndex + 4);
		emlBuffer.markReaderIndex();
		if (partial.isValid) {
			return partial.getByteBufSlice(emlBuffer);
		}
		return emlBuffer.slice(headerEndIndex + 4, len);
	}

	private boolean partAddr(String s) {
		return !Strings.isNullOrEmpty(s) && CharMatcher.inRange('0', '9').or(CharMatcher.is('.')).matchesAllOf(s);
	}

	private ByteBuf getPart(ByteBuf emlBuffer, String section) {
		try (InputStream in = new ByteBufInputStream(emlBuffer, true); Message parsed = Mime4JHelper.parse(in, false)) {
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

	private ByteBuf headers(Supplier<MessageBody> body, Set<String> options, WithId<MailboxRecord> rec) {
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
					String mime = body.get().structure.mime;
					Field ctf = Fields.contentType(mime, contentTypeParams(mime));
					sb.append(writeField(ctf));
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
		sb.append("\r\n");
		return Unpooled.wrappedBuffer(sb.toString().getBytes());
	}

	private Map<String, String> contentTypeParams(String mime) {
		if (mime.toLowerCase().startsWith("multipart/")) {
			return Map.of("boundary", "-=Part.TEXT=-");
		} else {
			return Collections.emptyMap();
		}
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

	private CompletableFuture<ByteBuf> readMmap(Stream s, int sizeHint) {
		try {
			MmapWriteStream out = new MmapWriteStream(TMP, sizeHint);
			ReadStream<Buffer> toRead = VertxStream.read(s);
			toRead.pipeTo(out);
			toRead.resume();
			return out.mmap();
		} catch (IOException e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	private class Partial {
		public final boolean isValid;
		public final int offset;
		public final int length;

		private record PartialBoundaries(int offset, int length) {
		}

		public Partial(String partial) {
			isValid = isValidPartial(partial);
			PartialBoundaries pb = getPartialBoundaries(partial);
			offset = pb.offset;
			length = pb.length;
		}

		private boolean isValidPartial(String partial) {
			String[] partialArray = partial.split("\\.");
			if (partialArray.length != 2) {
				return false;
			}
			for (int i = 0; i < partialArray.length; i++) {
				try {
					Integer.parseInt(partialArray[i]);
				} catch (NumberFormatException e) {
					return false;
				}
			}
			return true;
		}

		private PartialBoundaries getPartialBoundaries(String partial) {
			if (!isValid) {
				return new PartialBoundaries(0, 0);
			}
			int[] partialArray = Arrays.asList(partial.split("\\.")).stream().mapToInt(Integer::parseInt).toArray();
			return new PartialBoundaries(partialArray[0], partialArray[1]);
		}

		public ByteBuf getByteBufSlice(ByteBuf fullByteBuf) {
			if (offset > fullByteBuf.readableBytes()) {
				return DEFAULT_EMPTY_BYTE_BUF.duplicate();
			}
			if ((offset + length) > fullByteBuf.readableBytes()) {
				int finalIndex = fullByteBuf.readableBytes() - offset;
				return fullByteBuf.slice(offset, finalIndex);
			}
			return fullByteBuf.slice(offset, length);
		}
	}

}
