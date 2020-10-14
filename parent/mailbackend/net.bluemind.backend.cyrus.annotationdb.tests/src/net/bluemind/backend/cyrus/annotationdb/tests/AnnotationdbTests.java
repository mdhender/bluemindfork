/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.cyrus.annotationdb.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.io.BaseEncoding;

import net.bluemind.backend.cyrus.annotationdb.AnnotationDb;
import net.bluemind.backend.cyrus.annotationdb.ConversationInfo;
import net.bluemind.backend.cyrus.annotationdb.ConversationInfo.ConversationElement;
import net.bluemind.backend.cyrus.annotationdb.ConversationInfo.FORMAT;

public class AnnotationdbTests {

	@Test
	public void testSimpleAnnotation() throws Exception {
		List<String> content = load("simple.db");

		AnnotationDb db = new AnnotationDb();
		content.forEach(db::accept);
		ConversationInfo info = db.get();

		assertEquals(2, info.conversations.size());

		boolean found1 = false;
		boolean found2 = false;

		for (ConversationElement conversation : info.conversations) {
			assertEquals(FORMAT.BODY_GUID, conversation.format);
			if (conversation.conversationId == convertId("c3935370c11c6b47")) {
				assertEquals(2, conversation.uids.size());
				assertTrue(conversation.uids.contains("ee974c2e6ef01f3ddc1a157dd91a1e0d3f48add4"));
				assertTrue(conversation.uids.contains("52608a91ca7560557372c0120476c6476b181700"));
				found1 = true;
			} else if (conversation.conversationId == convertId("8f5f74ed9d71cd5f")) {
				assertEquals(1, conversation.uids.size());
				assertTrue(conversation.uids.contains("1eacad0c9618c64d977cede0a30d7d93f29f4fa2"));
				found2 = true;
			}
		}

		assertTrue(found1);
		assertTrue(found2);
	}

	@Test
	public void testComplexAnnotation() throws Exception {
		List<String> content = load("multi_folder.db");

		AnnotationDb db = new AnnotationDb();
		content.forEach(db::accept);
		ConversationInfo info = db.get();

		assertEquals(5, info.conversations.size());

		/*
		 * B8223d0561bcae6d6 0 (39 4 4 0 () ((0 31 1 1 0) (1 32 2 2 0) (6 39 1 1 0))
		 * (("user2 user2" NIL user2 dev-bm4.test 1622109118 2) ("user1 user1" NIL user1
		 * dev-bm4.test 1622109087 2)) test1 6866
		 * ((13d009b710a3edc4bffe161b8bb61da4d041c8b1 1 1622109041 1277517506)
		 * (32659930dd636c22165681a5875eb369316aec5a 2 1622109088 691226745)
		 * (4db837796b0c8d256b7aa5df5fa282989169e820 1 1622109118 3363666048)))
		 * 
		 * Bbd76fda8f1ea9ebd 0 (38 2 2 0 () ((0 38 1 1 0) (1 25 1 1 0)) (("user2 user2"
		 * NIL user2 dev-bm4.test 1622109063 2)) self 2385
		 * ((2c852449fa8395afc0ecbc82bdfd84a78d779d89 1 1622109063 1357619756)
		 * (3b13b51e03f0ea0e36a880addf4049f2d97e9b4f 1 1622109063 1357619756)))
		 * 
		 * Bdaf4afc37e5f32fa 0 (37 3 2 0 () ((0 37 1 0 0) (1 24 1 1 0) (6 35 1 1 0))
		 * (("user1 user1" NIL user1 dev-bm4.test 1622109081 1) ("user2 user2" NIL user2
		 * dev-bm4.test 1622109052 1)) okok 2819
		 * ((4b077622753639e876b3fab18acc0e67aec6f36e 1 1622109053 2917469708)
		 * (31279c6b647078209f58bdfc086e96e84c378d4b 1 1622109081 2491810786)))
		 * 
		 * Bdb9d9ea5771c435f 0 (29 3 3 1 () ((0 29 2 2 1) (1 10 1 1 0)) (("user1 user1"
		 * NIL user1 dev-bm4.test 1622109099 2) ("user2 user2" NIL user2 dev-bm4.test
		 * 1622022637 1)) firstmessage 5556 ((4a6e47447c75484dcf5bd070dffd5afc6df32927 1
		 * 1622022619 733543053) (1583020d8db208357c6712c5846d86d909b06ba4 1 1622022637
		 * 2522178585) (05ef7b5c5d3e53020fb39f314e58b68f1d3eeb42 1 1622109100
		 * 3894911539)))
		 */

		boolean check8223d0561bcae6d6 = false;
		boolean checkbd76fda8f1ea9ebd = false;
		boolean checkdaf4afc37e5f32fa = false;
		boolean checkdb9d9ea5771c435f = false;
		boolean check8f5f74ed9d71cd5f = false;
		String id8223d0561bcae6d6 = String.valueOf(convertId("8223d0561bcae6d6"));
		String idbd76fda8f1ea9ebd = String.valueOf(convertId("bd76fda8f1ea9ebd"));
		String iddaf4afc37e5f32fa = String.valueOf(convertId("daf4afc37e5f32fa"));
		String iddb9d9ea5771c435f = String.valueOf(convertId("db9d9ea5771c435f"));
		String id8f5f74ed9d71cd5f = String.valueOf(convertId("8f5f74ed9d71cd5f"));
		for (ConversationElement conversation : info.conversations) {
			assertEquals(FORMAT.BODY_GUID, conversation.format);
			if (String.valueOf(conversation.conversationId).equals(id8223d0561bcae6d6)) {
				check8223d0561bcae6d6 = true;
				assertEquals(3, conversation.uids.size());
				assertTrue(conversation.uids.contains("13d009b710a3edc4bffe161b8bb61da4d041c8b1"));
				assertTrue(conversation.uids.contains("32659930dd636c22165681a5875eb369316aec5a"));
				assertTrue(conversation.uids.contains("4db837796b0c8d256b7aa5df5fa282989169e820"));
			} else if (String.valueOf(conversation.conversationId).equals(idbd76fda8f1ea9ebd)) {
				checkbd76fda8f1ea9ebd = true;
				assertEquals(2, conversation.uids.size());
				assertTrue(conversation.uids.contains("2c852449fa8395afc0ecbc82bdfd84a78d779d89"));
				assertTrue(conversation.uids.contains("3b13b51e03f0ea0e36a880addf4049f2d97e9b4f"));
			} else if (String.valueOf(conversation.conversationId).equals(iddaf4afc37e5f32fa)) {
				checkdaf4afc37e5f32fa = true;
				assertEquals(2, conversation.uids.size());
				assertTrue(conversation.uids.contains("4b077622753639e876b3fab18acc0e67aec6f36e"));
				assertTrue(conversation.uids.contains("31279c6b647078209f58bdfc086e96e84c378d4b"));
			} else if (String.valueOf(conversation.conversationId).equals(iddb9d9ea5771c435f)) {
				checkdb9d9ea5771c435f = true;
				assertEquals(3, conversation.uids.size());
				assertTrue(conversation.uids.contains("4a6e47447c75484dcf5bd070dffd5afc6df32927"));
				assertTrue(conversation.uids.contains("1583020d8db208357c6712c5846d86d909b06ba4"));
				assertTrue(conversation.uids.contains("05ef7b5c5d3e53020fb39f314e58b68f1d3eeb42"));
			} else if (String.valueOf(conversation.conversationId).equals(id8f5f74ed9d71cd5f)) {
				check8f5f74ed9d71cd5f = true;
				assertEquals(1, conversation.uids.size());
				assertTrue(conversation.uids.contains("1eacad0c9618c64d977cede0a30d7d93f29f4fa2"));
			}
		}

		assertTrue(check8223d0561bcae6d6);
		assertTrue(checkbd76fda8f1ea9ebd);
		assertTrue(checkdaf4afc37e5f32fa);
		assertTrue(checkdb9d9ea5771c435f);
		assertTrue(check8f5f74ed9d71cd5f);
	}

	@Test
	public void testSimpleAnnotationMessageIdFormat() throws Exception {
		List<String> content = load("old_format.db");

		AnnotationDb db = new AnnotationDb();
		content.forEach(db::accept);
		ConversationInfo info = db.get();

		assertEquals(3, info.conversations.size());

		boolean found1 = false;
		boolean found2 = false;
		boolean found3 = false;

		for (ConversationElement conversation : info.conversations) {
			assertEquals(FORMAT.MESSAGE_ID, conversation.format);
			if (conversation.conversationId == convertId("17964ea4e28e5766")) {
				assertEquals(1, conversation.uids.size());
				assertTrue(conversation.uids.contains("<redmine.journal-5769.20150601104631@oss-sos.fr>"));
				found1 = true;
			} else if (conversation.conversationId == convertId("1fbf4c7cddb39e73")) {
				assertEquals(1, conversation.uids.size());
				assertTrue(conversation.uids.contains("<redmine.journal-5770.20150601111122@oss-sos.fr>"));
				found2 = true;
			} else if (conversation.conversationId == convertId("2fd66834bb62128e")) {
				assertEquals(1, conversation.uids.size());
				assertTrue(conversation.uids.contains("<redmine.journal-5772.20150601134336@oss-sos.fr>"));
				found3 = true;
			}
		}

		assertTrue(found1);
		assertTrue(found2);
		assertTrue(found3);
	}

	private long convertId(String id) {
		return new BigInteger(BaseEncoding.base16().decode(id.toUpperCase())).longValue();
	}

	private List<String> load(String name) throws IOException {
		List<String> data = new ArrayList<String>();
		String line;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(
				AnnotationdbTests.class.getClassLoader().getResourceAsStream("resources/" + name)))) {
			while ((line = in.readLine()) != null) {
				// add CRLF to be consistent with node exitlist
				data.add(line + "\r\n");
			}
		}
		return data;
	}

}
