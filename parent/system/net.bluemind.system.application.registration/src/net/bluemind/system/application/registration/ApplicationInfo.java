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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.application.registration;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.VertxPlatform;

public class ApplicationInfo {

	public String product;
	public String address;
	public String machineId;
	public String state;
	public String installationId;
	public String version;

	public ApplicationInfo() {

	}

	public ApplicationInfo(String product, String address, String machineId, String installationId) {
		this.product = product;
		this.address = address;
		this.machineId = machineId;
		this.installationId = installationId;
	}

	public static void register(ApplicationInfo info, Supplier<String> state, Supplier<String> version) {
		Store store = new Store(info.product + info.machineId);
		if (store.isEnabled()) {
			info.state = state.get();
			info.version = version.get();
			VertxPlatform.getVertx().eventBus().send(ApplicationRegistration.APPLICATION_REGISTRATION,
					JsonUtils.asString(info));
			VertxPlatform.getVertx().setPeriodic(TimeUnit.HOURS.toMillis(1), (id) -> {
				info.state = state.get();
				info.version = version.get();
				VertxPlatform.getVertx().eventBus().publish(ApplicationRegistration.APPLICATION_REGISTRATION, info);
			});
		}
	}

	public static void update(ApplicationInfo info) {
		VertxPlatform.getVertx().eventBus().publish(ApplicationRegistration.APPLICATION_REGISTRATION, info);
	}

	public String toJson() {
		return JsonUtils.asString(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((address == null) ? 0 : address.hashCode());
		result = prime * result + ((installationId == null) ? 0 : installationId.hashCode());
		result = prime * result + ((machineId == null) ? 0 : machineId.hashCode());
		result = prime * result + ((product == null) ? 0 : product.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationInfo other = (ApplicationInfo) obj;
		if (address == null) {
			if (other.address != null)
				return false;
		} else if (!address.equals(other.address))
			return false;
		if (installationId == null) {
			if (other.installationId != null)
				return false;
		} else if (!installationId.equals(other.installationId))
			return false;
		if (machineId == null) {
			if (other.machineId != null)
				return false;
		} else if (!machineId.equals(other.machineId))
			return false;
		if (product == null) {
			if (other.product != null)
				return false;
		} else if (!product.equals(other.product))
			return false;
		return true;
	}

}
