/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.lmtp.proxy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import net.bluemind.lmtp.impl.CoreStateListener;
import net.bluemind.lmtp.testhelper.client.Request;
import net.bluemind.lmtp.testhelper.client.Response;
import net.bluemind.lmtp.testhelper.client.VertxLmtpClient;
import net.bluemind.lmtp.testhelper.model.FakeMailbox;
import net.bluemind.lmtp.testhelper.model.FakeMailbox.State;
import net.bluemind.lmtp.testhelper.model.MailboxesModel;
import net.bluemind.lmtp.testhelper.model.MockServerStats;
import net.bluemind.lmtp.testhelper.server.MockServer;
import net.bluemind.lmtp.testhelper.server.ProxyServer;
import net.bluemind.metrics.testhelper.MetricsHelper;
import net.bluemind.metrics.testhelper.TestRegistry;
import net.bluemind.system.api.SystemState;

public abstract class AbstractChainTest {

	protected TestRegistry testRegistry;

	@Before
	public void before() throws Exception {
		CoreStateListener.state = SystemState.CORE_STATE_RUNNING;
		testRegistry = MetricsHelper.beforeTests();
		MailboxesModel.get().reset();
		MockServerStats.get().reset();
		MockServer.start();
		ProxyServer.start();

		MailboxesModel.get()//
				.addValidSender("sender@bm.lan")//
				.addMailbox(new FakeMailbox("full@bm.lan", State.OverQuotaOnNextMail)) //
				.addMailbox(new FakeMailbox("overq@bm.lan", State.OverQuota)) //
				.addMailbox(new FakeMailbox("overq2@bm.lan", State.OverQuota)) //
				.addMailbox(new FakeMailbox("fucked@bm.lan", State.Fucked)) //
				.addMailbox(new FakeMailbox("recip@bm.lan", State.Ok)) //
				.addMailbox(new FakeMailbox("recip2@bm.lan", State.Ok));
	}

	@After
	public void after() throws Exception {
		MailboxesModel.get().reset();
		MockServerStats.get().reset();
		ProxyServer.stop();
		MockServer.stop();
		MetricsHelper.afterTests();
	}

	protected CompletableFuture<Response> checkCode(Response resp, int... valid) {
		Set<Integer> codes = new HashSet<>();
		for (int code : valid) {
			codes.add(code);
		}
		for (String respPart : resp.parts()) {
			int code = Integer.parseInt(respPart.substring(0, 3));
			if (!codes.contains(code)) {
				fail(code + " is not an expected response code, expected: "
						+ Arrays.stream(valid).mapToObj(Integer::toString).collect(Collectors.joining(",")));
			}
		}
		return CompletableFuture.completedFuture(resp);

	}

	protected CompletableFuture<Response[]> checkCode(Response[] resp, int... valid) {
		Set<Integer> codes = new HashSet<>();
		for (int code : valid) {
			codes.add(code);
		}
		for (Response r : resp) {
			for (String respPart : r.parts()) {
				int code = Integer.parseInt(respPart.substring(0, 3));
				if (!codes.contains(code)) {
					fail(code + " is not an expected response code, expected: "
							+ Arrays.stream(valid).mapToObj(Integer::toString).collect(Collectors.joining(",")));
				}
			}
		}
		return CompletableFuture.completedFuture(resp);

	}

	protected abstract CompletableFuture<VertxLmtpClient> lmtpClient();

	protected void withConnection(Function<VertxLmtpClient, CompletableFuture<?>> withClient)
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		lmtpClient().thenAccept(client -> {
			try {
				client.connect().thenCompose(banner -> {
					return withClient.apply(client);
				}).thenCompose(v -> {
					System.out.println("Closing after client ops: " + v);
					return client.close();
				}).thenAccept(v -> {
					System.out.println("Client close.");
					ret.complete(null);
				});
			} catch (Exception e) {
				ret.completeExceptionally(e);
			}
		});
		ret.get(30, TimeUnit.SECONDS);
	}

	@Test
	public void testLHLO()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan");
		});
	}

	@Test
	public void testValidSenderValidRecip()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				return checkCode(rcptResp, 250);
			});
		});
	}

	@Test
	public void testValidSenderUnknownRecip()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("unknown@bm.lan");
			}).thenCompose(rcptResp -> {

				return checkCode(rcptResp, 550);
			});
		});
	}

	@Test
	public void testValidSenderValidRecipWithData()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				return checkCode(dataResp, 250);
			});
		});
	}

	@Test
	public void testValidSenderTwoValidRecipWithData()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo("recip2@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(2, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				assertEquals(2, dataResp.length);
				return checkCode(dataResp, 250);
			});
		});
	}

	@Test
	public void testValidSenderOneRecipOneOverQuotaWithData()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo("overq@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 452);
				return client.data(1, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				return checkCode(dataResp, 250);
			});
		});
	}

	@Test
	public void testValidSenderAllRecipOverQuotaWithData()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.rcptTo("overq@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 452);
				return client.data(0, Buffer.buffer("From: tcataldo@gmail.com\r\n"));
			}).thenCompose(dataResp -> {
				return checkCode(dataResp, 503);
			});
		});
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
		return AbstractChainTest.class.getClassLoader().getResourceAsStream(path);
	}

	@Test
	public void testValidWithRealMail()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("recip@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, resourceBuffer("data/basic_2attachments.eml"));
			}).thenCompose(dataResp -> {
				return checkCode(dataResp, 250);
			});
		});
	}

	@Test
	public void testIncomingMailTriggersOverQuota()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("full@bm.lan");
			}).thenCompose(rcptResp -> {
				checkCode(rcptResp, 250);
				return client.data(1, resourceBuffer("data/basic_2attachments.eml"));
			}).thenCompose(dataResp -> {
				return checkCode(dataResp, 250);
			});
		});
	}

	@Test
	public void testValidSenderOverQuotaRecip()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("overq@bm.lan");
			}).thenCompose(rcptResp -> {
				return checkCode(rcptResp, 452);
			});
		});
	}

	@Test
	public void testValidSenderFuckedRecip()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client.lhlo("bm.lan").thenCompose(lhloResp -> {
				checkCode(lhloResp, 250);
				return client.mailFrom("sender@bm.lan");
			}).thenCompose(mailResp -> {
				checkCode(mailResp, 250);
				return client.rcptTo("fucked@bm.lan");
			}).thenCompose(rcptResp -> {
				return checkCode(rcptResp, 451);
			});
		});
	}

	@Test
	public void testBatchOfCommands()
			throws UnknownHostException, IOException, InterruptedException, ExecutionException, TimeoutException {
		withConnection(client -> {
			return client
					.batch(Request.lhlo("bm.lan"), Request.mailFrom("sender@bm.lan"), Request.rcptTo("recip@bm.lan"))
					.thenCompose(batchResult -> {
						CompletableFuture<?> chain = CompletableFuture.completedFuture(null);
						for (Response resp : batchResult) {
							chain = chain.thenCompose(v -> {
								return checkCode(resp, 250);
							});
						}
						return chain;
					});
		});
	}

}
