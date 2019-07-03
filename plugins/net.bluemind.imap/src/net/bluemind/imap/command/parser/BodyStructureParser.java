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
package net.bluemind.imap.command.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.mime.MimePart;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.imap.mime.impl.ParenMatcher;
import net.bluemind.imap.mime.impl.PartDescriptionParser;

public class BodyStructureParser {

	private static final Logger logger = LoggerFactory.getLogger(BodyStructureParser.class);

	private PartDescriptionParser partParser;

	public BodyStructureParser() {
		this.partParser = new PartDescriptionParser(this);
	}

	public MimeTree parse(byte[] bs) {
		MimeTree ret = new MimeTree();
		parse(ret, bs);
		if (logger.isDebugEnabled()) {
			logger.debug("mime tree:\n" + ret);
		}
		return ret;
	}

	private char charAt(byte[] bytes, int i) {
		return (char) bytes[i];
	}

	private byte[] substring(byte[] bytes, int start, int end) {
		byte[] ret = new byte[end - start];
		System.arraycopy(bytes, start, ret, 0, ret.length);
		return ret;
	}

	private byte[] substring(byte[] bytes, int start) {
		return substring(bytes, start, bytes.length);
	}

	public void parse(MimePart parent, byte[] bs) {
		if (bs == null || bs.length == 0 || charAt(bs, 0) != '(') {
			return;
		}
		// System.err.println("parse parent:\n"+parent+"\n"+bs+"\n---");

		if (charAt(bs, 1) != '(') {
			int endIdx = ParenMatcher.closingParenIndex(bs, 0) + 1;
			byte[] single = substring(bs, 0, endIdx);
			MimePart part = parseSinglePart(single);
			parent.addPart(part);
			if (endIdx < bs.length) {
				byte[] next = substring(bs, endIdx);
				// logger.info("next: " + next);
				parse(parent, next);
			}
		} else {
			int endIdx = ParenMatcher.closingParenIndex(bs, 1) + 1;
			byte[] sub = substring(bs, 1, endIdx);
			MimePart mp = new MimePart();
			parent.addPart(mp);
			parse(mp, sub);
			if (endIdx < bs.length) {
				byte[] next = substring(bs, endIdx);
				// logger.info("next: " + next);
				parse(mp, next);
			}
			int nextBlock = ParenMatcher.closingParenIndex(bs, 0) + 1;
			byte[] next = substring(bs, nextBlock);
			parse(parent, next);
		}
	}

	private MimePart parseSinglePart(byte[] substring) {
		MimePart singlePart = new MimePart();
		try {
			partParser.parse(singlePart, substring);
		} catch (RuntimeException t) {
			logger.error("Error parsing part: " + substring);
			throw t;
		}

		return singlePart;
	}

}
