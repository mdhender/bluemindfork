/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.parsing;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;

public class VoidTaggedResponseProcessor extends TaggedResponseProcessor<Void> {

	public VoidTaggedResponseProcessor() {
		super(new ResponsePayloadBuilder<Void>() {

			private Status status;

			@Override
			public boolean untagged(Buffer b) {
				return false;
			}

			@Override
			public boolean tagged(String tag, Status st, String msg) {
				this.status = st;
				return true;
			}

			@Override
			public ImapResponseStatus<Void> build() {
				return new ImapResponseStatus<>(status, null);
			}
		});
	}

}
