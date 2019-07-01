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

import java.io.InputStream;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BodyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.mime4j.common.DefaultEntityBuilder;
import net.bluemind.mime4j.common.IMailRewriter;
import net.bluemind.utils.FBOSInput;

/**
 * This handler will leave the parsed message untouched.
 * 
 * @author tom
 * 
 */
public class DontTouchHandler extends DefaultEntityBuilder implements IMailRewriter {

	private static final Logger logger = LoggerFactory.getLogger(DontTouchHandler.class);

	private Message message;

	private boolean rewritten;

	private Mailbox from;

	public DontTouchHandler(Message entity, BodyFactory bodyFactory, Mailbox from) {
		super(entity, bodyFactory);
		this.message = entity;
		this.from = from;
	}

	@Override
	public InputStream renderAsMimeStream() throws Exception {
		rewrite();
		MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
		FileBackedOutputStream fbos = new FileBackedOutputStream(32678, "dont-touch-handler-render");
		writer.writeMessage(message, new FilterCRLFOutputStream(fbos));
		fbos.close();
		return FBOSInput.from(fbos);
	}

	@Override
	public final void rewrite() {
		if (!rewritten) {
			rewritten = true;
			this.message = firstRewrite(message);
			if (from != null) {
				this.message.setFrom(from);
			}
		}
	}

	protected Message firstRewrite(Message parsed) {
		logger.debug("default rewrite implementation that does nothing.");
		return parsed;
	}

	@Override
	public <T> T renderAs(Class<T> klass) throws Exception {

		if (Message.class.equals(klass)) {
			rewrite();
			return klass.cast(message);
		}

		logger.warn("I can only render as " + Message.class + ". " + klass.getCanonicalName() + " is not supported.");

		return null;
	}

}
