/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.metrics.core.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.mockito.Matchers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.metrics.alerts.api.AlertInfo;
import net.bluemind.metrics.alerts.api.AlertLevel;
import net.bluemind.metrics.core.service.MonitoringService;

public class GetAlertsTest {

	@Test
	public void testUnFilteredAllOk() throws Exception {

		AlertInfo info1 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.OK, "message1");
		AlertInfo info2 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.OK, "message2");
		AlertInfo info3 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name3",
				"datalocation3", "host3", AlertLevel.OK, "message3");
		List<AlertInfo> infos = Arrays.asList(info1, info2, info3);

		MonitoringService service = spy(new MonitoringService(null));
		doReturn(prepareResponse(infos).encode()).when(service).query(Matchers.anyString(), Matchers.anyInt());

		List<AlertInfo> collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.values()));
		assertEquals(3, collected.size());
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.OK));
		assertEquals(3, collected.size());
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.WARNING));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.CRITICAL));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.CRITICAL, AlertLevel.WARNING));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.OK, AlertLevel.WARNING));
		assertEquals(3, collected.size());
	}

	@Test
	public void testFilteredAllOk() throws Exception {

		AlertInfo info1 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.OK, "message1");
		AlertInfo info2 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.OK, "message2");
		AlertInfo info3 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name3",
				"datalocation3", "host3", AlertLevel.OK, "message3");
		List<AlertInfo> infos = Arrays.asList(info1, info2, info3);

		MonitoringService service = spy(new MonitoringService(null));
		doReturn(prepareResponse(infos).encode()).when(service).query(Matchers.anyString(), Matchers.anyInt());

		List<AlertInfo> collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.values()));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.OK));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.WARNING));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.CRITICAL));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.CRITICAL, AlertLevel.WARNING));
		assertEquals(0, collected.size());
		collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.OK, AlertLevel.WARNING));
		assertEquals(0, collected.size());
	}

	@Test
	public void testUnFilteredByLevel() throws Exception {

		AlertInfo info1 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.OK, "message1");
		AlertInfo info2 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.WARNING, "message2");
		AlertInfo info3 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name3",
				"datalocation3", "host3", AlertLevel.CRITICAL, "message3");
		List<AlertInfo> infos = Arrays.asList(info1, info2, info3);

		MonitoringService service = spy(new MonitoringService(null));
		doReturn(prepareResponse(infos).encode()).when(service).query(Matchers.anyString(), Matchers.anyInt());

		List<AlertInfo> collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.values()));
		assertEquals(3, collected.size());
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.OK));
		assertEquals(1, collected.size());
		assertEquals(info1.id, collected.get(0).id);
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.WARNING));
		assertEquals(1, collected.size());
		assertEquals(info2.id, collected.get(0).id);
		collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.CRITICAL));
		assertEquals(1, collected.size());
		assertEquals(info3.id, collected.get(0).id);

	}

	@Test
	public void testFiltered() throws Exception {

		// id1 CRITICAL -> OK -> WARNING -> OK -> CRITICAL
		AlertInfo id1_1 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.CRITICAL, "crit");
		AlertInfo id1_2 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.OK, "ok");
		AlertInfo id1_3 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.WARNING, "warn");
		AlertInfo id1_4 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.OK, "ok");
		AlertInfo id1_5 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id1", "name1",
				"datalocation1", "host1", AlertLevel.CRITICAL, "crit");

		// id2 OK -> WARNING -> OK -> WARNING
		AlertInfo id2_1 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.OK, "ok");
		AlertInfo id2_2 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.WARNING, "warn");
		AlertInfo id2_3 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.OK, "ok");
		AlertInfo id2_4 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id2", "name2",
				"datalocation2", "host2", AlertLevel.WARNING, "warn");

		// id3 WARNING -> OK -> CRITICAL
		AlertInfo id3_1 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id3", "name3",
				"datalocation3", "host3", AlertLevel.WARNING, "warn");
		AlertInfo id3_2 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id3", "name3",
				"datalocation3", "host3", AlertLevel.OK, "ok");
		AlertInfo id3_3 = createInfo(BmDateTimeWrapper.fromTimestamp(System.currentTimeMillis()), "id3", "name3",
				"datalocation3", "host3", AlertLevel.CRITICAL, "crit");

		List<AlertInfo> infos = Arrays.asList(id1_1, id1_2, id2_1, id3_1, id1_3, id2_2, id2_3, id2_4, id3_2, id3_3,
				id1_4, id1_5);

		MonitoringService service = spy(new MonitoringService(null));
		doReturn(prepareResponse(infos).encode()).when(service).query(Matchers.anyString(), Matchers.anyInt());

		List<AlertInfo> collected = service.collectAlerts("", 1000, false, Arrays.asList(AlertLevel.values()));
		assertEquals(12, collected.size());
		collected = service.collectAlerts("", 1000, true, Arrays.asList(AlertLevel.values()));
		assertEquals(2, collected.size());

		boolean found1 = false;
		boolean found3 = false;
		for (AlertInfo info : collected) {
			if (info.id.equals("id1") && info.level == AlertLevel.CRITICAL) {
				found1 = true;
			}
			if (info.id.equals("id3") && info.level == AlertLevel.WARNING) {
				found3 = true;
			}
		}

		assertTrue(found1);
		assertTrue(found3);
	}

	private JsonObject prepareResponse(List<AlertInfo> infos) {
		JsonObject ret = new JsonObject();
		JsonArray results = new JsonArray();
		ret.put("results", results);
		JsonObject result1 = new JsonObject();
		results.add(result1);
		JsonArray series = new JsonArray();
		result1.put("series", series);
		JsonObject value = new JsonObject();
		series.add(value);
		JsonArray values = new JsonArray();
		value.put("values", values);

		for (AlertInfo info : infos) {
			JsonArray row = new JsonArray();
			row.add(info.time.iso8601).add(info.id).add(info.name).add(info.datalocation).add(info.host)
					.add(info.level.name()).add(info.message);
			values.add(row);
		}
		return ret;
	}

	private AlertInfo createInfo(BmDateTime time, String id, String name, String datalocation, String host,
			AlertLevel level, String message) {
		AlertInfo info = new AlertInfo();
		info.time = time;
		info.id = id;
		info.name = name;
		info.datalocation = datalocation;
		info.host = host;
		info.level = level;
		info.message = message;
		return info;
	}

}
