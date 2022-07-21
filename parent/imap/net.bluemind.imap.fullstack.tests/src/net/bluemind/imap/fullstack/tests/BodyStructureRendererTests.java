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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.fullstack.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.imap.driver.mailapi.BodyStructureRenderer;

public class BodyStructureRendererTests {

	@Test
	public void testRenderMixedWithRelated() {
		String mixedRelated = "{\"mime\": \"multipart/mixed\", \"size\": 53615, \"address\": \"TEXT\", \"children\": [{\"mime\": \"multipart/related\", \"size\": 0, \"address\": \"1\", \"children\": [{\"mime\": \"text/html\", \"size\": 174, \"address\": \"1.1\", \"charset\": \"utf-8\", \"encoding\": \"quoted-printable\"}, {\"mime\": \"image/png\", \"size\": 38378, \"address\": \"1.2\", \"charset\": \"us-ascii\", \"encoding\": \"base64\", \"contentId\": \"<sapin-ad8a4976-bbf0-4b28-bc71-f074d0075423@bluemind.net>\", \"dispositionType\": \"INLINE\"}]}]}";
		BodyStructureRenderer bsr = new BodyStructureRenderer();
		ValueReader<Part> reader = JsonUtils.reader(Part.class);
		Part root = reader.read(mixedRelated);
		String result = bsr.from(root);
		assertNotNull(result);
		System.err.println("result: " + result);
	}

	@Test
	public void testRenderSimple() {
		String mixedRelated = "{\"mime\": \"text/html\", \"size\": 22927, \"address\": \"1\", \"charset\": \"utf-8\", \"encoding\": \"7bit\"}";
		BodyStructureRenderer bsr = new BodyStructureRenderer();
		ValueReader<Part> reader = JsonUtils.reader(Part.class);
		Part root = reader.read(mixedRelated);
		String result = bsr.from(root);
		assertNotNull(result);
		System.err.println("result: " + result);
	}

}
