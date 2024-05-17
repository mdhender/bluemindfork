/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.eas.http.tests.operations;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import net.bluemind.eas.client.MoveStatus;
import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.http.tests.AbstractMailshareTest;
import net.bluemind.eas.http.tests.helpers.CoreEmailHelper;
import net.bluemind.eas.http.tests.helpers.MoveHelper;
import net.bluemind.eas.http.tests.helpers.MoveItemRequest;
import net.bluemind.eas.http.tests.helpers.MoveItemRequest.MoveItemRequestBuilder;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.eas.http.tests.helpers.SyncRequest;
import net.bluemind.eas.http.tests.helpers.SyncRequest.SyncRequestBuilder;
import net.bluemind.eas.http.tests.validators.EmailValidator;

public class MoveEmailTests extends AbstractMailshareTest {

	@Test
	public void test_moveEmailFromInboxToTrash() throws Exception {
		CoreEmailHelper.addMail(login, password, "INBOX", "single_body.eml");

		AtomicReference<String> serverId = new AtomicReference<>();
		Runnable emailValidation = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.build().validate();

		SyncRequest request = new SyncRequestBuilder().withChanges().build();

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login, password) //
				.withCollectionId(inboxFolderUser1.internalId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.getValue("ServerId", serverId) //
				.endValidation() //
				.execute(emailValidation);

		long trashFolderId = CoreEmailHelper.getUserMailFolderId("user", domain.uid, "Trash",
				domainUserSecurityContext);
		MoveItemRequest moveRequest = new MoveItemRequestBuilder()
				.withClientChangesMove(serverId.get(), String.valueOf(trashFolderId)).build();

		new MoveHelper.MoveHelperBuilder()//
				.withAuth(login, password) //
				.withSrcMsgId(serverId.get()) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(moveRequest) //
				.startValidation() //
				.assertResponseStatus(serverId.get(), MoveStatus.SUCCESS) //
				.assertResponseDstMsgFolder(String.valueOf(trashFolderId), MoveStatus.SUCCESS) //
				.endValidation();

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login, password) //
				.withCollectionId(trashFolderId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.getValue("ServerId", serverId) //
				.endValidation() //
				.execute(emailValidation);
	}

	@Test
	public void test_moveEmailFromInboxToTrash_otherMailbox_write() throws Exception {
		CoreEmailHelper.addMail(login, password, "INBOX", "single_body.eml");

		AtomicReference<String> serverId = new AtomicReference<>();
		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		String subColId = SyncHelper.forgeCollectionId(subUser2Write.internalId, inboxFolderUser1.internalId);

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login2, password2) //
				.withCollectionId(subColId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.getValue("ServerId", serverId) //
				.endValidation();

		long trashFolderId = CoreEmailHelper.getUserMailFolderId("user", domain.uid, "Trash",
				domainUserSecurityContext);
		String subTrashId = SyncHelper.forgeCollectionId(subUser2Write.internalId, trashFolderId);
		MoveItemRequest moveRequest = new MoveItemRequestBuilder().withClientChangesMove(serverId.get(), subTrashId)
				.build();

		new MoveHelper.MoveHelperBuilder()//
				.withAuth(login2, password2) //
				.withSrcMsgId(serverId.get()) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(moveRequest) //
				.startValidation() //
				.assertResponseStatus(serverId.get(), MoveStatus.SUCCESS) //
				.assertResponseDstMsgFolder(subTrashId, MoveStatus.SUCCESS) //
				.endValidation();
	}

	@Test
	public void test_moveEmailFromInboxToTrash_otherMailbox_read() throws Exception {
		CoreEmailHelper.addMail(login, password, "INBOX", "single_body.eml");

		AtomicReference<String> serverId = new AtomicReference<>();
		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		String subColId = SyncHelper.forgeCollectionId(subUser3Read.internalId, inboxFolderUser1.internalId);

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login3, password3) //
				.withCollectionId(subColId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.getValue("ServerId", serverId) //
				.endValidation();

		long trashFolderId = CoreEmailHelper.getUserMailFolderId("user", domain.uid, "Trash",
				domainUserSecurityContext);
		String subTrashId = SyncHelper.forgeCollectionId(subUser3Read.internalId, trashFolderId);
		MoveItemRequest moveRequest = new MoveItemRequestBuilder().withClientChangesMove(serverId.get(), subTrashId)
				.build();

		new MoveHelper.MoveHelperBuilder()//
				.withAuth(login3, password3) //
				.withSrcMsgId(serverId.get()) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(moveRequest) //
				.startValidation() //
				.assertResponseStatus(serverId.get(), MoveStatus.INVALID_SOURCE_COLLECTION_ID) //
				.endValidation();

	}

}
