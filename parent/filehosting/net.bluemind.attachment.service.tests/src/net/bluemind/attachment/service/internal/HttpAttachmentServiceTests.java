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
package net.bluemind.attachment.service.internal;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Collections;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.Test;

import io.vertx.core.json.JsonObject;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.filehosting.api.IFileHosting;

public class HttpAttachmentServiceTests extends AttachmentServiceTests {
	protected IFileHosting getFileHostingService(SecurityContext context) throws ServerFault {
		SecurityContext sec = new SecurityContext("toto", context.getSubject(), Collections.emptyList(),
				Arrays.asList("canRemoteAttach", "canUseFilehosting"), Collections.emptyMap(), domainName, "fr",
				"origine", false, "toto");
		Sessions.get().put("toto", sec);
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "toto").instance(IFileHosting.class,
				AttachmentServiceTests.domainName);

	}

	protected IAttachment getAttachmentService(SecurityContext context) throws ServerFault {
		SecurityContext sec = new SecurityContext("toto", context.getSubject(), Collections.emptyList(),
				Arrays.asList("canRemoteAttach", "canUseFilehosting"), Collections.emptyMap(), domainName, "fr",
				"origine", false, "toto");
		Sessions.get().put("toto", sec);
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "toto").instance(IAttachment.class,
				AttachmentServiceTests.domainName);
	}

	@Test
	public void testNonChunkedFatUpload() throws Exception {
		MappedByteBuffer buffer = twoGigs();
		System.err.println("upload " + buffer);
		try (AsyncHttpClient ahc = new DefaultAsyncHttpClient()) {
			Response resp = ahc.preparePut("http://127.0.0.1:8090/api/attachment/" + domainName + "/test.txt/share")
					.addHeader("X-BM-ApiKey", "toto")//
					.addHeader("Content-Length", Long.toString(Integer.MAX_VALUE - 1L)).setBody(buffer)//
					.execute().get();
			assertNotNull(resp);
			System.err.println("ahc " + resp.getStatusCode());
			System.err.println(new JsonObject(resp.getResponseBody()).encodePrettily());
		}
	}

	private MappedByteBuffer twoGigs() throws IOException, FileNotFoundException {
		File tmpFile = File.createTempFile("big", ".file-for-junit");
		try (RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw")) {
			long len = Integer.MAX_VALUE - 1L;
			raf.setLength(len);
			return raf.getChannel().map(MapMode.READ_ONLY, 0, len);
		} finally {
			tmpFile.delete();// NOSONAR
		}
	}

}
