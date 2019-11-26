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
package net.bluemind.calendar.service;

import java.util.Optional;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.persistence.IWeightProvider;
import net.bluemind.core.container.service.internal.ContainerStoreService.IWeightSeedProvider;

public class VEventWeight {

	private static final IWeightSeedProvider<VEventSeries> prov = eventSerie -> {
		VEvent event = Optional.ofNullable(eventSerie.main).orElseGet(() -> eventSerie.occurrences.get(0));

		if (event.rrule != null) {
			if (event.rrule.until != null) {
				return BmDateTimeWrapper.toTimestamp(event.rrule.until.iso8601, "UTC");
			}
			return Long.MAX_VALUE;
		} else {
			if (event.dtend != null) {
				return BmDateTimeWrapper.toTimestamp(event.dtend.iso8601, "UTC");
			}
			return 0L;
		}
	};

	private static final IWeightProvider wProv = seed -> {
		return Long.MAX_VALUE - Math.abs(System.currentTimeMillis() - seed);
	};

	public static IWeightSeedProvider<VEventSeries> seedProvider() {
		return prov;
	}

	public static IWeightProvider weigthProvider() {
		return wProv;
	}

}
