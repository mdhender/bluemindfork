package net.bluemind.central.reverse.proxy.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class InstallationInfo {
	public final String uid;
	public final String dataLocationUid;
	public final String ip;
	public final boolean hasNginx;
	public final boolean hasCore;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public InstallationInfo(@JsonProperty("uid") String uid, @JsonProperty("dataLocation") String dataLocationUid,
			@JsonProperty("ip") String ip, @JsonProperty("hasNginx") boolean hasNginx,
			@JsonProperty("hasCore") boolean hasCore) {
		this.uid = uid;
		this.dataLocationUid = dataLocationUid;
		this.ip = ip;
		this.hasNginx = hasNginx;
		this.hasCore = hasCore;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(InstallationInfo.class).add("loc", dataLocationUid).add("ip", ip)
				.add("hasNginx", hasNginx).add("hasCore", hasCore).toString();
	}
}
