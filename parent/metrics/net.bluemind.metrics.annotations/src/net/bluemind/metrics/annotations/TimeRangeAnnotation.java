/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.annotations;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;

public class TimeRangeAnnotation {

	private TimeRangeAnnotation() {

	}

	public static void annotate(String name, Date start, Optional<Date> optEnd, Map<String, String> tags) {
		Objects.requireNonNull(name, "name is null");
		Objects.requireNonNull(start, "date is null");
		Date end = optEnd.orElse(start);

		JsonObject tagsJs = new JsonObject();
		tags.forEach(tagsJs::put);
		JsonObject js = new JsonObject().put("name", name).put("start", start.getTime()).put("end", end.getTime())
				.put("tags", tagsJs);
		VertxPlatform.eventBus().publish("metrics.range.annotate", js);
	}

}
