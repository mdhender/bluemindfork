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
package net.bluemind.imap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;
import net.bluemind.imap.command.parser.BodyStructureParser;
import net.bluemind.imap.mime.BodyParam;
import net.bluemind.imap.mime.MimePart;
import net.bluemind.imap.mime.MimeTree;
import net.bluemind.utils.FileUtils;

public class BodyStructureParserTests extends TestCase {

	public byte[] openTestStructure(String filePath) {
		InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
		try {
			return FileUtils.streamBytes(is, true);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Cannot open " + filePath);
		}
		return null;
	}

	public void testParseRFC2231EncodedParam() {
		byte[] bs = openTestStructure("data/rfc2231-param-encoding.dat");
		BodyStructureParser bsp = new BodyStructureParser();
		MimeTree mt = bsp.parse(bs);
		assertNotNull(mt);
		MimePart part = mt.getChildren().get(1).getChildren().get(1).getChildren().get(1);
		if (part.getAddress().equals("2.2.2")) {
			assertEquals("Infos erronées du 010910_1.pdf", part.getBodyParam("name").getValue());
			return;
		}
		fail("expected pdf not found");
	}

	public void testParseMozDeleted() {
		byte[] bs = openTestStructure("data/bs_01.dat");
		BodyStructureParser bsp = new BodyStructureParser();
		try {
			MimeTree mt = bsp.parse(bs);
			assertNotNull(mt);
		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	public void testParseBS04() {
		byte[] bs = openTestStructure("data/bs_04.dat");
		BodyStructureParser bsp = new BodyStructureParser();
		MimeTree mt = bsp.parse(bs);
		assertNotNull(mt);
		System.out.println("mt:\n" + mt);
	}

	public void testParseBS05() {
		byte[] bs = openTestStructure("data/bs_05.dat");
		BodyStructureParser bsp = new BodyStructureParser();
		try {
			MimeTree mt = bsp.parse(bs);
			assertNotNull(mt);
			System.out.println("mt:\n" + mt);
		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

	public void testParseBS06() {
		byte[] bs = openTestStructure("data/bs_06.dat");
		BodyStructureParser bsp = new BodyStructureParser();
		MimeTree mt = bsp.parse(bs);
		assertNotNull(mt);
		System.out.println("mt:\n" + mt);
		for (MimePart mp : mt) {
			System.err.println("mp " + mp.getAddress());
			Collection<BodyParam> bp = mp.getBodyParams();
			if (bp != null) {
				for (BodyParam s : bp) {
					System.err.println(s);
				}
			}
		}
	}

	// public void testInfinitLoop() {
	// byte[] bs = openTestStructure("data/bs_02.dat");
	// BodyStructureParser bsp = new BodyStructureParser();
	// try {
	// MimeTree mt = bsp.parse(bs);
	// assertNotNull(mt);
	// } catch (Throwable t) {
	// t.printStackTrace();
	// fail();
	// }
	// }

	public void testNestedWithDominoDisclaimer() {
		byte[] bs = openTestStructure("data/bs_03.dat");
		BodyStructureParser bsp = new BodyStructureParser();
		try {
			MimeTree mt = bsp.parse(bs);
			assertNotNull(mt);
			System.out.println("parsed tree: " + mt);
		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
	}

}
