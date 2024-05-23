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
import net.bluemind.eas.http.tests.validators.EmailValidator.Attachment;
import net.bluemind.eas.http.tests.validators.EmailValidator.BodyParts;
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
		Runnable emailValidationBefore = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.withBody(new BodyParts("test",
						"<div style=\"font-family: Montserrat, montserrat, &quot;Source Sans&quot;, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 9.75pt; color: rgb(0, 0, 0);\">test</div><div data-bm-signature=\"d18dfdd9-de9e-496e-98dd-2f275f457c30\"></div>")) //
				.build() //
				.validate();
		Runnable emailValidationAfter = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.withBody(new BodyParts("new body", "<div>new body</div>")) //
				.build() //
				.validate();
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
				.execute(emailValidationBefore) //
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
				.execute(emailValidationAfter);
	}

	@Test
	public void testMailSyncDraftClientChangesHavingAttachmentsShouldMergeChangeswithoutLosingAttachments()
			throws Exception {
		long drafts = CoreEmailHelper.getUserMailFolderAsRoot("user", domain.uid, "Drafts");

		AtomicReference<String> serverId = new AtomicReference<>();
		Runnable emailValidationBefore = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.withBody(new BodyParts("test",
						"<div style=\"font-family: Montserrat, montserrat, &quot;Source Sans&quot;, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 9.75pt; color: rgb(0, 0, 0);\">test</div><div data-bm-signature=\"d18dfdd9-de9e-496e-98dd-2f275f457c30\"></div>")) //
				.withAttachments(new Attachment("Screenshot 2024-05-22 at 09.54.16.png", "image/png")) //
				.build() //
				.validate();
		Runnable emailValidationAfter = () -> new EmailValidator.Builder(domain.uid, serverId.get()) //
				.withFrom("te.fr@bluemind.net") //
				.withSubject("test") //
				.withHeader("X-Mailer", "BlueMind-MailApp-v5") //
				.withTo("te.fr@bluemind.net") //
				.withBody(new BodyParts("new body", "<div>new body</div>")) //
				.withAttachments(new Attachment("Screenshot 2024-05-22 at 09.54.16.png", "image/png")) //
				.build() //
				.validate();

		SyncRequest request = new SyncRequestBuilder().withChanges().build();

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login, password) //
				.withCollectionId(drafts) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.execute(() -> CoreEmailHelper.addMail(login, password, "Drafts", "attachment.eml")) //
				.sync(request) //
				.startValidation() //
				.assertSyncKeyChanged() //
				.getValue("ServerId", serverId) //
				.endValidation() //
				.execute(emailValidationBefore) //
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
				.execute(emailValidationAfter);
	}

	@Test
	public void testMailSyncInstallationContainer() throws Exception {
		long installationContainerId = 1;

		SyncRequest request = new SyncRequestBuilder().withChanges().build();

		new SyncHelper.SyncHelperBuilder() //
				.withAuth(login, password) //
				.withCollectionId(installationContainerId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.execute(() -> CoreEmailHelper.addMail(login, password, "Drafts", "single_body.eml")) //
				.sync(request) //
				.startValidation() //
				.assertSyncStatus(String.valueOf(installationContainerId), SyncStatus.OBJECT_NOT_FOUND) //
				.endValidation();
	}

}
