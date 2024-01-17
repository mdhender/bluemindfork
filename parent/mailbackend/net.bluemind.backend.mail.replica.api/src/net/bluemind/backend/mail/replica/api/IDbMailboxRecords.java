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

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICountingSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IReadByIdSupport;
import net.bluemind.core.container.api.IRestoreItemCrudSupport;
import net.bluemind.core.container.api.ISortingSupport;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;

/**
 * Database only version of {@link IMailboxItems} for sync server usage.
 * 
 */
@BMApi(version = "3", internal = true, genericType = MailboxRecord.class)
@Path("/db_mailbox_records/{replicatedMailboxUid}")
public interface IDbMailboxRecords extends IChangelogSupport, IDataShardSupport, ICountingSupport, ISortingSupport,
		IReadByIdSupport<MailboxRecord>, IRestoreItemCrudSupport<MailboxRecord> {

	@GET
	@Path("{uid}/complete")
	ItemValue<MailboxRecord> getComplete(@PathParam("uid") String uid);

	@GET
	@Path("{imapUid}/completeByImapUid")
	ItemValue<MailboxRecord> getCompleteByImapUid(@PathParam("imapUid") long imapUid);

	@POST
	@Path("_imapBindings")
	List<ImapBinding> imapBindings(List<Long> ids);

	@GET
	@Path("_weight")
	Weight weight();

	@GET
	@Path("_labels")
	List<String> labels();

	@GET
	@Path("_all")
	List<ItemValue<MailboxRecord>> all();

	@POST
	@Path("_imapUidSet")
	List<RawImapBinding> imapIdSet(String set, @QueryParam("filter") String filter);

	/**
	 * Quick alternative to {@link IDbMailboxRecords#multipleGetById(List)}, this
	 * one will not load ItemValue stuff. Body is not loaded.
	 * 
	 * @param ids
	 * @return
	 */
	@POST
	@Path("_slice")
	List<WithId<MailboxRecord>> slice(List<Long> ids);

	@PUT
	@Path("id/{id}")
	Ack createById(long id, MailboxRecord mail);

	@PUT
	@Path("{uid}")
	ItemVersion create(@PathParam("uid") String uid, MailboxRecord mail);

	@PUT
	@Path("_mCreate")
	List<ItemIdentifier> multiCreate(List<MailboxRecord> mails);

	@POST
	@Path("{uid}")
	void update(@PathParam("uid") String uid, MailboxRecord mail);

	@POST
	@Path("id/{id}")
	Ack updateById(@PathParam("id") long id, MailboxRecord value);

	@DELETE
	@Path("{uid}")
	void delete(@PathParam("uid") String uid);

	@DELETE
	@Path("id/{id}")
	void deleteById(@PathParam("id") long id);

	@POST
	@Path("_updates")
	Ack updates(List<MailboxRecord> records);

	@DELETE
	@Path("_deleteImapUids")
	void deleteImapUids(List<Long> uids);

	@DELETE
	@Path("_deleteAll")
	void deleteAll();

	@DELETE
	@Path("_prepareContainerDelete")
	void prepareContainerDelete();

	@GET
	@Path("eml/{imapUid}")
	@Produces("message/rfc822")
	Stream fetchComplete(@PathParam("imapUid") long imapUid);

	/**
	 * @return the list of {@link MailboxRecord} for which the corresponding
	 *         {@link MessageBody} has a {@link MessageBody#bodyVersion} lower than
	 *         <code>version</code>
	 */
	@GET
	@Path("body/version/lowerthan/{version}")
	List<ImapBinding> havingBodyVersionLowerThan(@PathParam("version") int version);

}
