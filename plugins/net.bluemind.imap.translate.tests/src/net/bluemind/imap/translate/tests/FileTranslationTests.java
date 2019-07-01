/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012
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
 * END LICENSE
 */

package net.bluemind.imap.translate.tests;

import java.io.File;
import java.io.IOException;

import org.vertx.java.core.json.JsonObject;

import com.google.common.io.Files;

import junit.framework.TestCase;
import net.bluemind.imap.translate.impl.FileTranslation;

public class FileTranslationTests extends TestCase {

	public void setUp() throws Exception {
		System.out.println("Writing new version of translation");
		JsonObject jso = new JsonObject();
		jso.putString("Sent", "Ma prose");
		jso.putString("Trash", "La poubelle");
		jso.putString("Dossiers partagés", "Dossiers partagés");
		jso.encodePrettily();
		Files.write(jso.encodePrettily().getBytes(), new File("/etc/bm/imap.i18n.fr"));
	}

	public void testLoadTranslation() throws IOException {
		FileTranslation ft = new FileTranslation(new File("/etc/bm/imap.i18n.fr"));
		assertNotNull(ft);
	}

	public void testIsTranslated() throws IOException {
		FileTranslation ft = new FileTranslation(new File("/etc/bm/imap.i18n.fr"));
		assertTrue(ft.isTranslated("La poubelle"));
		assertFalse(ft.isTranslated("Toto"));
		assertFalse(ft.isTranslated("Dossiers partagés"));
	}

	public void testToImap() throws IOException {
		FileTranslation ft = new FileTranslation(new File("/etc/bm/imap.i18n.fr"));
		assertEquals("\"Trash\"", ft.toImap("La poubelle"));

		// shared
		assertEquals("\"Autres utilisateurs/admin/Trash\"", ft.toImap("Autres utilisateurs/admin/La poubelle"));
		assertEquals("\"Autres utilisateurs/admin/Trash/1/2/3/soleil\"",
				ft.toImap("Autres utilisateurs/admin/La poubelle/1/2/3/soleil"));

		assertEquals("\"Autres utilisateurs/admin/blah/1/2/3/soleil\"",
				ft.toImap("Autres utilisateurs/admin/blah/1/2/3/soleil"));

	}

	public void testToUser() throws IOException {
		FileTranslation ft = new FileTranslation(new File("/etc/bm/imap.i18n.fr"));
		assertEquals("\"La poubelle\"", ft.toUser("Trash"));

		// shared
		assertEquals("\"Autres utilisateurs/admin/La poubelle\"", ft.toUser("Autres utilisateurs/admin/Trash"));
		assertEquals("\"Autres utilisateurs/admin/La poubelle/1/2/3/soleil\"",
				ft.toUser("Autres utilisateurs/admin/Trash/1/2/3/soleil"));

		assertEquals("\"Dossiers partagés/p.project/La poubelle\"", ft.toUser("Dossiers partagés/p.project/Trash"));
		assertEquals("\"Dossiers partagés/p.project/La poubelle/1/2/3/soleil\"",
				ft.toUser("Dossiers partagés/p.project/Trash/1/2/3/soleil"));
		assertEquals("\"Dossiers partagés/p.project/blah\"", ft.toUser("Dossiers partagés/p.project/blah"));

	}
}
