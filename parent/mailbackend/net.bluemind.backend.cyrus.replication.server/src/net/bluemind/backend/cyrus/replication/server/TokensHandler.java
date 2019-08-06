/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server;

import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.parsetools.RecordParser;

import net.bluemind.backend.cyrus.replication.server.Token.LiteralFollowUp;

public class TokensHandler {

	private static final Logger logger = LoggerFactory.getLogger(TokensHandler.class);

	private RecordParser parser;
	private boolean delimitedMode;
	private Queue<ReplicationFrame> frames;
	long frameId = 0;

	private ReplicationFrameBuilder frameBuilder;
	private static final byte[] DELIM = "\r\n".getBytes();

	public TokensHandler() {
		frameBuilder = new ReplicationFrameBuilder(frameId++);
		frames = new LinkedList<>();
	}

	public boolean delimited() {
		return delimitedMode;
	}

	public void feed(Token token) {
		frameBuilder.add(token);
		if (delimitedMode) {
			LiteralFollowUp followUp = token.followup();
			if (followUp != null) {
				parser.fixedSizeMode(followUp.size());
				delimitedMode = false;
				if (logger.isDebugEnabled()) {
					logger.debug("Before binary with {}bytes", followUp.size());
				}
			} else {
				// last delimited, create a frame
				if (frameBuilder.complete()) {
					ReplicationFrame frame = frameBuilder.build();
					frames.add(frame);
					frameBuilder = new ReplicationFrameBuilder(frameId++);
				} else {
					logger.warn("Frame is not complete, currentContent: {}", frameBuilder);
				}

			}
		} else {
			parser.delimitedMode(DELIM);
			delimitedMode = true;
		}

	}

	public ReplicationFrame next() {
		return frames.poll();
	}

	public void parser(RecordParser lineParser) {
		this.parser = lineParser;
		this.delimitedMode = true;
	}

}
