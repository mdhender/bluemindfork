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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.Mime4JHelper.SizedStream;

public class EmlBuilder {

	private static final Logger logger = LoggerFactory.getLogger(EmlBuilder.class);

	private EmlBuilder() {
	}

	public static Message of(MessageBody mb, String owner) {

		MessageImpl msg = new MessageImpl();
		msg.setDate(mb.date);
		BasicBodyFactory bbf = new BasicBodyFactory();
		logger.info("Subject is '{}'", mb.subject);
		msg.setSubject(mb.subject);
		try {
			fillHeader(msg.getHeader(), mb.headers, true);
		} catch (MimeException e1) {
			logger.error(e1.getMessage(), e1);
		}
		setRecipients(msg, mb.recipients);

		Part structure = mb.structure;
		try {
			Body body = createBody(bbf, mb.structure, owner);
			if (body instanceof MultipartImpl) {
				msg.setMultipart((MultipartImpl) body);
			} else {
				setBody(msg, body, structure);
			}

		} catch (IOException e) {
			msg.setBody(bbf.textBody("CRAP: " + e.getMessage(), StandardCharsets.UTF_8));
		}

		return msg;
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
			Mailbox cur = new Mailbox(local, dom);
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
			return s;
		}
	}

	private static Body createBody(BasicBodyFactory bbf, Part structure, String owner) throws IOException {
		Body body = null;
		if (structure.children.isEmpty()) {
			switch (structure.mime) {
			case "text/plain":
			case "text/html":
				body = bbf.textBody(inputStream(null, null, null, structure, owner).input, "utf-8");
				break;
			default:
				body = bbf.binaryBody(inputStream(null, null, null, structure, owner).input);
			}
		} else {
			// multipart
			MultipartImpl mp = new MultipartImpl(structure.mime.substring("multipart/".length()));
			for (Part p : structure.children) {
				logger.info("Adding part {}", p.mime);
				Body childBody = createBody(bbf, p, owner);
				BodyPart bp = new BodyPart();
				if (childBody instanceof MultipartImpl) {
					bp.setMultipart((MultipartImpl) childBody);
				} else {
					setBody(bp, childBody, p);
				}
				Header partHeader = bp.getHeader();
				try {
					fillHeader(partHeader, p.headers, true);
				} catch (MimeException e) {
					logger.error(e.getMessage(), e);
				}
				mp.addBodyPart(bp);
			}
			body = mp;
		}
		return body;
	}

	private static void fillHeader(Header partHeader, List<net.bluemind.backend.mail.api.MessageBody.Header> headers,
			boolean replace) throws MimeException {
		for (net.bluemind.backend.mail.api.MessageBody.Header h : headers) {
			if (replace || h.name.equals("Content-Type") || h.name.equals("Content-Transfer-Encoding")) {
				partHeader.removeFields(h.name);
			}
			if (h.values.size() == 1) {
				ParsedField parsed = LenientFieldParser.parse(h.name + ": " + h.values.get(0));
				partHeader.addField(parsed);
			} else {
				logger.warn("Skipping multivalued {}", h.name);
			}
		}
	}

	private static InputStream stream(File f) throws IOException {
		return new BufferedInputStream(Files.newInputStream(f.toPath(), StandardOpenOption.READ));
	}

	public static SizedStream inputStream(Long id, String previousBody, Date date, Part structure, String owner) {
		Objects.requireNonNull(structure.address, "Part address must not be null");
		File emlInput = new File(Bodies.STAGING, structure.address + ".part");
		if (!emlInput.exists()) {
			throw ServerFault.notFound("Missing staging file " + emlInput.getAbsolutePath());
		}
		if (id == null) {
			try {
				InputStream input = stream(emlInput);
				SizedStream ss = new SizedStream();
				ss.input = input;
				ss.size = (int) emlInput.length();
				return ss;
			} catch (IOException e) {
				throw Throwables.propagate(e);
			}
		}
		try (InputStream in = stream(emlInput); Message asMessage = Mime4JHelper.parse(in)) {
			net.bluemind.backend.mail.api.MessageBody.Header idHeader = net.bluemind.backend.mail.api.MessageBody.Header
					.create(MailApiHeaders.X_BM_INTERNAL_ID, owner + "#" + InstallationId.getIdentifier() + ":" + id);
			List<net.bluemind.backend.mail.api.MessageBody.Header> toAdd = Arrays.asList(idHeader);
			if (previousBody != null) {
				net.bluemind.backend.mail.api.MessageBody.Header prevHeader = net.bluemind.backend.mail.api.MessageBody.Header
						.create(MailApiHeaders.X_BM_PREVIOUS_BODY, previousBody);
				toAdd = Arrays.asList(idHeader, prevHeader);
			}
			asMessage.setDate(date);
			fillHeader(asMessage.getHeader(), toAdd, true);
			return Mime4JHelper.asSizedStream(asMessage);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
