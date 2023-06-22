/* BEGIN LICENSE
* Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.application.registration.model;

import java.util.HashSet;
import java.util.Set;

import net.bluemind.core.utils.JsonUtils;

public class ApplicationInfoModel {

	public String product;
	public String address;
	public String machineId;
	public String installationId;
	public ApplicationState state;
	public Set<ApplicationMetric> metrics;

	public ApplicationInfoModel() {
	}

	public ApplicationInfoModel(String product, String address, String machineId, String installationId) {
		this.product = product;
		this.address = address;
		this.machineId = machineId;
		this.installationId = installationId;
		this.metrics = new HashSet<>();
		this.state = new ApplicationState();
	}

	public String toJson() {
		return JsonUtils.asString(this);
	}

	@Override
	public String toString() {
		String msg = "[Info product = %s, address = %s, machine = %s, installation = %s]";
		return String.format(msg, product, address, machineId, installationId);
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
		ApplicationInfoModel other = (ApplicationInfoModel) obj;
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
