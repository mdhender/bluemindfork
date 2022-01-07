/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.signature.commons.action;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.field.ContentTypeField;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.RawField;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.io.CharStreams;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class AddDisclaimer {

	private BasicBodyFactory bodyFactory;
	private DisclaimerVariables variables;
	private static final String CRLF = "\r\n";
	private static final String PLACEHOLDER = "--X-BM-SIGNATURE--";

	public AddDisclaimer(Supplier<Optional<VCard>> vCardSupplier) {
		bodyFactory = new BasicBodyFactory();
		variables = new DisclaimerVariables(vCardSupplier);
	}

	private TextBody toBodyPart(Entity e, String content) {
		String encoding = "utf-8";
		Header h = e.getHeader();
		TextBody textBody = null;
		textBody = bodyFactory.textBody(content, StandardCharsets.UTF_8);
		h.setField(Fields.contentType(e.getMimeType() + "; charset=" + encoding.toLowerCase()));
		return textBody;
	}

	private static void replaceBody(Entity e, Body body) {
		Body removedBody = e.removeBody();
		if (removedBody != null) {
			removedBody.dispose();
		}
		e.setBody(body);
	}

	public void addToTextPart(Entity e, Map<String, String> configuration) {
		String disclaimer = CRLF + CRLF + variables.replace(configuration.get("plain"));
		String content = getBodyContent(e);

		e.getHeader().setField(Fields.contentTransferEncoding("quoted-printable"));
		if (Boolean.TRUE.equals(Boolean.valueOf(configuration.get("usePlaceholder")))) {
			int index = content.indexOf(PLACEHOLDER);
			if (index >= 0 && content.lastIndexOf(PLACEHOLDER) == index) {
				content = content.replaceFirst(PLACEHOLDER, disclaimer);
			} else {
				content = content.replace(PLACEHOLDER, "");
				content += disclaimer;
			}
		} else {
			content += disclaimer;
		}

		TextBody tb = toBodyPart(e, content);

		replaceBody(e, tb);
	}

	public void addToHtmlPart(Entity e, Map<String, String> configuration) {
		e.getHeader().setField(Fields.contentTransferEncoding("quoted-printable"));
		String html = variables.replace(configuration.get("html"), VariableDecorators.newLineToBr());
		Document disclaimerContent = Jsoup.parse(html);
		Elements images = disclaimerContent.getElementsByTag("img");

		boolean isMultipart = e.getParent() != null && e.getParent().getMimeType().startsWith("multipart");
		Body body = null;
		if (images.isEmpty() || !isMultipart) {
			body = updateBodyWithDisclaimer(e, disclaimerContent, configuration);
		} else if (e.getParent() != null && "multipart/related".equals(e.getParent().getMimeType())) {
			body = updateRelatedBodyWithDisclaimer(e, disclaimerContent, images, configuration);
		} else {
			body = createRelatedBodyWithDisclaimer(e, disclaimerContent, images, configuration);
		}
		replaceBody(e, body);
	}

	private Body updateBodyWithDisclaimer(Entity e, Document disclaimerContent, Map<String, String> configuration) {
		Document bodyContent = extractBody(e);
		addDisclaimerToContent(disclaimerContent, bodyContent, configuration);
		return toBodyPart(e, bodyContent.html());
	}

	private Body updateRelatedBodyWithDisclaimer(Entity e, Document disclaimerContent, Elements images,
			Map<String, String> configuration) {
		Multipart parent = (Multipart) e.getParent().getBody();
		Document bodyContent = extractBody(e);
		if (Boolean.TRUE.equals(Boolean.valueOf(configuration.get("removePrevious")))) {
			removeDisclaimerImageFromMessage(parent, bodyContent);
		}
		addDisclaimerImagesToMessage(images, parent);
		addDisclaimerToContent(disclaimerContent, bodyContent, configuration);
		return toBodyPart(e, bodyContent.html());
	}

	private Body createRelatedBodyWithDisclaimer(Entity e, Document disclaimerContent, Elements images,
			Map<String, String> configuration) {
		Multipart parent = new MultipartImpl("related");

		addDisclaimerImagesToMessage(images, parent);

		Body body = updateBodyWithDisclaimer(e, disclaimerContent, configuration);
		BodyPart bp = new BodyPart();
		bp.setBody(body);
		bp.setHeader(e.getHeader());
		bp.getHeader().setField(Fields.contentType(e.getMimeType() + "; charset=utf-8"));
		parent.addBodyPart(bp, 0);
		BodyPart parentBodyPart = new BodyPart();
		parentBodyPart.setMultipart(parent);
		e.setHeader(parentBodyPart.getHeader());

		body = parentBodyPart.getBody();
		return body;
	}

	private void addDisclaimerToContent(Document disclaimerContent, Document bodyContent,
			Map<String, String> configuration) {
		String hash = variables.uid();
		if (Boolean.TRUE.equals(Boolean.valueOf(configuration.get("removePrevious")))) {
			bodyContent.body().getElementsByClass(hash).remove();
		}
		String disclaimer = "<div class='" + hash + "'>" + disclaimerContent.html() + "</div>";
		if (Boolean.TRUE.equals(Boolean.valueOf(configuration.get("usePlaceholder")))) {
			Elements placeholderContainers = bodyContent.body().getElementsContainingOwnText(PLACEHOLDER);
			if (placeholderContainers.size() == 1) {
				String html = placeholderContainers.html().replaceFirst(PLACEHOLDER, disclaimer);
				html = html.replace(PLACEHOLDER, "");
				placeholderContainers.html(html);
				return;
			} else if (placeholderContainers.size() > 1) {
				placeholderContainers.forEach(container -> container.html(container.html().replace(PLACEHOLDER, "")));
			}
		}
		bodyContent.body().append(disclaimer);
	}

	private Document extractBody(Entity e) {
		String content = getBodyContent(e);
		Document htmlContent = Jsoup.parse(content);
		htmlContent.outputSettings().charset("utf-8");
		return htmlContent;
	}

	private void removeDisclaimerImageFromMessage(Multipart parent, Document content) {
		Set<String> cids = content.body().getElementsByClass(variables.uid()).stream()
				.flatMap(e -> e.getElementsByTag("img").stream()).map(img -> img.attr("src"))
				.filter(src -> src.startsWith("cid:")).map(src -> "<" + src.substring("cid:".length()) + ">")
				.collect(Collectors.toSet());

		parent.setBodyParts(parent.getBodyParts().stream().filter(part -> {
			Field cid = part.getHeader().getField("Content-ID");
			return cid == null || !cids.contains(cid.getBody());
		}).collect(Collectors.toList()));

	}

	private void addDisclaimerImagesToMessage(Elements images, Multipart parent) {
		String hash = variables.uid();
		Iterator<Element> it = images.iterator();
		long i = System.currentTimeMillis();
		while (it.hasNext()) {
			Element img = it.next();
			String src = img.attr("src");

			if (src.startsWith("data:image/")) {
				String ext = "png";
				if (src.startsWith("data:image/jpg")) {
					ext = "jpg";
				}
				String mime = "image/" + ext;
				String filename = hash + "-" + i + "." + ext;
				String cid = filename + "@bm-disclaimer";
				img.attr("src", "cid:" + cid);
				parent.addBodyPart(toInlineImageBodyPart(bodyFactory, src, filename, cid, mime));
			}
			i++;
		}
	}

	public void addToMultiPart(Entity e, Map<String, String> configuration) {
		Multipart mp = (Multipart) e.getBody();
		List<AddressableEntity> parts = Mime4JHelper.expandParts(mp.getBodyParts());

		// Apple Mail sends more than one non-attachment text/html parts
		// ex: html email + inline pdf
		boolean htmlDone = false;

		// iOS sends more than one non-attachment text/plain parts
		// ex: text + inline image + text
		boolean textDone = false;

		for (AddressableEntity ae : parts) {
			String mime = ae.getMimeType();
			if (!textDone && Mime4JHelper.TEXT_PLAIN.equals(mime) && !Mime4JHelper.isAttachment(ae)) {
				addToTextPart(ae, configuration);
				textDone = true;
			} else if (!htmlDone && Mime4JHelper.TEXT_HTML.equals(mime) && !Mime4JHelper.isAttachment(ae)) {
				addToHtmlPart(ae, configuration);
				htmlDone = true;
			}
		}
	}

	private static String getBodyContent(Entity e) {
		try {
			String encoding = "UTF-8";
			Field field = e.getHeader().getField("Content-Type");
			if (null != field) {
				ContentTypeField ctField = (ContentTypeField) field;
				String cs = ctField.getCharset();
				if (null != cs) {
					encoding = cs;
				}
			}
			TextBody tb = (TextBody) e.getBody();
			String partContent = null;
			try (InputStream in = tb.getInputStream()) {
				partContent = CharStreams.toString(new InputStreamReader(in, encoding));
			}
			return partContent;
		} catch (IOException io) {
			throw new ServerFault(io);
		}
	}

	private static BodyPart toInlineImageBodyPart(BasicBodyFactory bodyFactory, String data, String filename,
			String cid, String mime) {
		BodyPart bpInlineImg = new BodyPart();
		String b64 = data.substring(data.indexOf(",") + 1);
		bpInlineImg.setBody(bodyFactory.binaryBody(java.util.Base64.getDecoder().decode(b64)));
		Header h = new HeaderImpl();
		h.addField(Fields.contentType(mime + "; charset=utf-8; name=\"" + filename + "\""));
		h.addField(Fields.contentTransferEncoding("base64"));
		h.addField(new RawField("Content-ID", "<" + cid + ">"));
		h.addField(Fields.contentDisposition("inline; filename=\"" + filename + "\""));
		h.addField(new RawField("Content-Description", filename));
		bpInlineImg.setHeader(h);
		return bpInlineImg;
	}

}
