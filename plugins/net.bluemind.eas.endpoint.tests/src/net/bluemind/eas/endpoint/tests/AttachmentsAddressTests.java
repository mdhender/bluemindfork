/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.eas.endpoint.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;

import net.bluemind.eas.command.sync.SyncEndpoint;
import net.bluemind.eas.endpoint.tests.helpers.TestMail;
import net.bluemind.eas.http.wbxml.WbxmlHandlerBase;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.mime.MimePart;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class AttachmentsAddressTests extends AbstractEndpointTest {

	@Override
	public WbxmlHandlerBase createEndpoint() {
		return new SyncEndpoint();
	}

	public void testMime4jExpandIsCorrect() throws Exception {
		TestMail tm = appendEml("INBOX", "data/ItemOperations/with_attachments.eml", new FlagsList());
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");

			// Structure from cyrus
			Collection<MimeTree> struct = sc.uidFetchBodyStructure(Arrays.asList(tm.uid));
			assertEquals(1, struct.size());
			MimeTree tree = struct.iterator().next();
			System.out.println("Tree:\n" + tree);
			Set<String> addressesFromBodyStructure = new LinkedHashSet<>();
			fillSet(addressesFromBodyStructure, tree);

			IMAPByteSource stream = sc.uidFetchMessage(tm.uid);
			Message parsed = Mime4JHelper.parse(stream.source().openStream());
			stream.close();
			Multipart multipart = (Multipart) parsed.getBody();
			List<AddressableEntity> asParts = Mime4JHelper.expandParts(multipart.getBodyParts());
			Set<String> addressesFromMime4j = new LinkedHashSet<>();
			for (AddressableEntity ae : asParts) {
				addressesFromMime4j.add(ae.getMimeAddress());
			}
			assertEquals(addressesFromBodyStructure, addressesFromMime4j);
			parsed.dispose();
		} catch (IMAPException e) {
		}
	}

	public void testMime4jTreeIsCorrect() throws Exception {
		TestMail tm = appendEml("INBOX", "data/ItemOperations/with_attachments.eml", new FlagsList());
		try (StoreClient sc = new StoreClient(vmHostname, 143, login, password)) {
			assertTrue(sc.login());
			sc.select("INBOX");

			// Structure from cyrus
			Collection<MimeTree> struct = sc.uidFetchBodyStructure(Arrays.asList(tm.uid));
			assertEquals(1, struct.size());
			MimeTree tree = struct.iterator().next();
			System.out.println("Tree:\n" + tree);
			Set<String> addressesFromBodyStructure = new LinkedHashSet<>();
			fillSet(addressesFromBodyStructure, tree);

			IMAPByteSource stream = sc.uidFetchMessage(tm.uid);
			Message parsed = Mime4JHelper.parse(stream.source().openStream());
			stream.close();
			Multipart multipart = (Multipart) parsed.getBody();
			List<AddressableEntity> asParts = Mime4JHelper.expandTree(multipart.getBodyParts());
			Set<String> addressesFromMime4j = new LinkedHashSet<>();
			for (AddressableEntity ae : asParts) {
				addressesFromMime4j.add(ae.getMimeAddress());
			}
			assertEquals(addressesFromBodyStructure, addressesFromMime4j);
			parsed.dispose();
		} catch (IMAPException e) {
		}
	}

	private void fillSet(Set<String> addressesFromBodyStructure, MimePart part) {
		if (part.getAddress() != null && !part.getAddress().isEmpty()) {
			addressesFromBodyStructure.add(part.getAddress());
		}
		if (part.getChildren() == null || part.getChildren().size() == 0) {
			return;
		}
		for (MimePart child : part.getChildren()) {
			fillSet(addressesFromBodyStructure, child);
		}
	}

}
