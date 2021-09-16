package net.bluemind.central.reverse.proxy.model.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class InstallationInfo {

	public final String dataLocation;
	public final String ip;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public InstallationInfo(@JsonProperty("dataLocation") String dataLocation, @JsonProperty("ip") String ip) {
		this.dataLocation = dataLocation;
		this.ip = ip;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(InstallationInfo.class).add("loc", dataLocation).add("ip", ip).toString();
	}

}
