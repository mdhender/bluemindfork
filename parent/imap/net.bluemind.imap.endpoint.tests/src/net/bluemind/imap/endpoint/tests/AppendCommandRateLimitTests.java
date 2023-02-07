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
package net.bluemind.imap.endpoint.tests;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPRuntimeException;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.endpoint.EndpointConfig;
import net.bluemind.imap.endpoint.ratelimiter.ThroughputLimiterRegistry;
import net.bluemind.imap.endpoint.ratelimiter.ThroughputLimiterRegistry.Strategy;
import net.bluemind.imap.endpoint.tests.driver.MockConnection;
import net.bluemind.imap.endpoint.tests.driver.MockMailboxDriver;
import net.bluemind.imap.endpoint.tests.driver.MockModel;
import net.bluemind.tests.extensions.WithVertxExtension;

@ExtendWith(WithVertxExtension.class)
public class AppendCommandRateLimitTests {
	private static final Logger logger = LoggerFactory.getLogger(AppendCommandRateLimitTests.class);

	private static final int INITIAL_CAPACITY = 200;
	private static final int REFILL_CAPACITY = 10;
	private static final int REFILL_PERIOD = 100;

	private InputStream literal23x(int repeat) {
		return new ByteArrayInputStream("From: gg123@gmail.com\r\n".repeat(repeat).getBytes());
	}

	private double emlSize(int repeat) {
		return 23d * repeat;
	}

	private double appendCommandSize(int repeat) {
		return 27d + emlSize(repeat);
	}

	private int port;
	private MockModel mdl;

	@BeforeEach
	public void beforeEach() throws Exception {
		this.port = EndpointConfig.get().getInt("imap.port");
		this.mdl = MockModel.INSTANCE;
		this.mdl.registerFolder(UUID.randomUUID(), "INBOX");

		MockMailboxDriver.maxLiteralSize = INITIAL_CAPACITY;
		MockConnection.maxLiteralSize = INITIAL_CAPACITY;
	}

	@AfterEach
	public void afterEach() throws Exception {
		ThroughputLimiterRegistry.clear();
	}

	@Nested
	@DisplayName("Check NOOP rate limiter")
	class NoopStrategyTest {

		@BeforeAll
		public static void beforeClass() {
			System.setProperty("imap.throughput.strategy", Strategy.NONE.name());
			System.setProperty("imap.throughput.capacity", REFILL_CAPACITY + "b");
			System.setProperty("imap.throughput.period", REFILL_PERIOD + "ms");
			EndpointConfig.reload();
		}

		@Test
		@DisplayName("Noop rate limiter don't enforce limits")
		public void testThroughtputUnlimitatedWithEmlOfMaxLiteralSize() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				long startAt = System.currentTimeMillis();
				int numberOfAppend = 5;
				for (int i = 0; i < numberOfAppend; i++) {
					long beforeAppend = System.currentTimeMillis();
					sc.append("INBOX", literal23x(5), new FlagsList());
					long duration = System.currentTimeMillis() - beforeAppend;
					assertThat(duration).isLessThan(REFILL_PERIOD);
				}
			}
		}
	}

	@Nested
	@DisplayName("Check INTIME rate limiter with bucket Strategy")
	class InTimeStrategyTest {

		@BeforeAll
		public static void beforeClass() {
			System.setProperty("imap.chunk-size", "50b");
			System.setProperty("imap.throughput.strategy", Strategy.INTIME.name());
			System.setProperty("imap.throughput.capacity", REFILL_CAPACITY + "b");
			System.setProperty("imap.throughput.period", REFILL_PERIOD + "ms");
			EndpointConfig.reload();
		}

		@Test
		@DisplayName("Should not rate limit when commands literal are send below the configured througput")
		public void testUnderLimit() throws InterruptedException {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				int allowedBeforeLimit = (int) (INITIAL_CAPACITY / appendCommandSize(1));
				double timeToRefill = (appendCommandSize(1) / REFILL_CAPACITY) * REFILL_PERIOD;
				int numberOfAppend = allowedBeforeLimit + 5;
				for (int i = 0; i < numberOfAppend; i++) {
					long beforeAppend = System.currentTimeMillis();
					sc.append("INBOX", literal23x(1), new FlagsList());
					long duration = System.currentTimeMillis() - beforeAppend;
					assertThat(duration).isLessThan(REFILL_PERIOD);
					Thread.sleep((int) Math.ceil(timeToRefill));
				}
			}
		}

		@Test
		@DisplayName("Should rate limit when commands with literal below 'imap.chunk-size' are send above the configured througput")
		public void testThroughtputLimitationWithEmlOf20bytes() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				int allowedBeforeLimit = (int) (INITIAL_CAPACITY / appendCommandSize(1));
				double timeToRefill = (appendCommandSize(1) / REFILL_CAPACITY) * REFILL_PERIOD;
				int numberOfAppend = allowedBeforeLimit + 5;
				long startAt = System.currentTimeMillis();
				for (int i = 0; i < numberOfAppend; i++) {
					long beforeAppend = System.currentTimeMillis();
					logger.info("BEFORE APPEND");
					sc.append("INBOX", literal23x(1), new FlagsList());
					double duration = System.currentTimeMillis() - beforeAppend;
					if (i < allowedBeforeLimit) {
						// we haven't reaches the limit yet, we accept
						assertThat(duration).isAtMost(REFILL_PERIOD);
					} else {
						// last ones have to wait for timeToRefill
						assertThat(duration).isWithin(timeToRefill * 0.05).of(timeToRefill);
					}
				}
				long duration = System.currentTimeMillis() - startAt;
				double totalSend = numberOfAppend * appendCommandSize(1);
				double expectedThroughput = REFILL_CAPACITY / (double) REFILL_PERIOD;
				double throughput = (totalSend - INITIAL_CAPACITY) / (double) duration;
				assertThat(throughput).isWithin(0.001).of(expectedThroughput);
			}
		}

		@Test
		@DisplayName("Should rate limit when commands with literal above 'imap.chunk-size' are send above the configured througput")
		public void testThroughtputLimitationWithEmlOfMaxLiteralSize() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				long startAt = System.currentTimeMillis();
				int numberOfAppend = 10;
				for (int i = 0; i < numberOfAppend; i++) {
					logger.info("BEFORE APPEND");
					sc.append("INBOX", literal23x(5), new FlagsList());
				}
				long duration = System.currentTimeMillis() - startAt;
				double totalSend = numberOfAppend * appendCommandSize(5);
				double expectedThroughput = REFILL_CAPACITY / (double) REFILL_PERIOD;
				double throughput = (totalSend - INITIAL_CAPACITY) / (double) duration;
				assertThat(throughput).isWithin(0.001).of(expectedThroughput);
			}
		}

		@Test
		@DisplayName("Should not rate limit when literal are oversized")
		public void testAboveMaxMessageSize() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();
				long startAt = System.currentTimeMillis();
				try {
					int uid = sc.append("INBOX", literal23x(10), new FlagsList());
					// we have either that:
					assertThat(uid).isEqualTo(-1);
				} catch (IMAPRuntimeException e) {
					// or that client side (socket is closed)
					logger.error("Got IMAPRuntimeException: {}", e.getMessage());
				}
				long duration = System.currentTimeMillis() - startAt;
				// eyther way, it happens with no rate limiting:
				assertThat(duration).isLessThan(REFILL_PERIOD);
			}
		}

		@Test
		@DisplayName("Should process command in order even after during a rate limit")
		public void testTwoClientsOrderingWithTwoSockets() throws InterruptedException, ExecutionException {
			Queue<Message> accepted = new ConcurrentLinkedQueue<>();
			Client cl1 = new Client(port, accepted, new Message(1, Duration.ofMillis(0)), //
					new Message(1, Duration.ofMillis(300)), //
					new Message(1, Duration.ofMillis(700)));
			Client cl2 = new Client(port, accepted, new Message(7, Duration.ofMillis(200)));

			double totalSize = 3 * appendCommandSize(1) + appendCommandSize(7);

			new Thread(cl1).start();
			new Thread(cl2).start();

			long startAt = System.currentTimeMillis();
			CompletableFuture.allOf(cl1.futureEnd, cl2.futureEnd).get();
			long duration = System.currentTimeMillis() - startAt;
			double expectedDuration = (totalSize - INITIAL_CAPACITY) * REFILL_PERIOD / REFILL_CAPACITY;
			assertThat((double) duration).isWithin(expectedDuration * 0.06).of(expectedDuration);

			accepted.stream().reduce((m1, m2) -> {
				assertThat(m1.initialDelay).isLessThan(m2.initialDelay);
				return m2;
			});
		}

		@Test
		@DisplayName("Should stop the socket during a rate limit")
		public void testTwoClientsOrderingWithOneSocket() throws InterruptedException, ExecutionException {
			StoreClient client = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom");
			client.login();
			Queue<Message> accepted = new ConcurrentLinkedQueue<>();
			Client cl1 = new Client(client, accepted, new Message(1, Duration.ofMillis(0)), //
					new Message(1, Duration.ofMillis(300)), //
					new Message(1, Duration.ofMillis(700)));
			Client cl2 = new Client(client, accepted, new Message(7, Duration.ofMillis(200)));

			double totalSize = 3 * appendCommandSize(1) + appendCommandSize(7);

			new Thread(cl1).start();
			new Thread(cl2).start();

			long startAt = System.currentTimeMillis();
			CompletableFuture.allOf(cl1.futureEnd, cl2.futureEnd).get();
			long duration = System.currentTimeMillis() - startAt;
			double expectedDuration = (totalSize - INITIAL_CAPACITY) * REFILL_PERIOD / REFILL_CAPACITY;
			assertThat((double) duration).isWithin(expectedDuration * 0.06).of(expectedDuration);

			accepted.stream().reduce((m1, m2) -> {
				assertThat(m1.initialDelay).isLessThan(m2.initialDelay);
				return m2;
			});
		}
	}

	@Nested
	@DisplayName("Check BEHIND rate limiter with bucket impl")
	class BehindStrategyTest {

		@BeforeAll
		public static void beforeClass() {
			System.setProperty("imap.chunk-size", "50b");
			System.setProperty("imap.throughput.strategy", Strategy.BEHIND.name());
			System.setProperty("imap.throughput.capacity", REFILL_CAPACITY + "b");
			System.setProperty("imap.throughput.period", REFILL_PERIOD + "ms");
			EndpointConfig.reload();
		}

		@Test
		@DisplayName("Should not rate limit when commands literal are send below the configured througput")
		public void testUnderLimit() throws InterruptedException {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				int allowedBeforeLimit = (int) (INITIAL_CAPACITY / appendCommandSize(1));
				int numberOfAppend = allowedBeforeLimit + 5;
				for (int i = 0; i < numberOfAppend; i++) {
					long beforeAppend = System.currentTimeMillis();
					sc.append("INBOX", literal23x(1), new FlagsList());
					long duration = System.currentTimeMillis() - beforeAppend;
					assertThat(duration).isLessThan(REFILL_PERIOD);
					double timeToRefill = (appendCommandSize(1) / REFILL_CAPACITY) * REFILL_PERIOD;
					Thread.sleep((int) Math.ceil(timeToRefill));
				}
			}
		}

		@Test
		@DisplayName("Should rate limit when commands with literal below 'imap.chunk-size' are send above the configured througput")
		public void testThroughtputLimitationWithEmlOf20bytes() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				int allowedBeforeLimit = (int) (INITIAL_CAPACITY / appendCommandSize(1));
				double timeToRefill = (appendCommandSize(1) / REFILL_CAPACITY) * REFILL_PERIOD;
				int numberOfAppend = allowedBeforeLimit + 5;
				long startAt = System.currentTimeMillis();
				for (int i = 0; i < numberOfAppend; i++) {
					long beforeAppend = System.currentTimeMillis();
					logger.info("BEFORE APPEND");
					sc.append("INBOX", literal23x(1), new FlagsList());
					double duration = System.currentTimeMillis() - beforeAppend;
					if (i < allowedBeforeLimit) {
						// we haven't reaches the limit yet, we accept
						assertThat(duration).isAtMost(REFILL_PERIOD);
					} else if (i == allowedBeforeLimit) {
						// we are one the limit, depending on how the buffer is splitted, some might be
						// limited
						assertThat(duration).isAtMost(timeToRefill);
					} else {
						// depending on how the buffer is splitted and how the previous one was splitted
						// last ones might wait less than the timeToRefill
						assertThat(duration).isAtLeast(timeToRefill * 0.49);
						// or more than the time to refill
						assertThat(duration).isAtMost(timeToRefill * 1.51);
					}
				}
				double duration = System.currentTimeMillis() - startAt;

				double totalSend = numberOfAppend * appendCommandSize(1);
				double lowExpectedDuration = (totalSend - appendCommandSize(1) - INITIAL_CAPACITY) * REFILL_PERIOD
						/ REFILL_CAPACITY;
				double highExpectedDuration = (totalSend - INITIAL_CAPACITY) * REFILL_PERIOD / REFILL_CAPACITY;
				assertThat(duration).isAtLeast(lowExpectedDuration * 0.9);
				assertThat(duration).isAtMost(highExpectedDuration * 1.1);
			}
		}

		@Test
		@DisplayName("Should rate limit when commands with literal above 'imap.chunk-size' are send above the configured througput")
		public void testThroughtputLimitationWithEmlOfMaxLiteralSize() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();

				long startAt = System.currentTimeMillis();
				int numberOfAppend = 10;
				double timeToRefill = (appendCommandSize(5) / REFILL_CAPACITY) * REFILL_PERIOD;
				for (int i = 0; i < numberOfAppend; i++) {
					logger.info("BEFORE APPEND");
					sc.append("INBOX", literal23x(5), new FlagsList());
				}
				double duration = System.currentTimeMillis() - startAt;

				double totalSend = numberOfAppend * appendCommandSize(5);
				double lowExpectedDuration = (totalSend - appendCommandSize(5) - INITIAL_CAPACITY) * REFILL_PERIOD
						/ REFILL_CAPACITY;
				double highExpectedDuration = (totalSend - INITIAL_CAPACITY) * REFILL_PERIOD / REFILL_CAPACITY;
				assertThat(duration).isAtLeast(lowExpectedDuration * 0.9);
				assertThat(duration).isAtMost(highExpectedDuration * 1.1);
			}
		}

		@Test
		@DisplayName("Should not rate limit when literal are oversized")
		public void testAboveMaxMessageSize() {
			try (StoreClient sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom")) {
				assertThat(sc.login()).isTrue();
				long startAt = System.currentTimeMillis();
				try {
					int uid = sc.append("INBOX", literal23x(10), new FlagsList());
					// we have either that:
					assertThat(uid).isEqualTo(-1);
				} catch (IMAPRuntimeException e) {
					// or that client side (socket is closed)
					logger.error("Got IMAPRuntimeException: {}", e.getMessage());
				}
				long duration = System.currentTimeMillis() - startAt;
				// eyther way, it happens with no rate limiting:
				assertThat(duration).isLessThan(REFILL_PERIOD);
			}
		}

		@Test
		@DisplayName("Should process command in order even after during a rate limit")
		public void testTwoClientsOrderingWithTwoSockets() throws InterruptedException, ExecutionException {
			Queue<Message> accepted = new ConcurrentLinkedQueue<>();
			Client cl1 = new Client(port, accepted, new Message(1, Duration.ofMillis(0)), //
					new Message(1, Duration.ofMillis(300)), //
					new Message(1, Duration.ofMillis(700)));
			Client cl2 = new Client(port, accepted, new Message(7, Duration.ofMillis(200)));

			double totalSize = ((3 - 0.5) * appendCommandSize(1)) + appendCommandSize(7);

			new Thread(cl1).start();
			new Thread(cl2).start();

			long startAt = System.currentTimeMillis();
			CompletableFuture.allOf(cl1.futureEnd, cl2.futureEnd).get();
			double duration = System.currentTimeMillis() - startAt;

			double lowExpectedDuration = (totalSize - appendCommandSize(1) - INITIAL_CAPACITY) * REFILL_PERIOD
					/ REFILL_CAPACITY;
			double highExpectedDuration = (totalSize - INITIAL_CAPACITY) * REFILL_PERIOD / REFILL_CAPACITY;
			assertThat(duration).isAtLeast(lowExpectedDuration * 0.9);
			assertThat(duration).isAtMost(highExpectedDuration * 1.1);

			accepted.stream().reduce((m1, m2) -> {
				assertThat(m1.initialDelay).isLessThan(m2.initialDelay);
				return m2;
			});
		}

		@Test
		@DisplayName("Should stop the socket during a rate limit")
		public void testTwoClientsOrderingWithOneSocket() throws InterruptedException, ExecutionException {
			StoreClient client = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom");
			client.login();
			Queue<Message> accepted = new ConcurrentLinkedQueue<>();
			Client cl1 = new Client(client, accepted, new Message(1, Duration.ofMillis(0)), //
					new Message(1, Duration.ofMillis(300)), //
					new Message(1, Duration.ofMillis(700)));
			Client cl2 = new Client(client, accepted, new Message(7, Duration.ofMillis(200)));

			double totalSize = (3 * appendCommandSize(1)) + appendCommandSize(7);

			new Thread(cl1).start();
			new Thread(cl2).start();

			long startAt = System.currentTimeMillis();
			CompletableFuture.allOf(cl1.futureEnd, cl2.futureEnd).get();
			double duration = System.currentTimeMillis() - startAt;

			double lowExpectedDuration = (totalSize - appendCommandSize(1) - INITIAL_CAPACITY) * REFILL_PERIOD
					/ REFILL_CAPACITY;
			double highExpectedDuration = (totalSize - INITIAL_CAPACITY) * REFILL_PERIOD / REFILL_CAPACITY;
			assertThat(duration).isAtLeast(lowExpectedDuration * 0.9);
			assertThat(duration).isAtMost(highExpectedDuration * 1.1);

			accepted.stream().reduce((m1, m2) -> {
				assertThat(m1.initialDelay).isLessThan(m2.initialDelay);
				return m2;
			});
		}
	}

	public class Client implements Runnable {
		private final Queue<Message> accepted;
		private final Message[] messages;
		private final DelayQueue<Message> queue;
		private final StoreClient sc;
		public final CompletableFuture<Void> futureEnd;

		public Client(int port, Queue<Message> accepted, Message... messages) {
			this.accepted = accepted;
			this.messages = messages;
			this.queue = new DelayQueue<>();
			this.sc = new StoreClient("127.0.0.1", port, "tom@devenv.blue", "tom");
			this.futureEnd = new CompletableFuture<>();
			sc.login();
		}

		public Client(StoreClient sc, Queue<Message> accepted, Message... messages) {
			this.accepted = accepted;
			this.messages = messages;
			this.queue = new DelayQueue<>();
			this.sc = sc;
			this.futureEnd = new CompletableFuture<>();
		}

		public void run() {
			Stream.of(messages).forEach(message -> queue.add(message.copy()));
			while (true) {
				Message message = queue.poll();
				if (message == null && queue.size() == 0) {
					futureEnd.complete(null);
					return;
				} else if (message == null) {
					continue;
				}

				logger.info("BEFORE APPEND: {}", message.initialDelay.toMillis());
				sc.append("INBOX", literal23x(message.size), new FlagsList());
				accepted.add(message);
			}
		}
	}

	public class Message implements Delayed {

		private final int size;
		private final Duration initialDelay;
		private final long startTime;

		public Message(int size, Duration delay) {
			this.size = size;
			this.initialDelay = delay;
			this.startTime = System.currentTimeMillis() + delay.toMillis();
		}

		private int size() {
			return size;
		}

		private Message copy() {
			return new Message(size, initialDelay);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			long diff = startTime - System.currentTimeMillis();
			return unit.convert(diff, TimeUnit.MILLISECONDS);
		}

		@Override
		public int compareTo(Delayed o) {
			return Ints.saturatedCast(this.startTime - ((Message) o).startTime);
		}

	}

}
