/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.synchronization;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.dto.sync.SyncStatus;
import net.bluemind.eas.http.tests.AbstractEasTest;
import net.bluemind.eas.http.tests.builders.EmailBuilder;
import net.bluemind.eas.http.tests.helpers.CoreEmailHelper;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.eas.http.tests.helpers.SyncRequest;
import net.bluemind.eas.http.tests.helpers.SyncRequest.SyncRequestBuilder;
import net.bluemind.eas.http.tests.validators.EmailValidator;
import net.bluemind.eas.http.tests.validators.SyncResponseValidator.ResponseSyncType;

public class EmailSynchronizationTests extends AbstractEasTest {

	@Test
	public void testMailSyncInboxClientChangesModifyShouldReturnStatus1OnNonExistingItemsAndIgnoringChanges()
			throws Exception {
		long inbox = CoreEmailHelper.getUserMailFolderAsRoot("user", domain.uid, "INBOX");

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login, password) //
				.withCollectionId(inbox) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request.copy() //
						.withClientChangesModify(inbox + ":12345", EmailBuilder.getSimpleMail(ProtocolVersion.V161) //
						).build()) //
				.startValidation() //
				.assertResponseStatus(inbox, 12345, SyncStatus.OK, ResponseSyncType.CHANGE) //
				.endValidation();
	}

	@Test
	public void testMailSyncDraftClientChangesModifyShouldReturnStatus8OnNonExistingItem() throws Exception {
		long drafts = CoreEmailHelper.getUserMailFolderAsRoot("user", domain.uid, "Drafts");

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login, password) //
				.withCollectionId(drafts) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request.copy() //
						.withClientChangesModify(drafts + ":12346", EmailBuilder.getSimpleMail(ProtocolVersion.V161) //
						).build()) //
				.startValidation() //
				.assertResponseStatus(drafts, 12346, SyncStatus.OBJECT_NOT_FOUND, ResponseSyncType.CHANGE) //
				.endValidation();
	}

	@Test
	public void testMailSyncDraftClientChangesOnlyHTMLShouldMergeChanges() throws Exception {
		long drafts = CoreEmailHelper.getUserMailFolderAsRoot("user", domain.uid, "Drafts");

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
				.withCollectionId(drafts) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.execute(() -> CoreEmailHelper.addMail(login, password, "Drafts", "single_body.eml")) //
				.sync(request) //
				.startValidation() //
				.assertSyncKeyChanged() //
				.getValue("ServerId", serverId) //
				.endValidation() //
				.execute(emailValidation) //
				.sync(request.copy() //
						.withClientChangesBodyHtmlModify(serverId.get(), "te.fr@bluemind.net", "test",
								"&lt;div&gt;new body&lt;/div&gt;") //
						.build()) //
				.startValidation() //
				.assertResponseStatus(serverId.get(), SyncStatus.OK, ResponseSyncType.CHANGE) //
				.assertSyncKeyChanged() //
				.endValidation() //
				.sync(request) //
				.startValidation() //
				.endValidation() //
				.execute(emailValidation);
	}

}
