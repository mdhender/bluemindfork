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
package net.bluemind.eas.serdes.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.calendar.CalendarResponseFormatter;
import net.bluemind.eas.serdes.contact.ContactResponseFormatter;
import net.bluemind.eas.serdes.email.EmailResponseFormatter;
import net.bluemind.eas.serdes.tasks.TasksResponseFormatter;

public final class AppDataFormatter {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(AppDataFormatter.class);
	private final IBodyOutput output;
	private static final InlineBodyOutput INLINE = new InlineBodyOutput();

	public AppDataFormatter() {
		this(INLINE);
	}

	public AppDataFormatter(IBodyOutput output) {
		this.output = output;
	}

	public void append(final IResponseBuilder b, final double protocolVersion, final AppData data,
			final Callback<IResponseBuilder> done) {

		if (data.metadata.attachment != null) {
			output.appendAttachment(b, protocolVersion, data, done);
		} else {
			Callback<IResponseBuilder> metadataWritten = new Callback<IResponseBuilder>() {

				@Override
				public void onResult(IResponseBuilder rb) {
					output.appendBody(rb, protocolVersion, data, done);
				}
			};

			if (data.metadata.email != null) {
				EmailResponseFormatter emf = new EmailResponseFormatter();
				emf.append(b, protocolVersion, data.metadata.email, metadataWritten);
			} else if (data.metadata.event != null) {
				CalendarResponseFormatter cf = new CalendarResponseFormatter();
				cf.append(b, protocolVersion, data.metadata.event, metadataWritten);
			} else if (data.metadata.contact != null) {
				ContactResponseFormatter cf = new ContactResponseFormatter();
				cf.append(b, protocolVersion, data.metadata.contact, metadataWritten);
			} else if (data.metadata.task != null) {
				TasksResponseFormatter tf = new TasksResponseFormatter();
				tf.append(b, protocolVersion, data.metadata.task, metadataWritten);
			}

		}

	}

}
