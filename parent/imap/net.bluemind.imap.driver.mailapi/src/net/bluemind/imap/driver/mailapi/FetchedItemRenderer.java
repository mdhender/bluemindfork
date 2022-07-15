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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.field.DateTimeField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.imap.endpoint.driver.MailPart;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.vertx.utils.MmapWriteStream;

public class FetchedItemRenderer {

	private static final Logger logger = LoggerFactory.getLogger(FetchedItemRenderer.class);

	private IDbMailboxRecords recApi;
	private SelectedFolder selected;
	private List<MailPart> fields;
	private IServiceProvider prov;

	public FetchedItemRenderer(IServiceProvider prov, IDbMailboxRecords recApi, SelectedFolder selected,
			List<MailPart> fields) {
		this.prov = prov;
		this.recApi = recApi;
		this.selected = selected;
		this.fields = fields;
	}

	public Map<String, ByteBuf> renderFields(ItemValue<MailboxRecord> rec) {
		Map<String, ByteBuf> ret = new HashMap<>();

		Supplier<MessageBody> body = Suppliers.memoize(() -> getBody(selected, rec));

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
			case "RFC822.SIZE":
				String size = Integer.toString(body.get().size);
				ret.put(f.toString(), Unpooled.wrappedBuffer(size.getBytes()));
				break;
			case "BODY.PEEK":
				ByteBuf bodyPeek = bodyPeek(recApi, body, f, rec);
				if (bodyPeek != null) {
					int len = bodyPeek.readableBytes();
					ByteBuf lenBuf = Unpooled.wrappedBuffer(("{" + len + "}\r\n").getBytes());
					bodyPeek = Unpooled.wrappedBuffer(lenBuf, bodyPeek);
					ret.put(f.toString(), bodyPeek);
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

	private MessageBody getBody(SelectedFolder selected, ItemValue<MailboxRecord> rec) {
		return Optional.ofNullable(rec.value.body).orElseGet(() -> {
			IContainers conApi = prov.instance(IContainers.class);
			ContainerDescriptor recContainer = conApi.get(IMailReplicaUids.mboxRecords(selected.folder.uid));
			CyrusPartition part = CyrusPartition.forServerAndDomain(recContainer.datalocation, recContainer.domainUid);
			IDbMessageBodies bodyApi = prov.instance(IDbMessageBodies.class, part.name);
			return bodyApi.getComplete(rec.value.messageBody);
		});
	}

	private ByteBuf bodyPeek(IDbMailboxRecords recApi, Supplier<MessageBody> body, MailPart f,
			ItemValue<MailboxRecord> rec) {
		if (f.section.equalsIgnoreCase("header.fields")) {
			StringBuilder sb = new StringBuilder();
			for (String h : f.options) {
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
				case "date":
					DateTimeField dateField = Fields.date("Date", rec.value.internalDate);
					sb.append(writeField(dateField));
					break;
				default:
					logger.warn("{} is not managed", h);
					break;
				}
			}
			sb.append("\r\n");
			return Unpooled.wrappedBuffer(sb.toString().getBytes());
		} else if (f.section.isEmpty()) {
			// full eml
			Stream fullMsg = recApi.fetchComplete(rec.value.imapUid);
			int len = body.get().size;
			return readMmap(fullMsg, len).join();
		} else {
			logger.warn("unknown section '{}'", f.section);
		}

		return null;
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
