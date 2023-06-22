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
package net.bluemind.cloud.monitoring.server.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.bluemind.system.application.registration.model.ApplicationInfoModel;

public class NodeInfo {

	public NodeType type;
	public String forestId;
	public long timestamp;

	public final ApplicationInfoModel info;

	public NodeInfo(ApplicationInfoModel info) {
		this.info = info;
	}

	@Override
	public String toString() {
		String msg = "[Node type = %s, forestId = %s, info = (%s)]";
		return String.format(msg, type, forestId, info.toString());
	}

	@Override
	public int hashCode() {
		return info.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeInfo other = (NodeInfo) obj;
		if (info == null) {
			if (other.info != null)
				return false;
		} else if (!info.equals(other.info))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (forestId == null) {
			if (other.forestId != null)
				return false;
		} else if (!forestId.equals(other.forestId))
			return false;
		return true;
	}

	public String id() {
		return "" + hashCode();
	}

	@JsonIgnore
	public boolean isMaster() {
		return NodeType.MASTER == type;
	}

	@JsonIgnore
	public boolean isFork() {
		return NodeType.FORK == type;
	}

	@JsonIgnore
	public boolean isPrimaryNode() {
		return isMaster() || isFork();
	}

	@JsonIgnore
	public boolean isCrp() {
		return NodeType.CRP == type;
	}

	@JsonIgnore
	public boolean isTail() {
		return NodeType.TAIL == type;
	}

	@JsonIgnore
	public boolean isCloningState() {
		return "CORE_STATE_CLONING".equals(info.state.state);
	}

	@JsonIgnore
	public boolean isNotInstalledStateOrNoId() {
		return "CORE_STATE_NOT_INSTALLED".equals(info.state.state) || "bluemind-noid".equals(info.installationId);
	}

}
