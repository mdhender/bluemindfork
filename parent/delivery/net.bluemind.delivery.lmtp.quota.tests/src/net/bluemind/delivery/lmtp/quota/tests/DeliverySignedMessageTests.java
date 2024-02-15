/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.delivery.lmtp.quota.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.netty.util.internal.PlatformDependent;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.FlagsList;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.testhelper.client.Response;
import net.bluemind.lmtp.testhelper.client.VertxLmtpClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.user.api.User;
import net.bluemind.utils.ByteSizeUnit;

public class DeliverySignedMessageTests extends MailApiTestsBase {
	private static final Logger logger = LoggerFactory.getLogger(DeliverySignedMessageTests.class);

	private ItemValue<User> nearFull;
	private String rcptEmail;

	@BeforeEach
	@Override
	public void before(TestInfo info) throws Exception {
		super.before(info);
		IMailboxes mboxApi = serverProv.instance(IMailboxes.class, domUid);

		this.nearFull = sharedUser("near.full", domUid, userUid, 32, ByteSizeUnit.KB);
		MailboxQuota startQuota = mboxApi.getMailboxQuota(nearFull.uid);
		System.err.println("startQuota : " + startQuota);
		imapAsUser(nearFull.uid, sc -> {
			int sizeKb = 16;
			FlagsList fl = new FlagsList();
			int fresh = sc.append("INBOX", eml(ByteSizeUnit.KB.toBytes(sizeKb)), fl);
			assertTrue(fresh > 0);
			return null;
		});

		this.rcptEmail = userUid + "@" + domUid;
	}

	@Test
	public void testSmimeEml_oneRcpt()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo(rcptEmail);
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, Buffer.buffer(eml("test_smime.eml")));
			}).thenApply(dataResp -> {
				return checkCode(dataResp, 250);
			});
		});
	}

	@Test
	public void testSmimeEml_fourRcpt()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo(rcptEmail);
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo(rcptEmail);
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo(rcptEmail);
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo(rcptEmail);
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(4, Buffer.buffer(eml("test_smime-4.eml")));
			}).thenApply(dataResp -> {
				return checkCode(dataResp, 250);
			});
		});
	}

	protected <T> void withConnection(Function<VertxLmtpClient, CompletableFuture<T>> withClient)
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		lmtpClient().thenCompose(client -> {
			return client.connect().thenCompose(banner -> {
				return withClient.apply(client);
			}).thenCompose(v -> {
				System.out.println("Closing after client ops: " + v);
				return client.close();
			}).thenApply(v -> {
				System.out.println("Client close.");
				return null;
			});
		}).orTimeout(30, TimeUnit.SECONDS).join();
	}

	protected Response checkCode(Response resp, int... valid) {
		Set<Integer> codes = new HashSet<>();
		for (int code : valid) {
			codes.add(code);
		}
		List<String> parts = resp.parts();
		assertFalse(parts.isEmpty());
		for (String respPart : parts) {
			int code = Integer.parseInt(respPart.substring(0, 3));
			if (!codes.contains(code)) {
				fail(code + " is not an expected response code, expected: "
						+ Arrays.stream(valid).mapToObj(Integer::toString).collect(Collectors.joining(",")));
			}
		}
		return resp;

	}

	protected Response[] checkCode(Response[] resp, int... valid) {
		Set<Integer> codes = new HashSet<>();
		for (int code : valid) {
			codes.add(code);
		}
		for (Response r : resp) {
			List<String> parts = r.parts();
			assertFalse(parts.isEmpty());
			for (String respPart : parts) {
				int code = Integer.parseInt(respPart.substring(0, 3));
				if (!codes.contains(code)) {
					fail(code + " is not an expected response code, expected: "
							+ Arrays.stream(valid).mapToObj(Integer::toString).collect(Collectors.joining(",")));
				}
			}
		}
		return resp;

	}

	protected CompletableFuture<VertxLmtpClient> lmtpClient() {
		CompletableFuture<VertxLmtpClient> ret = new CompletableFuture<VertxLmtpClient>();
		Vertx vertx = VertxPlatform.getVertx();
		vertx.setTimer(1, tid -> {
			VertxLmtpClient client = new VertxLmtpClient(vertx, "127.0.0.1", 2400);
			ret.complete(client);
		});
		return ret;
	}

	private byte[] emlBuffer(long size) {
		StringBuilder sb = new StringBuilder();
		sb.append("From: " + userUid + "@" + domUid).append("\r\n");
		sb.append("Message-Id: <" + UUID.randomUUID().toString() + "@" + domUid + "\r\n");
		sb.append("X-Crap-Size: " + size + "\r\n");
		sb.append("Content-Type: multipart/signed\r\n");
		sb.append("\r\n");
		byte[] content = PlatformDependent.allocateUninitializedArray((int) size);
		ThreadLocalRandom.current().nextBytes(content);
		String b64string = Base64.getMimeEncoder(76, "\r\n".getBytes()).encodeToString(content);
		sb.append(b64string);
		return sb.toString().getBytes(StandardCharsets.US_ASCII);
	}

	private InputStream eml(long size) {
		return new ByteArrayInputStream(emlBuffer(size));
	}

	private byte[] eml(String filename) {
		try {
			return DeliverySignedMessageTests.class.getClassLoader().getResourceAsStream("emls/" + filename)
					.readAllBytes();
		} catch (Exception e) {
			// TODO: handle exception
		}

		return null;
	}

	protected Buffer resourceBuffer(String path) {
		Buffer ret = Buffer.buffer();
		try (InputStream in = resourceStream(path)) {
			Objects.requireNonNull(in, "Can't open resource with path " + path);
			byte[] data = ByteStreams.toByteArray(in);
			ret.appendBytes(data);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return ret;
	}

	protected InputStream resourceStream(String path) {
		return DeliverySignedMessageTests.class.getClassLoader().getResourceAsStream(path);
	}

}
