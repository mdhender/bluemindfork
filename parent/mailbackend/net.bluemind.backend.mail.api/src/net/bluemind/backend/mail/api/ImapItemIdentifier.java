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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemIdentifier;

@BMApi(version = "3")
public class ImapItemIdentifier extends ItemIdentifier {

	public long imapUid;

	public ImapItemIdentifier() {
	}

	public ImapItemIdentifier(long imapUid, long id, long version) {
		super(null, id, version);
		this.imapUid = imapUid;
	}

	public static ImapItemIdentifier of(long imapUid, long id, long version) {
		return new ImapItemIdentifier(imapUid, id, version);
	}
}
