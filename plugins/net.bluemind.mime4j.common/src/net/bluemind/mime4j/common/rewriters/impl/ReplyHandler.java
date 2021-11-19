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
package net.bluemind.mime4j.common.rewriters.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.RawField;
import org.apache.james.mime4j.util.CharsetUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.utils.FileUtils;

public class ReplyHandler extends DontTouchHandler {

	private static final Logger logger = LoggerFactory.getLogger(ReplyHandler.class);

	private Message replied;
	private boolean keepAttachments;

	/**
	 * @param entity
	 * @param bf
	 * @param defaultFrom
	 * @param toAnswer
	 * @param keepAttachments
	 */
	public ReplyHandler(Message entity, BodyFactory bf, Mailbox defaultFrom, InputStream toAnswer,
			boolean keepAttachments) {
		super(entity, bf, defaultFrom);
		replied = Mime4JHelper.makeUtf8Compatible(toAnswer);
		this.keepAttachments = keepAttachments;
		try {
			toAnswer.close();
		} catch (IOException e) {
		}
	}

	@Override
	protected Message firstRewrite(Message parsed) {
		logger.info("Rewrite message: {}", parsed.getClass().getCanonicalName());

		Message ret = parsed;

		Body body = ret.getBody();
		Multipart mp;
		if (body instanceof Multipart) {
			mp = (Multipart) body;
		} else {

			TextBody tb = (TextBody) body;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			BasicBodyFactory bodyFactory = new BasicBodyFactory();

			try {
				FileUtils.transfer(tb.getInputStream(), out, true);
			} catch (IOException ioe) {
				logger.error(ioe.getMessage(), ioe);
			}

			BodyPart bp = createTextPart(bodyFactory, new String(out.toByteArray()));
			mp = new MultipartImpl("mixed");
			mp.addBodyPart(bp);

		}

		List<Entity> parts = mp.getBodyParts();
		List<AddressableEntity> expParts = Mime4JHelper.expandParts(parts);
		MultipartImpl mixedParent = new MultipartImpl("mixed");
		MultipartImpl relatedParent = new MultipartImpl("related");
		BodyPart bp = new BodyPart();
		bp.setMultipart(relatedParent);
		mixedParent.addBodyPart(bp);

		Map<String, Set<Entity>> allParts = new HashMap<>();
		List<Entity> attachments = new ArrayList<>();

		for (Entity e : expParts) {
			if (e.getMimeType() != null && e.getMimeType().startsWith("text/") && !Mime4JHelper.isAttachment(e)) {
				Body b = e.getBody();
				if (b instanceof TextBody) {
					TextBody tb = (TextBody) b;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					try {
						FileUtils.transfer(tb.getInputStream(), out, true);
						String reply = new String(out.toByteArray());
						allParts.put(e.getMimeType(), concatOriginalParts(reply, e.getMimeType().trim()));
					} catch (IOException ioe) {
						logger.error(ioe.getMessage(), ioe);
					}
				}
			} else {
				attachments.add(e);
			}
		}

		if (!allParts.isEmpty()) {
			// BM-8453 quick fix
			if (allParts.containsKey("text/html")) {
				boolean first = true;
				for (Entity e : allParts.get("text/html")) {
					Field cid = e.getHeader().getField("Content-ID");
					// BM-10503 push attachments with content-ids insert a
					// related multipart (inline images in signature),
					// but push the other ones in a multipart mixed the
					// thunderbird's forward grabs them correctly
					if (cid != null || first) {
						relatedParent.addBodyPart(e);
					} else {
						mixedParent.addBodyPart(e);
					}
					first = false;
				}
			} else if (allParts.containsKey("text/plain")) {
				boolean first = true;
				for (Entity e : allParts.get("text/plain")) {
					Field cid = e.getHeader().getField("Content-ID");
					if (cid != null || first) {
						relatedParent.addBodyPart(e);
					} else {
						mixedParent.addBodyPart(e);
					}
					first = false;
				}
			}

			for (Entity e : attachments) {
				Field cid = e.getHeader().getField("Content-ID");
				if (cid != null) {
					relatedParent.addBodyPart(e);
				} else {
					mixedParent.addBodyPart(e);
				}
			}

			MessageImpl msg = new MessageImpl();
			msg.setFrom(ret.getFrom());
			msg.setTo(ret.getTo());
			msg.setCc(ret.getCc());
			msg.setBcc(ret.getBcc());
			msg.setSubject(ret.getSubject());

			HeaderImpl h = new HeaderImpl();
			Header reh = ret.getHeader();
			copyHeaderField(h, reh, FieldName.CONTENT_TYPE);
			copyHeaderField(h, reh, FieldName.CONTENT_TRANSFER_ENCODING);

			msg.setMultipart(mixedParent);

			ret = msg;
		} else {
			// Workaround: add replied as attachment
			logger.warn("unable to concat response to orig msg, we add original body parts as attachment");
			addOriginalParts(mp);
		}

		Header h = ret.getHeader();
		Header repliedHeader = replied.getHeader();

		Field messageId = repliedHeader.getField(FieldName.MESSAGE_ID);
		if (messageId != null) {
			Field inReplyTo = new RawField("In-Reply-To", messageId.getBody());
			h.setField(inReplyTo);
			Field references = new RawField("References", messageId.getBody());
			h.setField(references);
		} else {
			logger.error(
					"Replied message has no MESSAGE_ID header. Does replied message still in INBOX? Original part may not be append");
		}

		ret.setDate(new Date());

		return ret;
	}

	/**
	 * @param bodyFactory
	 * @param text
	 * @return
	 */
	private static BodyPart createTextPart(BasicBodyFactory bodyFactory, String text) {
		TextBody body;
		try {
			body = bodyFactory.textBody(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported encoding");
		}
		BodyPart bodyPart = new BodyPart();
		bodyPart.setText(body, "html");
		return bodyPart;
	}

	/**
	 * @param mp
	 * @param reply
	 * @param mime
	 */
	private Set<Entity> concatOriginalParts(String reply, String mime) {
		logger.info("concat reply to original parts");

		boolean parsedBodyHtml = mime.equals("text/html");

		String anwser = "";

		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		List<Entity> attachments = new ArrayList<>();
		if (replied.isMultipart()) {
			Multipart repMulti = (Multipart) replied.getBody();
			List<Entity> parts = repMulti.getBodyParts();
			List<AddressableEntity> expParts = Mime4JHelper.expandParts(parts);
			Entity htmlPart = null;
			Entity textPart = null;
			for (Entity e : expParts) {
				if (e.getMimeType() != null && e.getMimeType().startsWith("text/") && !Mime4JHelper.isAttachment(e)) {
					if ("text/html".equals(e.getMimeType())) {
						htmlPart = e;
					} else if ("text/plain".equals(e.getMimeType())) {
						textPart = e;
					}
				} else {
					if (Mime4JHelper.isAttachment(e) && keepAttachments) {
						attachments.add(e);
					} else {
						logger.info("Skipping non-text part: " + e.getMimeType());
					}
				}
			}
			if (htmlPart != null) {
				anwser = insertQuotePart(parsedBodyHtml, reply, htmlPart);
			} else if (textPart != null) {
				anwser = insertQuotePart(parsedBodyHtml, reply, textPart);
			} else {
				anwser = reply;
			}
		} else {
			anwser = insertQuotePart(parsedBodyHtml, reply, replied);
		}
		TextBody body = bodyFactory.textBody(anwser, CharsetUtil.UTF_8);
		BodyPart bodyPart = new BodyPart();
		bodyPart.setBody(body);
		HeaderImpl h = new HeaderImpl();

		if (parsedBodyHtml) {
			h.setField(Fields.contentType("text/html; charset=utf-8"));
		} else {
			h.setField(Fields.contentType("text/plain; charset=utf-8"));
		}
		bodyPart.setHeader(h);
		bodyPart.setContentTransferEncoding("base64");

		Set<Entity> ret = new LinkedHashSet<>();
		ret.add(bodyPart);
		ret.addAll(attachments);
		return ret;
	}

	/**
	 * @param reply
	 * @param parsedBodyHtml
	 * @param quote
	 * @param e
	 * @param body
	 */
	private String insertQuotePart(boolean parsedBodyHtml, String reply, Entity e) {
		String quotePart = null;
		TextBody tb = (TextBody) e.getBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(tb.getInputStream(), out, true);
			String charset = e.getCharset();
			if (charset == null) {
				charset = "utf-8";
			}
			quotePart = new String(out.toByteArray(), charset);
		} catch (IOException ioe) {
			logger.error(ioe.getMessage(), ioe);
		}

		if (parsedBodyHtml) {
			if ("text/html".equals(e.getBody().getParent().getMimeType())) {

				try {
					Document doc = Jsoup.parse(reply);
					Elements blockquotes = doc.getElementsByTag("blockquote");
					if (!blockquotes.isEmpty()) {
						Element blockquote = blockquotes.get(0);
						Element fragementBody = Jsoup.parseBodyFragment(quotePart).body();
						blockquote.prependChild(fragementBody);
						return doc.html();
					}

					Elements bodies = doc.getElementsByTag("body");
					if (!bodies.isEmpty()) {
						Element body = bodies.get(0);
						String blockquote = "<blockquote type=\"cite\" style=\"padding-left:5px; border-left:2px solid #1010ff; margin-left:5px\">"
								+ quotePart + "</blockquote>";
						Element fragementBody = Jsoup.parseBodyFragment(blockquote).body();
						body.appendChild(fragementBody);
						return doc.html();
					}

					return reply
							+ "<blockquote type=\"cite\" style=\"padding-left:5px; border-left:2px solid #1010ff; margin-left:5px\">"
							+ quotePart + "</blockquote>";

				} catch (Exception ex) {
					logger.error(ex.getMessage(), ex);
					return reply
							+ "<blockquote type=\"cite\" style=\"padding-left:5px; border-left:2px solid #1010ff; margin-left:5px\">"
							+ quotePart + "</blockquote>";
				}
			} else if ("text/plain".equals(e.getBody().getParent().getMimeType())) {
				StringBuilder sb = new StringBuilder();
				sb.append(reply);
				sb.append(quotePart.replaceAll("\\n", "<br/>"));
				return sb.toString();
			}
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(reply);

			String[] quoteLines = quotePart.split("\n");
			for (String line : quoteLines) {
				sb.append(">");
				sb.append(line);
				sb.append("\n");
			}
			return sb.toString();
		}

		return reply;
	}

	private void addOriginalParts(Multipart mi) {
		if (replied.isMultipart()) {
			Multipart repMulti = (Multipart) replied.getBody();
			List<Entity> parts = repMulti.getBodyParts();
			List<AddressableEntity> expParts = Mime4JHelper.expandParts(parts);
			List<Entity> attachments = new ArrayList<Entity>();
			for (Entity e : expParts) {
				if (e.getMimeType() != null && e.getMimeType().startsWith("text/") && !Mime4JHelper.isAttachment(e)) {
					BodyPart bpa = new BodyPart();
					bpa.setBody(e.getBody());
					bpa.setHeader(e.getHeader());
					bpa.setContentTransferEncoding("base64");
					bpa.setContentDisposition("inline");
					mi.addBodyPart(bpa);
				} else {
					if (keepAttachments) {
						attachments.add(e);
					} else {
						logger.info("Skipping non-text part: " + e.getMimeType());
					}
				}
			}

			// ensure attachments are in the end
			for (Entity attachment : attachments) {
				mi.addBodyPart(attachment);
			}

		} else {
			BodyPart bpa = new BodyPart();
			HeaderImpl h = new HeaderImpl();
			Header reh = replied.getHeader();
			copyHeaderField(h, reh, FieldName.CONTENT_TYPE);
			bpa.setBody(replied.getBody());
			bpa.setHeader(h);
			bpa.setContentTransferEncoding("base64");
			mi.addBodyPart(bpa);
		}
	}

	private void copyHeaderField(HeaderImpl newHeader, Header source, String field) {
		Field f = source.getField(field);
		if (f != null) {
			newHeader.addField(f);
		}
	}
}
