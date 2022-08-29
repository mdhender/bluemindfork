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
package net.bluemind.backend.mail.api;

import jakarta.ws.rs.Path;

import net.bluemind.backend.mail.api.events.MailEventAddresses;
import net.bluemind.core.api.BMApi;

/**
 * API to access the hierarchy of a user or shared mailbox.
 * 
 * <p>
 * 
 * <code>container</code>: subtree_bm.lan!user.tomUid (the uid of the subtree
 * container)
 * 
 * <p>
 * 
 * The uid of items returned by this API are sometime called the uniqueid of the
 * mailbox folders. You can pass those uid(s) to
 * {@link MailEventAddresses#mailboxContentChanged(String)} to obtain the vertx
 * eventbus address that will receive notifications when the content of a folder
 * changes.
 * 
 * This API is tied to a container of {@link MailboxFolder} with a changelog.
 *
 */
@BMApi(version = "3")
@Path("/mail_folders_container/{container}")
public interface IMailboxFoldersByContainer extends IMailboxFolders {

}
