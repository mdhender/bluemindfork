package net.bluemind.core.serialization;
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

import java.io.File;
import java.nio.file.Path;

import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;

import io.vertx.core.json.JsonObject;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;

public class HzHollowAnnouncer extends HollowFilesystemAnnouncer implements HollowProducer.Announcer {
	public final String dataset;

	public HzHollowAnnouncer(String dataset, File publishDir) {
		super(publishDir);
		this.dataset = dataset;
	}

	public HzHollowAnnouncer(String dataset, Path publishPath) {
		super(publishPath);
		this.dataset = dataset;
	}

	@Override
	public void announce(long stateVersion) {
		super.announce(stateVersion);
		JsonObject data = new JsonObject();
		data.put("action", "version_announcement");
		data.put("dataset", dataset);
		data.put("version", stateVersion);
		MQ.getProducer(Topic.DATA_SERIALIZATION_NOTIFICATIONS).send(data);
	}

}
