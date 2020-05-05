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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.backend.mail.api.events.MailEventAddresses;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;

/**
 * API to access the hierarchy of a user or shared mailbox.
 * 
 * <p>
 * 
 * <code>partition</code>: vagrant_vmw (domain name with dots replaced by '_')
 * 
 * <p>
 * 
 * <code>mailbox_root</code>: user.log^in (for a user with the login log.in) or
 * shar^ed (for a mailshare with the name shar.ed).
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
@Path("/mail_folders/{partition}/{mailboxRoot}")
public interface IMailboxFolders extends IBaseMailboxFolders {

	@GET
	@Path("{id}/completeById")
	ItemValue<MailboxFolder> getCompleteById(@PathParam("id") long id);

	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, MailboxFolder value);

	/**
	 * @param hierId
	 *                   the numerical id we want to end up with in
	 *                   {@link IContainersFlatHierarchy}
	 * @param value
	 *                   the folder to create
	 * @return the identifier allocated in the subtree container
	 */
	@PUT
	@Path("id/{hierarchyId}")
	ItemIdentifier createForHierarchy(@PathParam("hierarchyId") long hierId, MailboxFolder value);

	/**
	 * @param value
	 *                  the folder to create
	 * @return the identifier allocated in the subtree container
	 */
	@PUT
	ItemIdentifier createBasic(MailboxFolder value);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	/**
	 * Also delete all known child folders
	 * 
	 * @param id
	 */
	@DELETE
	@Path("deep/{id}")
	void deepDelete(@PathParam("id") long id);

	/**
	 * Empty an email folder including child folders
	 * 
	 * @param id
	 */
	@DELETE
	@Path("empty/{id}")
	void emptyFolder(@PathParam("id") long id);
	
	/**
	 * Mark folder as read (does not include sub-folders).
	 * 
	 * @param id the folder identifier
	 */
	@PUT
	@Path("markAsRead/{id}")
	void markFolderAsRead(@PathParam("id") long id);

	/**
	 * Import MailboxItems from a source folder describe
	 * {@link ImportMailboxItemSet#mailboxFolderId}
	 * 
	 * Source and destination folders must be in the same subtree
	 * 
	 * @param folderDestinationId
	 * @param mailboxItems
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	@Path("importItems/{folderDestinationId}")
	ImportMailboxItemsStatus importItems(@PathParam("folderDestinationId") long folderDestinationId,
			ImportMailboxItemSet mailboxItems) throws ServerFault;
}
