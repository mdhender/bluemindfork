package net.bluemind.metrics.registry.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Tag;
import com.netflix.spectator.api.patterns.PolledMeter;

import net.bluemind.metrics.registry.MetricsRegistry;

public class BMRegistryTests {

	@Test
	public void testCounters() throws Exception {
		Registry registry = MetricsRegistry.get();
		assertTrue(MetricsRegistry.isAvailable());
		Id requests = registry.createId("server.requests");
		for (int i = 0; i < 50; i++) {
			boolean even = i % 2 == 0;
			Id refinedId = requests.withTag("status", even ? "ok" : "ko");
			registry.counter(refinedId).increment();
		}
		registry.counter("test.amount").increment();
		registry.counter("test.amount").increment(12);
		registry.counter("test.amount").increment(100);
		Thread.sleep(2000);
		String metrics = MetricsRegistry.getMetrics().get(10, TimeUnit.SECONDS);
		int testedMetrics = 0;
		for (String metric : metrics.split("\n")) {
			InfluxMetric met = InfluxMetric.fromLineProtocol(metric);
			if ("server.requests".equals(met.name)) {
				// To verify that the tag is created
				for (Tag tag : met.tags) {
					if ("status".equals(tag.key())) {
						testedMetrics += 1;
						assertEquals(25L, met.values.get("count").longValue());
						break;
					}
				}
			} else if ("test.amount".equals(met.name)) {
				assertEquals(113, met.values.get("count").longValue());
				testedMetrics += 1;
			}
		}
		assertEquals(3, testedMetrics);
	}

	@Test
	public void testDistSum() throws Exception {
		Registry registry = MetricsRegistry.get();
		assertTrue(MetricsRegistry.isAvailable());
		Id requests = registry.createId("server.requestsizes");
		for (int i = 0; i < 50; i++) {
			boolean even = i % 2 == 0;
			Id refinedId = requests.withTag("status", even ? "ok" : "ko");
			registry.distributionSummary(refinedId).record(even ? 12 : 20);
			registry.distributionSummary(refinedId).totalAmount();
		}
		registry.distributionSummary(requests.withTag("status", "ok")).record(-312213);
		registry.distributionSummary(requests.withTag("status", "ko")).record(-123);
		Thread.sleep(2000);
		String metrics = MetricsRegistry.getMetrics().get(10, TimeUnit.SECONDS);

		int testedMetrics = 0;
		for (String metric : metrics.split("\n")) {
			InfluxMetric met = InfluxMetric.fromLineProtocol(metric);
			if ("server.requestsizes".equals(met.name)) {
				for (Tag tag : met.tags) {
					if ("status".equals(tag.key()) && "ko".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(377L, met.values.get("totalAmount").longValue());
						break;
					} else if ("status".equals(tag.key()) && "ok".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(-311913L, met.values.get("totalAmount").longValue());
						break;
					}
				}
			}
		}
		assertEquals(2, testedMetrics);
	}

	@Test
	public void testTimers() throws Exception {
		Registry registry = MetricsRegistry.get();
		assertTrue(MetricsRegistry.isAvailable());
		Id requests = registry.createId("server.requesttimes");
		Id id1 = requests.withTag("status", "ok");
		Id id2 = requests.withTag("status", "ko");
		registry.timer(id1).record(2000, TimeUnit.MILLISECONDS);
		registry.timer(id2).record(4000, TimeUnit.MILLISECONDS);
		registry.timer(id1).record(300, TimeUnit.MILLISECONDS);
		registry.timer(id1).record(1, TimeUnit.NANOSECONDS);
		Thread.sleep(2000);
		String metrics = MetricsRegistry.getMetrics().get(10, TimeUnit.SECONDS);

		int testedMetrics = 0;
		for (String metric : metrics.split("\n")) {
			InfluxMetric met = InfluxMetric.fromLineProtocol(metric);
			if ("server.requesttimes".equals(met.name)) {
				for (Tag tag : met.tags) {
					if ("status".equals(tag.key()) && "ko".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(4000000000L, met.values.get("totalTime").longValue());
						break;
					} else if ("status".equals(tag.key()) && "ok".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(2300000001L, met.values.get("totalTime").longValue());
						break;
					}
				}
			}
		}
		assertEquals(2, testedMetrics);
	}

	@Test
	public void testGauges() throws Exception {
		Registry registry = MetricsRegistry.get();
		assertTrue(MetricsRegistry.isAvailable());
		AtomicInteger polledValue = PolledMeter.using(registry).withName("test.gauge")
				.monitorValue(new AtomicInteger(0));
		polledValue.incrementAndGet();
		polledValue.addAndGet(32);
		PolledMeter.update(registry);
		Id id = registry.createId("server.gauge");
		registry.gauge(id).set(123.456);
		registry.gauge(id.withTag("a", "b")).set(-99.1);
		registry.gauge(id.withTag("a", "c")).set(1232);
		registry.gauge(id.withTag("toto", "tata")).set(98.9875);
		registry.gauge("qwerty").set(666.666);
		registry.gauge("server.gauge").set(4269.111);
		Thread.sleep(2000);
		String metrics = MetricsRegistry.getMetrics().get(10, TimeUnit.SECONDS);
		int testedMetrics = 0;
		for (String metric : metrics.split("\n")) {
			InfluxMetric met = InfluxMetric.fromLineProtocol(metric);
			if ("server.gauge".equals(met.name)) {
				for (Tag tag : met.tags) {
					if ("a".equals(tag.key()) && "b".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(-99L, met.values.get("value").longValue());
						break;
					} else if ("a".equals(tag.key()) && "c".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(1232L, met.values.get("value").longValue());
						break;
					} else if ("toto".equals(tag.key()) && "tata".equals(tag.value())) {
						testedMetrics += 1;
						assertEquals(99L, met.values.get("value").longValue());
						break;
					}
				}
			} else if ("test.gauge".equals(met.name)) {
				assertEquals(33L, met.values.get("value").longValue());
				testedMetrics += 1;
			}
		}
		assertEquals(4, testedMetrics);

	}
}
