/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.forest.cloud.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Required;

@BMApi(version = "3")
public class ForestTopology {

	public static class KafkaListener {
		/**
		 * PLAINTEXT://172.16.2.4:9092
		 */
		public String address;

		public static KafkaListener of(String addr) {
			KafkaListener kl = new KafkaListener();
			kl.address = addr;
			return kl;
		}
	};

	@Required
	public KafkaListener broker;

}
