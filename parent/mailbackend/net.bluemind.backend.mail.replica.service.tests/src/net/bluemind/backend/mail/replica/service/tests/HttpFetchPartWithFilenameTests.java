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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.mime.MimeTree;

public class HttpFetchPartWithFilenameTests extends AbstractRollingReplicationTests {

	@BeforeEach
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);

		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});
	}

	@Override
	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "sid");
	}

	@Test
	public void fetchPartWithFileName()
			throws IMAPException, InterruptedException, IOException, ExecutionException, TimeoutException {

		IMailboxFolders mboxesApi = provider().instance(IMailboxFolders.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName("INBOX");

		ItemValue<MailboxItem> item = this.addDraft(inbox);

		try (AsyncHttpClient httpClient = new DefaultAsyncHttpClient()) {

			RequestBuilder requestBuilder = new RequestBuilder();
			requestBuilder.setMethod("GET");
			requestBuilder.setHeader("X-BM-ApiKey", apiKey);
			requestBuilder.setHeader("Content-Type", "application/json");

			String pdfAttachmentAddress = item.value.body.structure.children.stream()
					.filter(childPart -> childPart.fileName != null && childPart.fileName.equals("schema_mailapi.pdf"))
					.findFirst().get().address;

			// Don't specify any encoding
			requestBuilder.setUrl("http://localhost:8090/api/mail_items/" + inbox.uid + "/part/" + item.value.imapUid
					+ "/" + pdfAttachmentAddress + "?filename=blabla.pdf");
			Response resp = httpClient.executeRequest(requestBuilder.build()).get(10, TimeUnit.SECONDS);

			String expectedContentDisposition = "attachment; filename=\"blabla.pdf\";";

			assertEquals(200, resp.getStatusCode());
			assertEquals(expectedContentDisposition, resp.getHeader("Content-Disposition"));

			// Ask for an encoding
			requestBuilder.setUrl("http://localhost:8090/api/mail_items/" + inbox.uid + "/part/" + item.value.imapUid
					+ "/" + pdfAttachmentAddress + "?encoding=base64&filename=blabla.pdf");
			resp = httpClient.executeRequest(requestBuilder.build()).get(10, TimeUnit.SECONDS);

			assertEquals(200, resp.getStatusCode());
			assertEquals(expectedContentDisposition, resp.getHeader("Content-Disposition"));
			System.err.println("len: " + resp.getResponseBodyAsBytes().length);
		}
	}

}
