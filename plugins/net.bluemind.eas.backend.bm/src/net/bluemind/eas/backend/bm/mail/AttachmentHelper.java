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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttachmentHelper {

	private static final Logger logger = LoggerFactory.getLogger(AttachmentHelper.class);

	public final static String COLLECTION_ID = "collectionId";
	public final static String MESSAGE_ID = "messageId";
	public final static String MIME_PART_ADDRESS = "mimePartAddress";
	public final static String CONTENT_TYPE = "contentType";
	public final static String CONTENT_TRANSFER_ENCODING = "contentTransferEncoding";

	public static String getAttachmentId(int collectionId, long mailboxItemId, String mimePartAddress,
			String contentType, String contentTransferEncoding) {
		String ret = collectionId + "_" + mailboxItemId + "_" + mimePartAddress + "_" + toB64(contentType);
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
		String[] tab = attachmentId.split("_");
		if (tab.length < 4) {
			logger.error("Invalid attachmentId {}", attachmentId);
			return null;
		}
		Map<String, String> data = new HashMap<String, String>();
		data.put(COLLECTION_ID, tab[0]);
		data.put(MESSAGE_ID, tab[1]);
		data.put(MIME_PART_ADDRESS, tab[2]);
		data.put(CONTENT_TYPE, fromB64(tab[3]));
		if (tab.length >= 5) {
			data.put(CONTENT_TRANSFER_ENCODING, fromB64(tab[4]));
		}
		return data;
	}

	private static String toB64(String s) {
		ChannelBuffer src = ChannelBuffers.wrappedBuffer(s.getBytes());
		ChannelBuffer result = org.jboss.netty.handler.codec.base64.Base64.encode(src, false);
		return result.toString(Charset.defaultCharset());
	}

	private static String fromB64(String s) {
		ChannelBuffer src = ChannelBuffers.wrappedBuffer(s.getBytes());
		ChannelBuffer result = org.jboss.netty.handler.codec.base64.Base64.decode(src);
		return result.toString(Charset.defaultCharset());
	}

}
