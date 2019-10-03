/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.calendar.helper.mail;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.icalendar.parser.Mime;
import net.bluemind.utils.Trust;

public class EventAttachmentHelper {

	private static final Logger logger = LoggerFactory.getLogger(EventAttachmentHelper.class);

	public static boolean hasBinaryAttachments(List<EventAttachment> attachments) {
		return !attachments.isEmpty() && attachments.get(0).isBinaryAttachment();
	}

	public static List<EventAttachment> getAttachments(VEvent event, long maxBytes) {
		long bytesRead = 0;
		List<EventAttachment> attachments = new ArrayList<>();
		try {
			List<EventAttachment> binaryParts = new ArrayList<>(event.attachments.size());
			for (AttachedFile att : event.attachments) {
				try {
					byte[] attachmentAsBytes = loadAttachment(att, bytesRead, maxBytes);
					bytesRead += attachmentAsBytes.length;
					BodyPart binaryPart = new CalendarMailHelper().createBinaryPart(attachmentAsBytes);
					binaryParts
							.add(new EventAttachment(att.publicUrl, att.name, Mime.getMimeType(att.name), binaryPart));
				} catch (IOException e) {
					logger.warn("Cannot read event attachment from url {}", att.publicUrl, e);
				}
			}
			attachments.addAll(binaryParts);
		} catch (FileSizeExceededException fee) {
			attachments.addAll(event.attachments.stream()
					.map(att -> new EventAttachment(att.publicUrl, att.name, Mime.getMimeType(att.name)))
					.collect(Collectors.toList()));
		}
		return attachments;
	}

	private static byte[] loadAttachment(AttachedFile attachment, long read, long maxBytes) throws IOException {
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[] { Trust.createTrustManager() }, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (BufferedInputStream in = new BufferedInputStream(new URL(attachment.publicUrl).openStream())) {
			byte dataBuffer[] = new byte[8192];
			int bytesRead = 0;
			while ((bytesRead = in.read(dataBuffer, 0, 8192)) != -1) {
				read += bytesRead;
				if (read > maxBytes) {
					throw new FileSizeExceededException();
				}
				baos.write(dataBuffer, 0, bytesRead);
			}
		}
		return baos.toByteArray();
	}

	@SuppressWarnings("serial")
	public static class FileSizeExceededException extends RuntimeException {
	}

}
