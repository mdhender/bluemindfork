/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.api;

import jakarta.ws.rs.Path;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.core.api.BMApi;

/**
 * Database only version of {@link IMailboxItems} for sync server usage.
 * 
 */
@BMApi(version = "3", internal = true, genericType = MailboxRecord.class)
@Path("/db_mailbox_records_sync_index/{replicatedMailboxUid}")
public interface ISyncDbMailboxRecords extends IDbMailboxRecords {

}
