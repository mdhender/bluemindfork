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
package net.bluemind.eas.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.rtf.RTFEditorKit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.freeutils.tnef.CompressedRTFInputStream;

public class RTFUtils {

	private static final Logger logger = LoggerFactory.getLogger(RTFUtils.class);

	public static String getFolderId(String devId, String dataClass) {
		return devId + "\\" + dataClass;
	}

	public static String extractB64CompressedRTF(String b64) {
		String ret = "";
		try {
			byte[] bin = Base64.getDecoder().decode(b64);
			if (bin.length > 0) {
				ByteArrayInputStream in = new ByteArrayInputStream(bin);
				CompressedRTFInputStream cin = new CompressedRTFInputStream(in);

				String rtfDecompressed = FileUtils.streamString(cin, true);
				ret = extractRtfText(new ByteArrayInputStream(rtfDecompressed.getBytes()));
			}
		} catch (Exception e) {
			logger.error("error extracting compressed rtf", e);
		}
		return ret;
	}

	public static String extractCompressedRTF(InputStream in) {
		String ret = "";
		try {
			CompressedRTFInputStream cin = new CompressedRTFInputStream(in);
			String rtfDecompressed = FileUtils.streamString(cin, true);
			ret = extractRtfText(new ByteArrayInputStream(rtfDecompressed.getBytes()));
		} catch (Exception e) {
			logger.error("error extracting compressed rtf", e);
		}
		return ret;
	}

	private static String extractRtfText(InputStream stream) throws IOException, BadLocationException {
		RTFEditorKit kit = new RTFEditorKit();
		Document doc = kit.createDefaultDocument();
		kit.read(stream, doc, 0);

		return doc.getText(0, doc.getLength());
	}

}
