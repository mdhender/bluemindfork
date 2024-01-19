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

import java.util.Date;
import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.api.flags.FlagUpdate;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.ICrudByIdSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;

/**
 * Container of {@link MailboxItem}.
 * 
 * The container is created by the {@link IMailboxFolders} service when a new
 * replicated folder is created.
 */
@BMApi(version = "3", genericType = MailboxItem.class)
@Path("/mail_items/{replicatedMailboxUid}")
public interface IMailboxItems
		extends IChangelogSupport, ICrudByIdSupport<MailboxItem>, ICountingSupport, ISortingSupport {

	/**
	 * Upload an email part (eg. attachment, html body). The returned address can be
	 * used as {@link Part#address} when creating or updating a {@link MailboxItem}.
	 * 
	 * The uploaded parts need to be cleaned-up explicitly with
	 * {@link IMailboxItems#removePart(String)}
	 * 
	 * @param part a re-usable email part.
	 * @return an address usable as {@link Part#address}
	 */
	@PUT
	@Path("_part")
	String uploadPart(Stream part);

	/**
	 * Get the list of unread items, applying the per-user overlay when dealing with
	 * a shared folder.
	 * 
	 * @return the list of {@link ItemValue#internalId}
	 */
	@GET
	@Path("_unread")
	List<Long> unreadItems();

	/**
	 * Get the list of {@link ItemValue#internalId} for {@link MailboxItem}
	 * delivered or updated after or at the given date.
	 * 
	 * @param deliveredOrUpdatedAfter
	 * @return
	 */
	@GET
	@Path("_recent")
	List<Long> recentItems(Date deliveredOrUpdatedAfter);

	/**
	 * Remove a part uploaded through {@link IMailboxItems#uploadPart(Stream)}
	 * 
	 * @param partId an address returned by a previous <code>uploadPart</code> call
	 */
	@DELETE
	@Path("{partId}/_part")
	void removePart(@PathParam("partId") String partId);

	@POST
	@Path("id/{id}")
	ImapAck updateById(@PathParam("id") long id, MailboxItem value);

	@PUT
	@Path("id/{id}")
	ImapAck createById(@PathParam("id") long id, MailboxItem value);

	@PUT
	ImapItemIdentifier create(MailboxItem value);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	/**
	 * Fetch a single part from an email mime tree. The address, encoding & charset
	 * are specified in the {@link Part} objects from {@link MessageBody#structure}.
	 * 
	 * @param imapUid
	 * @param address
	 * @param encoding set null to fetch pristine part
	 * @param mime     override the mime type of the response
	 * @param charset  override the charset of the response
	 * @param filename set a part name (useful for download purpose)
	 * @return a stream of the (optionally) decoded part
	 */
	@GET
	@Path("part/{imapUid}/{address}")
	@Produces("application/octet-stream")
	Stream fetch(@PathParam("imapUid") long imapUid, @PathParam("address") String address,
			@QueryParam("encoding") String encoding, @QueryParam("mime") String mime,
			@QueryParam("charset") String charset, @QueryParam("filename") String filename);

	/**
	 * Re-injects a deleted item into the current folder
	 * 
	 * @param itemId the item id of a deleted or deleted+expunged message
	 * @return
	 */
	@POST
	@Path("_unexpunge/{itemId}")
	ItemIdentifier unexpunge(@PathParam("itemId") long itemId);

	/**
	 * Re-injects multiple items into the current folder
	 * 
	 * @param itemIds the item ids of deleted or deleted+expunged messages
	 * @return
	 */
	@POST
	@Path("_multipleUnexpunge")
	List<ItemIdentifier> multipleUnexpungeById(List<Long> itemIds);


	/**
	 * Mark deleted items as ready for removal. Physical will removal will occur
	 * later (cyr_expire & co)
	 */
	@POST
	@Path("_expunge")
	void expunge();

	/**
	 * @param imapUid
	 * @return
	 */
	@GET
	@Path("eml/{imapUid}")
	@Produces("message/rfc822")
	Stream fetchComplete(@PathParam("imapUid") long imapUid);

	/**
	 * Add one flag to multiple {@link MailboxItem}.
	 * 
	 * @param flagUpdate
	 * @return the new container version
	 */
	@PUT
	@Path("_addFlag")
	Ack addFlag(FlagUpdate flagUpdate);

	/**
	 * Delete one flag to multiple {@link MailboxItem}.
	 * 
	 * @param flagUpdate
	 * @return the new container version
	 */
	@POST
	@Path("_deleteFlag")
	Ack deleteFlag(FlagUpdate flagUpdate);

	/**
	 * Decompose EML in temporary parts, useful to update drafts
	 * 
	 * @param message id
	 * @return message structure with temporary addresses
	 */
	@GET
	@Path("{id}/getForUpdate")
	ItemValue<MailboxItem> getForUpdate(@PathParam("id") long id);

}
