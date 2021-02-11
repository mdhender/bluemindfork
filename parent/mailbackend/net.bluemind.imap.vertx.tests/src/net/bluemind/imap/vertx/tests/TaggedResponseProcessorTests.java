/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.parsing.ImapChunker.ImapChunk;
import net.bluemind.imap.vertx.parsing.ImapChunker.Type;
import net.bluemind.imap.vertx.parsing.ResponsePayloadBuilder;
import net.bluemind.imap.vertx.parsing.TaggedResponseProcessor;

public class TaggedResponseProcessorTests {

	@Test
	public void testExtract() throws InterruptedException, ExecutionException, TimeoutException {
		String st = "V1 OK [CAPABILITY IMAP4rev1 LITERAL+ ID ENABLE ACL RIGHTS=kxten QUOTA MAILBOX-REFERRALS NAMESPACE UIDPLUS NO_ATOMIC_RENAME UNSELECT CHILDREN MULTIAPPEND BINARY CATENATE CONDSTORE ESEARCH SEARCH=FUZZY SORT SORT=MODSEQ SORT=DISPLAY SORT=UID THREAD=ORDEREDSUBJECT THREAD=REFERENCES THREAD=REFS ANNOTATEMORE ANNOTATE-EXPERIMENT-1 METADATA LIST-EXTENDED LIST-STATUS LIST-MYRIGHTS LIST-METADATA WITHIN QRESYNC SCAN XLIST XMOVE MOVE SPECIAL-USE CREATE-SPECIAL-USE DIGEST=SHA1 X-REPLICATION URLAUTH URLAUTH=BINARY LOGINDISABLED COMPRESS=DEFLATE X-QUOTA=STORAGE X-QUOTA=MESSAGE X-QUOTA=X-ANNOTATION-STORAGE X-QUOTA=X-NUM-FOLDERS IDLE] User logged in SESSIONID=<cyrus-5130-1613119587-2-14675163357648897633>";
		AtomicReference<String> gotMsg = new AtomicReference<>();
		AtomicReference<String> gotTag = new AtomicReference<>();
		AtomicReference<Status> gotStat = new AtomicReference<>();
		TaggedResponseProcessor<String> trp = new TaggedResponseProcessor<>(new ResponsePayloadBuilder<String>() {

			@Override
			public boolean untagged(Buffer b) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean tagged(String tag, Status st, String msg) {
				System.err.println("t: " + tag + ", st: " + st + ", msg: " + msg);
				gotTag.set(tag);
				gotMsg.set(msg);
				gotStat.set(st);
				return true;
			}

			@Override
			public ImapResponseStatus<String> build() {
				return null;
			}
		});
		ImapChunk ic = new ImapChunk(Type.Text, Buffer.buffer(st.getBytes(StandardCharsets.US_ASCII)), true);
		trp.processText(ic);
		trp.future().get(10, TimeUnit.SECONDS);
		assertNotNull(gotMsg.get());
		assertEquals("V1", gotTag.get());
		assertEquals(Status.Ok, gotStat.get());
	}

}
