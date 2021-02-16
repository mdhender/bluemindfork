/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.mail.parsing.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.SystemUtils;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.junit.After;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import net.bluemind.backend.mail.api.DispositionType;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.parsing.Bodies;
import net.bluemind.backend.mail.parsing.EmlBuilder;
import net.bluemind.mime4j.common.Mime4JHelper;

public class EmlBuilderTests {

	private List<File> clearQueue = new LinkedList<>();

	private String sid = "sid";

	@After
	public void after() {
		clearQueue.forEach(File::delete);
		clearQueue.clear();
	}

	@Test
	public void testBuilderTextPlainUtf8() throws IOException {
		MessageBody mb = new MessageBody();
		mb.subject = "un text plain avec de l'unicod€";
		mb.structure = text("Des caractèr€s accentués", TextStyle.plain);
		Message built = EmlBuilder.of(mb, sid);
		String eml = toEML(built);
		assertNotNull(eml);
		assertFalse("Euro unicode character must be encoded", eml.contains("€"));
		assertTrue("UTF-8 charset must be explicit in eml", eml.contains("charset=utf-8"));
	}

	@Test
	public void testBuilderAlternativeAndRelated() throws IOException {
		MessageBody mb = new MessageBody();
		mb.subject = "un mail réaliste";
		Part text = text("Coucou, tu veux...", TextStyle.plain);

		Part html = text("<b>Coucou, tu veux...</b>\r\n<img src=\"cid:toto_id\"/>", TextStyle.html);
		Part img = new Part();
		img.mime = "image/png";
		img.contentId = "toto_id";
		img.fileName = "zizi coptère.png";
		img.dispositionType = DispositionType.INLINE;
		img.address = genPart(new byte[0]);
		Part related = multipart(MultipartStyle.related, html, img);
		Part alternative = multipart(MultipartStyle.alternative, text, related);
		mb.structure = alternative;
		String eml = toEML(EmlBuilder.of(mb, sid));
		assertNotNull(eml);
		assertTrue(eml.contains("inline"));
		assertTrue(eml.contains("<toto_id>"));
		// attach filename encoding
		assertFalse(eml.contains("coptère"));
		assertTrue(eml.contains("zizi"));
	}

	@Test
	public void testMessageCreationShouldNotLeakOpenFileDescriptor() throws IOException {
		assumeTrue(!SystemUtils.IS_OS_WINDOWS);
		long openFileDescriptorCountBefore = getOpenFileDescriptorCount();
		final BasicBodyFactory bbf = new BasicBodyFactory();
		for (int x = 0; x < 100; x++) {
			final MessageBody mb = new MessageBody();
			mb.subject = "un mail réaliste";
			mb.structure = text("avec presque rien dedans", TextStyle.plain);
			final Message message = EmlBuilder.of(mb, sid);
		}
		long openFileDescriptorCountAfter = getOpenFileDescriptorCount();
		assertEquals(openFileDescriptorCountBefore, openFileDescriptorCountAfter);

	}

	private long getOpenFileDescriptorCount() {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		try {
			Method getOpenFileDescriptorCountField = os.getClass().getDeclaredMethod("getOpenFileDescriptorCount");
			getOpenFileDescriptorCountField.setAccessible(true);
			return (long) getOpenFileDescriptorCountField.invoke(os);
		} catch (Exception e) {
			return -1l;
		}
	}

	public enum TextStyle {
		plain, html;
	}

	public enum MultipartStyle {
		mixed, related, alternative;
	}

	Part multipart(MultipartStyle multipartStyle, Part... children) {
		Part mpart = new Part();
		mpart.mime = "multipart/" + multipartStyle.name();
		for (Part p : children) {
			mpart.children.add(p);
		}
		return mpart;
	}

	private Part text(String content, TextStyle markup) throws IOException {
		Part p = new Part();
		p.mime = "text/" + markup.name();
		p.charset = "utf-8";
		p.encoding = "quoted-printable";
		p.address = genPart(content.getBytes());
		return p;
	}

	private String genPart(byte[] content) throws IOException {
		String addr = UUID.randomUUID().toString();
		File staging = new File(Bodies.getFolder(sid), addr + ".part");
		Files.write(content, staging);
		clearQueue.add(staging);
		return addr;
	}

	private String toEML(Message built) throws IOException {
		try (InputStream in = Mime4JHelper.asStream(built)) {
			String s = new String(ByteStreams.toByteArray(in));
			System.err.println(s);
			return s;
		}
	}

}
