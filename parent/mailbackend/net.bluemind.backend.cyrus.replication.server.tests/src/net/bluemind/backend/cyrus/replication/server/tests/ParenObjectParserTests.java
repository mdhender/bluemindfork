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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.common.io.CharStreams;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.protocol.parsing.JsonElement;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;

public class ParenObjectParserTests {

	@Test
	public void testBigMailboxObject() throws IOException {
		InputStream in = ParenObjectParserTests.class.getClassLoader()
				.getResourceAsStream("data/parent_objects/apply_mailbox.txt");
		String fat = CharStreams.toString(new InputStreamReader(in, StandardCharsets.US_ASCII));
		System.out.println("Object len is " + fat.length());
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(fat);
		assertNotNull(parsed);
		assertTrue(parsed.isObject());
		// System.out.println(parsed.asObject().encodePrettily());
	}

	@Test
	public void testRealListOfFolders() throws IOException {
		InputStream in = ParenObjectParserTests.class.getClassLoader()
				.getResourceAsStream("data/parent_objects/get_mailboxes.txt");
		String fat = CharStreams.toString(new InputStreamReader(in, StandardCharsets.US_ASCII));
		System.out.println("Object len is " + fat.length());
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(fat);
		assertNotNull(parsed);
		assertTrue(parsed.isArray());
		System.out.println(parsed.asArray().encodePrettily());
	}

	@Test
	public void testSmallProblematicFolder() throws IOException {
		String fat = "(\"blue-mind.net!user.adrien^francois.PARTENARIAT.D-F.FactorFX.Point Commercial\" \"blue-mind.net!juri.Tout.Relations Pro\")";
		System.out.println("Object len is " + fat.length());
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(fat);
		assertNotNull(parsed);
		assertTrue(parsed.isArray());
		System.out.println(parsed.asArray().encodePrettily());
	}

	@Test
	public void testParseMailboxObject() {
		String complexString = "%(UNIQUEID 002647c6582c5f46 MBOXNAME ex2016.vmw!user.nico SYNC_CRC 2950253899 "
				+ "SYNC_CRC_ANNOT 0 LAST_UID 7 HIGHESTMODSEQ 25 RECENTUID 7 RECENTTIME 1481733869 LAST_APPENDDATE 1481733763 POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 UIDVALIDITY 1479302982 PARTITION ex2016_vmw "
				+ "ACL \"admin0	lrswipkxtecda	147CAED2-F9AA-4B66-984D-94109FD4DBDE@ex2016.vmw	lrsp	526BE6B3-281B-4B78-BCCC-CBB7606CF2DA@ex2016.vmw	lrswipkxtecda	nico@ex2016.vmw	lrswipkxtecda	\" OPTIONS P RECORD (%(UID 2 MODSEQ 20 LAST_UPDATED 1482931800 FLAGS () INTERNALDATE 1481703222 SIZE 6454 GUID c32c8d6e83553db2d9905f19f65c918d896416fa) %(UID 3 MODSEQ 21 LAST_UPDATED 1482931800 FLAGS () INTERNALDATE 1481707761 SIZE 2230 GUID 7e4088434bca20ee5a8d822dd136da4339d9ca12) %(UID 4 MODSEQ 22 LAST_UPDATED 1482931800 FLAGS () INTERNALDATE 1481711078 SIZE 1341 GUID 8eceb45707ebc5a40032f1cd62a6da2f8acd4e92) %(UID 5 MODSEQ 23 LAST_UPDATED 1482931800 FLAGS () INTERNALDATE 1481715552 SIZE 7136 "
				+ "GUID b2d40dd7fc4a67016d951899edc41205d7f8beea) %(UID 6 MODSEQ 24 LAST_UPDATED 1482931800 FLAGS (\\Seen) INTERNALDATE 1481719363 SIZE 6879 GUID f1b6bb899366de8b97446ec17a18ccf419c64756) %(UID 7 MODSEQ 25 "
				+ "LAST_UPDATED 1482931800 FLAGS (\\Seen) BMARRAY (%(FOO BAR) %(TITI TOTO)) INTERNALDATE 1481733763 SIZE 2199 GUID be5be50f875ae0b710469f58aecf2f8b6c01eb56)))";
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(complexString);
		assertNotNull(parsed);
		assertTrue(parsed.isObject());
		System.out.println("parsed:\n" + parsed.asObject().encodePrettily());
	}

	@Test
	public void testParseAnotherMailboxObjectWithDollarFlags() {
		String complexString = "%(UNIQUEID 6399ac9b-a1b7-4b44-a1ea-41700c5c6355 MBOXNAME ex2016.vmw!marketing SYNC_CRC 2018734627 SYNC_CRC_ANNOT 0 LAST_UID 16 HIGHESTMODSEQ 22 RECENTUID 0 RECENTTIME 0 LAST_APPENDDATE 1531761979 POP3_LAST_LOGIN 0 POP3_SHOW_AFTER 0 UIDVALIDITY 1531392047 PARTITION bm-master__ex2016_vmw ACL \"anyone	p	admin0	lrswipkxtecda	83C21B7E-F4FE-4CF7-9197-4512A7FAFC4C@ex2016.vmw	lrswipkxtecd	\" OPTIONS P RECORD (%(UID 12 MODSEQ 18 LAST_UPDATED 1531761976 FLAGS ($NotJunk NotJunk) INTERNALDATE 1230571924 SIZE 6171233 GUID 3d1fb37cb62ac969a51809d2b4e07059a7bdb9b9) %(UID 13 MODSEQ 19 LAST_UPDATED 1531761977 FLAGS ($NotJunk NotJunk) INTERNALDATE 1359235380 SIZE 5799603 GUID f6db90a0590b22658f6c595167db5b54220fd8ab) %(UID 14 MODSEQ 20 LAST_UPDATED 1531761977 FLAGS ($NotJunk NotJunk) INTERNALDATE 1264159159 SIZE 5597518 GUID 52ca48ea8dff77bdc460d498ffd05b28c856eb34) %(UID 15 MODSEQ 21 LAST_UPDATED 1531761978 FLAGS ($NotJunk NotJunk) INTERNALDATE 1235037445 SIZE 6000400 GUID 941f803bc1a9039ed0cec186bac924b113f60824) %(UID 16 MODSEQ 22 LAST_UPDATED 1531761979 FLAGS ($NotJunk NotJunk) INTERNALDATE 1260464837 SIZE 5701216 GUID c1120437ed620ee7ea14ac73d9cfc356df2e33ee)))";
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(complexString);
		assertNotNull(parsed);
		assertTrue(parsed.isObject());
		JsonObject js = parsed.asObject();
		System.out.println("parsed:\n" + parsed.asObject().encodePrettily());
		JsonArray record = js.getJsonArray("RECORD");
		assertEquals(5, record.size());
	}

	@Test
	public void testParseMboxList() {
		String complexString = "(ex2016.vmw!user.tom ex2016.vmw!user.nico vagrant.vmw!user.jdoe vagrant.vmw!user.jdoe.Sent "
				+ "vagrant.vmw!user.jdoe.Outbox vagrant.vmw!user.jdoe.Trash "
				+ "vagrant.vmw!user.jdoe.Drafts vagrant.vmw!user.jdoe.Junk vagrant.vmw!user.janedoe vagrant.vmw!user.janedoe.Sent "
				+ "vagrant.vmw!user.janedoe.Outbox vagrant.vmw!user.janedoe.Trash vagrant.vmw!user.janedoe.Drafts vagrant.vmw!user.janedoe.Junk "
				+ "vagrant.vmw!domino^res^a2aae8f869e638e3c1257cc30023a0bd_at_domino^res vagrant.vmw!domino^res^a2aae8f869e638e3c1257cc30023a0bd_at_domino^res.Sent "
				+ "vagrant.vmw!domino^room^a8177012dabce540c1257cd0004bbb34_at_domino^res vagrant.vmw!domino^room^a8177012dabce540c1257cd0004bbb34_at_domino^res.Sent "
				+ "vagrant.vmw!domino^room^00c86c6c2095945ec1257cc20052c6fa_at_domino^res vagrant.vmw!domino^room^00c86c6c2095945ec1257cc20052c6fa_at_domino^res.Sent "
				+ "vagrant.vmw!user.admin ex2016.vmw!user.nico.Drafts ex2016.vmw!user.nico.Junk ex2016.vmw!user.nico.Outbox ex2016.vmw!user.nico.Sent ex2016.vmw!user.nico.Trash "
				+ "ex2016.vmw!user.sylvain ex2016.vmw!user.sylvain.Sent ex2016.vmw!user.sylvain.Outbox ex2016.vmw!user.sylvain.Trash ex2016.vmw!user.sylvain.Drafts ex2016.vmw!user.sylvain.Junk ex2016.vmw!user.admin ex2016.vmw!user.admin.Drafts ex2016.vmw!user.admin.Junk ex2016.vmw!user.admin.Outbox ex2016.vmw!user.admin.Sent ex2016.vmw!user.admin.Trash ex2016.vmw!user.sga ex2016.vmw!user.sga.Drafts ex2016.vmw!user.sga.Junk ex2016.vmw!user.sga.Outbox ex2016.vmw!user.sga.Sent ex2016.vmw!user.sga.Trash ex2016.vmw!tom ex2016.vmw!user.tom.Drafts ex2016.vmw!DELETED.user.tom.Drafts.5863AA42 ex2016.vmw!user.tom.Junk ex2016.vmw!DELETED.user.tom.Junk.5863AA42 ex2016.vmw!user.tom.Outbox "
				+ "ex2016.vmw!DELETED.user.tom.Outbox.5863AA42 ex2016.vmw!user.tom.Sent ex2016.vmw!DELETED.user.tom.Sent.5863AA42 ex2016.vmw!user.tom.Trash ex2016.vmw!DELETED.user.tom.Trash.5863AA42)";
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(complexString);
		assertNotNull(parsed);
		assertTrue(parsed.isArray());
		System.out.println("parsed:\n" + parsed.asArray().encodePrettily());
	}

	@Test
	public void testParseSimpleMboxList() {
		String complexString = "(vagrant.vmw!user.admin.cuculcucu \"vagrant.vmw!user.admin.tango dd\")";
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(complexString);
		assertNotNull(parsed);
		assertTrue(parsed.isArray());
		System.out.println("parsed:\n" + parsed.asArray().encodePrettily());
	}

	@Test
	public void testParseObjectValue() {
		String complexString = "%(key %(foo bar) john (bang bang))";
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(complexString);
		assertNotNull(parsed);
		assertTrue(parsed.isObject());
		JsonObject obj = parsed.asObject();
		assertEquals(2, obj.getJsonArray("john").size());
		assertEquals("bar", obj.getJsonObject("key").getString("foo"));
		System.out.println("parsed:\n" + obj.encodePrettily());
	}

	@Test
	public void testParseWithExtraSpace() {
		String spaced = "%(UNIQUEID 5596488a58661ddc "
				+ "MBOXNAME vagrant.vmw!user.admin LAST_UID 1 HIGHESTMODSEQ 3 RECENTUID 1 RECENTTIME 1483088318 LAST_APPENDDATE 1483088316 "
				+ "POP3_LAST_LOGIN 0 UIDVALIDITY 1483087324 PARTITION vagrant_vmw "
				+ "ACL \"admin@vagrant.vmw	lrswipkxtecda	admin0	lrswipkxtecda	\" OPTIONS P SYNC_CRC 2729519180 "
				+ "RECORD (  %(UID 1 MODSEQ 3 LAST_UPDATED 1483088316 FLAGS () INTERNALDATE 1483088316 SIZE 1331 GUID 0cd4d7a059b7b5772b33881da783536bf06020d7)))";
		ParenObjectParser pop = ParenObjectParser.create();
		JsonElement parsed = pop.parse(spaced);
		assertNotNull(parsed);
		assertTrue(parsed.isObject());
		JsonObject obj = parsed.asObject();
		System.out.println("parsed:\n" + obj.encodePrettily());
	}

}
