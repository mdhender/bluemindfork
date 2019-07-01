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
package net.bluemind.mime4j.common;

import java.io.InputStream;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.MessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.mime4j.common.rewriters.impl.ForwardAsAttachmentHandler;
import net.bluemind.mime4j.common.rewriters.impl.ReplyHandler;

public class RewriterBuilder {

	private static final Logger logger = LoggerFactory.getLogger(RewriterBuilder.class);

	private InputStream includedContent;
	private boolean keepAttachments;
	private RewriteMode mode;
	private Mailbox defaultFrom;

	public RewriterBuilder() {
		this.keepAttachments = false;
		this.mode = RewriteMode.REPLY;
	}

	public IMailRewriter build() {
		IMailRewriter ret = null;
		BasicBodyFactory bf = new BasicBodyFactory();
		MessageImpl m = new MessageImpl();
		switch (mode) {
		case FORWARD_AS_ATTACHMENT:
			ret = new ForwardAsAttachmentHandler(m, bf, defaultFrom, includedContent);
			break;
		case FORWARD_INLINE:
			ret = new ReplyHandler(m, bf, defaultFrom, includedContent, keepAttachments);
			break;
		case REPLY:
			ret = new ReplyHandler(m, bf, defaultFrom, includedContent, keepAttachments);
			break;
		}
		return ret;
	}

	public RewriterBuilder setIncludedContent(InputStream includedContent) {
		this.includedContent = includedContent;
		return this;
	}

	public RewriterBuilder setKeepAttachments(boolean keepAttachments) {
		this.keepAttachments = keepAttachments;
		logger.debug("keepAttachents: {}", this.keepAttachments);
		return this;
	}

	public RewriterBuilder setMode(RewriteMode mode) {
		this.mode = mode;
		return this;
	}

	public RewriterBuilder setFrom(Mailbox userEmail) {
		this.defaultFrom = userEmail;
		return this;
	}

}
