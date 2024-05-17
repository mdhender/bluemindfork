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

import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.dto.sync.SyncStatus;
import net.bluemind.eas.http.tests.AbstractMailshareTest;
import net.bluemind.eas.http.tests.helpers.CoreEmailHelper;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.eas.http.tests.helpers.SyncRequest;
import net.bluemind.eas.http.tests.helpers.SyncRequest.SyncRequestBuilder;
import net.bluemind.eas.http.tests.validators.EmailValidator;
import net.bluemind.eas.http.tests.validators.SyncResponseValidator.ResponseSyncType;

public class DeleteEmailTests extends AbstractMailshareTest {

	@Test
	public void test_deleteEmail() throws Exception {
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
				.execute(emailValidation) //
				.sync(request.copy() //
						.withClientChangesDelete(serverId.get()) //
						.build()) //
				.startValidation() //
				.assertResponseServerId(serverId.get(), ResponseSyncType.DELETE) //
				.assertSyncKeyChanged() //
				.endValidation();
	}

	@Test
	public void test_deleteEmail_otherMailbox_write() throws Exception {
		CoreEmailHelper.addMail(login, password, "INBOX", "single_body.eml");

		AtomicReference<String> serverId = new AtomicReference<>();
		Runnable emailValidation = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.withRead("0") //
				.build().validate(domainUser2SecurityContext);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		String subColId = SyncHelper.forgeCollectionId(subUser2Write.internalId, inboxFolderUser1.internalId);

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login2, password2) //
				.withCollectionId(subColId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.getValue("ServerId", serverId) //
				.endValidation() //
				.execute(emailValidation) //
				.sync(request.copy() //
						.withClientChangesDelete(serverId.get()) //
						.build()) //
				.startValidation() //
				.assertResponseServerId(serverId.get(), ResponseSyncType.DELETE) //
				.assertSyncKeyChanged() //
				.endValidation();

	}

	@Test
	public void test_deleteEmail_otherMailbox_read() throws Exception {
		CoreEmailHelper.addMail(login, password, "INBOX", "single_body.eml");

		AtomicReference<String> serverId = new AtomicReference<>();
		Runnable emailValidation = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.withRead("0") //
				.build().validate(domainUser3SecurityContext);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		String subColId = SyncHelper.forgeCollectionId(subUser3Read.internalId, inboxFolderUser1.internalId);

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login3, password3) //
				.withCollectionId(subColId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.getValue("ServerId", serverId) //
				.endValidation() //
				.execute(emailValidation) //
				.sync(request.copy() //
						.withClientChangesDelete(serverId.get()) //
						.build()) //
				.startValidation() //
				.assertSyncStatus(subColId, SyncStatus.SERVER_ERROR) //
				.endValidation() //
				.execute(emailValidation);

	}

}
