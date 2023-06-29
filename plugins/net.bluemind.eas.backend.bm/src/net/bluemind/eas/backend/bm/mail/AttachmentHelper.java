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
package net.bluemind.eas.backend.bm.mail;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.eas.dto.sync.CollectionId;

public class AttachmentHelper {

	private static final Logger logger = LoggerFactory.getLogger(AttachmentHelper.class);

	public static final String TYPE = "TYPE";
	public static final String ATTACH = "ATTACH";
	public static final String BM_FILEHOSTING = "BM-FILEHOSTING";
	public static final String BM_FILEHOSTING_EVENT = "BM-FILEHOSTING-EVENT";
	public static final String URL = "url";
	public static final String COLLECTION_ID = "collectionId";
	public static final String MESSAGE_ID = "messageId";
	public static final String MIME_PART_ADDRESS = "mimePartAddress";
	public static final String CONTENT_TYPE = "contentType";
	public static final String CONTENT_TRANSFER_ENCODING = "contentTransferEncoding";

	private AttachmentHelper() {

	}

	public static String getEmailFileHostingAttachmentFileReference(String url, String contentType) {
		String ret = BM_FILEHOSTING + "_" + toB64(url) + "_" + toB64(contentType);
		return URLEncoder.encode(ret, StandardCharsets.UTF_8);
	}

	public static String getEmailAttachmentFileReference(CollectionId collectionId, long mailboxItemId,
			String mimePartAddress, String contentType, String contentTransferEncoding) {
		String ret = collectionId.getValue() + "_" + mailboxItemId + "_" + mimePartAddress + "_" + toB64(contentType);
		if (contentTransferEncoding != null && !contentTransferEncoding.isEmpty()) {
			String cte = toB64(contentTransferEncoding);
			ret += "_" + cte;
		}
		return URLEncoder.encode(ret, StandardCharsets.UTF_8);
	}

	public static String getEventFileHostingAttachmentFileReference(String url) {
		return URLEncoder.encode(BM_FILEHOSTING_EVENT + "_" + toB64(url), StandardCharsets.UTF_8);
	}

	public static Map<String, String> parseAttachmentId(String attId) {
		String attachmentId = URLDecoder.decode(attId, StandardCharsets.UTF_8);

		Map<String, String> data = new HashMap<>();

		if (attachmentId.startsWith(BM_FILEHOSTING_EVENT)) {
			String[] tab = attachmentId.split("_");
			data.put(TYPE, BM_FILEHOSTING_EVENT);
			data.put(URL, fromB64(tab[1]));
			data.put(CONTENT_TYPE, "application/octet-stream"); // fake
			return data;
		}

		if (attachmentId.startsWith(BM_FILEHOSTING)) {
			String[] tab = attachmentId.split("_");
			data.put(TYPE, BM_FILEHOSTING);
			data.put(URL, fromB64(tab[1]));
			data.put(CONTENT_TYPE, fromB64(tab[2]));
			return data;
		}

		String[] col = attachmentId.split("__");
		String sub = null;
		if (col.length == 2) {
			sub = col[0];
			attachmentId = col[1];
		}

		String[] tab = attachmentId.split("_");
		if (tab.length < 4) {
			logger.error("Invalid attachmentId {}", attachmentId);
			return null;
		}
		if (Strings.isNullOrEmpty(sub)) {
			data.put(COLLECTION_ID, tab[0]);
		} else {
			data.put(COLLECTION_ID, CollectionId.of(Long.parseLong(sub), tab[0]).getValue());
		}
		data.put(TYPE, ATTACH);
		data.put(MESSAGE_ID, tab[1]);
		data.put(MIME_PART_ADDRESS, tab[2]);
		data.put(CONTENT_TYPE, fromB64(tab[3]));
		if (tab.length >= 5) {
			data.put(CONTENT_TRANSFER_ENCODING, fromB64(tab[4]));
		}
		return data;
	}

	private static String toB64(String s) {
		return Base64.getEncoder().encodeToString(s.getBytes());
	}

	private static String fromB64(String s) {
		return new String(Base64.getDecoder().decode(s));
	}

}
