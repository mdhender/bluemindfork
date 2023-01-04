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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.api;

import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.BaseContainerDescriptor;

public interface IBackupStoreFactory {

	InstallationWriteLeader leadership();

	<T> IBackupStore<T> forContainer(BaseContainerDescriptor c);

	default void pause() {
		System.setProperty(CloneDefaults.DISABLE_SYSPROP, "true");
		LoggerFactory.getLogger(IBackupStoreFactory.class).info("{} PAUSED.", this);
	}

	default boolean isPaused() {
		return "true".equals(System.getProperty(CloneDefaults.DISABLE_SYSPROP, "xxx"));
	}

	default void resume() {
		System.clearProperty(CloneDefaults.DISABLE_SYSPROP);
		LoggerFactory.getLogger(IBackupStoreFactory.class).info("{} RESUMED.", this);
	}

}
