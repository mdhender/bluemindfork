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
package net.bluemind.cloud.monitoring.server.zk;

public class ZkNode {

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + ((forestId == null) ? 0 : forestId.hashCode());
		result = prime * result + ((installationId == null) ? 0 : installationId.hashCode());
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
		ZkNode other = (ZkNode) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (forestId == null) {
			if (other.forestId != null)
				return false;
		} else if (!forestId.equals(other.forestId))
			return false;
		if (installationId == null) {
			if (other.installationId != null)
				return false;
		} else if (!installationId.equals(other.installationId))
			return false;
		return true;
	}

	public final String base;
	public final String forestId;
	public final String installationId;

	public ZkNode(String base, String forestId, String installationId) {
		this.base = base;
		this.forestId = forestId;
		this.installationId = installationId;
	}

}
