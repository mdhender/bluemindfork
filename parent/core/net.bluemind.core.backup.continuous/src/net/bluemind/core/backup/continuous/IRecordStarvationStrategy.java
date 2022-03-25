/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public interface IRecordStarvationStrategy {

	public enum ExpectedBehaviour {

		/**
		 * Try consuming again (aka run round of consume loop)
		 */
		RETRY,

		/**
		 * Stop consuming record (aka break the consume loop)
		 */
		ABORT
	}

	ExpectedBehaviour onStarvation(JsonObject infos);

	default void onRecordsReceived(@SuppressWarnings("unused") JsonObject metas) {

	}

	/**
	 * Override this to save intermediate states while cloning
	 * 
	 * @param state
	 */
	default void checkpoint(@SuppressWarnings("unused") String topicName,
			@SuppressWarnings("unused") IResumeToken state) {

	}

}
