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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.eas.dto.sync.CollectionId;

public class AttachmentHelper {

	private static final Logger logger = LoggerFactory.getLogger(AttachmentHelper.class);

	public final static String TYPE = "TYPE";
	public final static String ATTACH = "ATTACH";
	public final static String BM_FILEHOSTING = "BM-FILEHOSTING";
	public final static String URL = "url";
	public final static String COLLECTION_ID = "collectionId";
	public final static String MESSAGE_ID = "messageId";
	public final static String MIME_PART_ADDRESS = "mimePartAddress";
	public final static String CONTENT_TYPE = "contentType";
	public final static String CONTENT_TRANSFER_ENCODING = "contentTransferEncoding";

	public static String getAttachmentId(String url, String contentType) {
		String ret = String.format("%s_%s_%s", BM_FILEHOSTING, toB64(url), toB64(contentType));

		try {
			ret = URLEncoder.encode(ret, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	public static String getAttachmentId(CollectionId collectionId, long mailboxItemId, String mimePartAddress,
			String contentType, String contentTransferEncoding) {

		String ret = collectionId.getValue() + "_" + mailboxItemId + "_" + mimePartAddress + "_" + toB64(contentType);
		if (contentTransferEncoding != null && !contentTransferEncoding.isEmpty()) {
			String cte = toB64(contentTransferEncoding);
			ret += "_" + cte;
		}
		try {
			ret = URLEncoder.encode(ret, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		return ret;
	}

	public static Map<String, String> parseAttachmentId(String attId) {
		String attachmentId = attId;
		try {
			attachmentId = URLDecoder.decode(attachmentId, "utf-8");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}

		Map<String, String> data = new HashMap<String, String>();

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
