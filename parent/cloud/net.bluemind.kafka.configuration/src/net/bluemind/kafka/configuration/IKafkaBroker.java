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
package net.bluemind.kafka.configuration;

public interface IKafkaBroker {

	/**
	 * @return kafka.devenv.blue
	 */
	public String inspectAddress();

	default int port() {
		return 9093;
	}

	/**
	 * @return plaintext://kafka.devenv.blue:9093
	 */
	default String kafkaListener() {
		return "PLAINTEXT://" + inspectAddress() + ":" + port();
	}

	default int defaultPartitions() {
		return 5;
	}

	default int maxReplicas() {
		return 1;
	}

}
