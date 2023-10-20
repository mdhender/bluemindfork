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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.mail.internet.MimeUtility;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ParsedField;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.message.AbstractEntity;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.Mime4JHelper.HashedBuffer;

public class EmlBuilder {

	private static final Logger logger = LoggerFactory.getLogger(EmlBuilder.class);

	private EmlBuilder() {
	}

	public static Message of(MessageBody mb, String sid) {

		MessageImpl msg = new MessageImpl();
		msg.setDate(mb.date);
		BasicBodyFactory bbf = new BasicBodyFactory();
		logger.info("Subject is '{}'", mb.subject);
		msg.setSubject(mb.subject);
		fillHeader(msg.getHeader(), mb.headers, true);
		setRecipients(msg, mb.recipients);

		Part structure = mb.structure;
		try {
			Body body = createBody(bbf, mb.structure, sid);
			if (body instanceof MultipartImpl mpImpl) {
				Map<String, String> parameters = new HashMap<String, String>();
				parameters.put("boundary", MimeUtil.createUniqueBoundary());
				handleMultipartReport(mb, parameters);
				msg.setBody(body, mb.structure.mime, parameters);
			} else {
				setBody(msg, body, structure);
			}
			Header partHeader = msg.getHeader();
			fillHeader(partHeader, mb.structure.headers, true);
		} catch (IOException e) {
			msg.setBody(bbf.textBody("CRAP: " + e.getMessage(), StandardCharsets.UTF_8));
		}

		return msg;
	}

	/**
	 * In case of a "multipart/report" mime type, set the required "Content-Type"
	 * header parameter "report-type".
	 * 
	 * @see https://www.rfc-editor.org/rfc/rfc6522#section-3
	 */
	private static void handleMultipartReport(MessageBody messageBody, Map<String, String> parameters) {
		if ("multipart/report".equalsIgnoreCase(messageBody.structure.mime)
				&& messageBody.structure.children.size() >= 2) {
			String[] secondChildMime = messageBody.structure.children.get(1).mime.split("/");
			String secondChildSubType = secondChildMime.length > 1 ? secondChildMime[1] : "";
			parameters.put("report-type", secondChildSubType);
		}
	}

	private static void setRecipients(MessageImpl msg, List<Recipient> recipients) {
		Mailbox from = null;
		Mailbox sender = null;
		List<Address> to = new LinkedList<>();
		List<Address> cc = new LinkedList<>();
		List<Address> bcc = new LinkedList<>();

		Splitter splitter = Splitter.on("@");
		for (Recipient r : recipients) {
			Iterator<String> split = splitter.split(r.address).iterator();
			String local = split.next();
			String dom = null;
			if (split.hasNext()) {
				dom = split.next();
			}
			Mailbox cur = new Mailbox(r.dn, local, dom);
			switch (r.kind) {
			case BlindCarbonCopy:
				bcc.add(cur);
				break;
			case CarbonCopy:
				cc.add(cur);
				break;
			case Originator:
				from = cur;
				break;
			case Primary:
				to.add(cur);
				break;
			case Sender:
				sender = cur;
				break;
			}
		}
		msg.setFrom(from);
		if (sender != null) {
			msg.setSender(sender);
		}
		if (!to.isEmpty()) {
			msg.setTo(to);
		}
		if (!cc.isEmpty()) {
			msg.setCc(cc);
		}
		if (!bcc.isEmpty()) {
			msg.setBcc(bcc);
		}
	}

	private static void setBody(AbstractEntity ae, Body b, Part p) {
		Map<String, String> bodyParams = new HashMap<>();
		if (p.charset != null) {
			bodyParams.put("charset", p.charset);
		}
		ae.setBody(b, p.mime, bodyParams);
		if (p.encoding != null) {
			ae.setContentTransferEncoding(p.encoding);
		}
		if (p.fileName != null && p.dispositionType == DispositionType.ATTACHMENT) {
			ae.setContentDisposition(ContentDispositionField.DISPOSITION_TYPE_ATTACHMENT, safeEncode(p.fileName));
		} else if (p.dispositionType == DispositionType.INLINE && p.contentId != null) {
			if (p.fileName == null) {
				ae.setContentDisposition(ContentDispositionField.DISPOSITION_TYPE_INLINE);
			} else {
				ae.setContentDisposition(ContentDispositionField.DISPOSITION_TYPE_INLINE, safeEncode(p.fileName));
			}
			try {
				String cid = p.contentId.startsWith("<") ? p.contentId : "<" + p.contentId + ">";
				ae.getHeader().addField(LenientFieldParser.parse("Content-ID: " + cid));
			} catch (MimeException e) {
				logger.warn("Failed to set content-id to {}: {}", p.contentId, e.getMessage());
			}
		}
	}

	private static String safeEncode(String s) {
		try {
			return MimeUtility.encodeWord(s, "utf-8", "Q");
		} catch (UnsupportedEncodingException e) {
			// should not happen as utf-8 is always available
			logger.error(e.getMessage(), e);
			return "broken-name-" + System.nanoTime() + ".bin";
		}
	}

	private static Body createBody(BasicBodyFactory bbf, Part structure, String sid) throws IOException {
		Body body = null;
		if (structure.children.isEmpty()) {
			try (InputStream structureInputStream = partStream(structure, sid)) {
				if (structure.mime.startsWith("text/")) {
					body = bbf.textBody(structureInputStream, "utf-8");
				} else {
					body = bbf.binaryBody(structureInputStream);
				}
			}
		} else {
			MultipartImpl mp = new MultipartImpl(structure.mime.substring("multipart/".length()));
			for (Part p : structure.children) {
				logger.info("Adding part {}", p.mime);
				Body childBody = createBody(bbf, p, sid);
				BodyPart bp = new BodyPart();
				if (childBody instanceof MultipartImpl multiPartImpl) {
					bp.setMultipart(multiPartImpl);
				} else {
					setBody(bp, childBody, p);
				}
				Header partHeader = bp.getHeader();
				fillHeader(partHeader, p.headers, true);
				mp.addBodyPart(bp);
			}
			body = mp;
		}
		return body;
	}

	private static final Set<String> IGNORED_CLIENT_HEADERS = Sets.newHashSet("content-disposition");

	private static void fillHeader(Header partHeader, List<net.bluemind.backend.mail.api.MessageBody.Header> headers,
			boolean replace) {
		for (net.bluemind.backend.mail.api.MessageBody.Header header : headers) {
			if (IGNORED_CLIENT_HEADERS.contains(header.name.toLowerCase())) {
				logger.warn("Ignoring client provided {}: {}", header.name, header.values);
				continue;
			}

			if (replace || header.name.equals("Content-Type") || header.name.equals("Content-Transfer-Encoding")) {
				partHeader.removeFields(header.name);
			}

			if (header.values.size() == 1) {
				try {
					String headerName = header.name + ": ";
					String value = MimeUtil.fold(header.values.get(0), headerName.length());
					ParsedField headerField = LenientFieldParser.parse(headerName + value);
					partHeader.addField(headerField);
				} catch (MimeException e) {
					logger.warn("Cannot add header {}", header.name, e);
				}
			} else {
				logger.warn("Skipping multivalued {}", header.name);
			}
		}
	}

	private static InputStream stream(File f) throws IOException {
		return new BufferedInputStream(Files.newInputStream(f.toPath(), StandardOpenOption.READ));
	}

	private static File emlFile(Part structure, String sid) {
		Objects.requireNonNull(structure.address, "Part address must not be null");
		File emlFile = new File(Bodies.getFolder(sid), structure.address + ".part");
		if (!emlFile.exists()) {
			throw ServerFault.notFound("Missing staging file " + emlFile.getAbsolutePath());
		}
		return emlFile;
	}

	private static InputStream partStream(Part structure, String sid) throws IOException {
		final File emlInput = emlFile(structure, sid);
		return stream(emlInput);
	}

	public static HashedBuffer inputStream(Date date, Part structure, String owner, String sid) {
		final File emlInput = emlFile(structure, sid);
		try (InputStream in = stream(emlInput); Message asMessage = Mime4JHelper.parse(in, false)) {
			if (date != null) {
				asMessage.setDate(date);
			}
			fillHeader(asMessage.getHeader(), Collections.emptyList(), true);
			return Mime4JHelper.mmapedEML(asMessage, false);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

}
