package net.bluemind.central.reverse.proxy.model.mapper;

public class InstallationInfo {

	public String dataLocation;

	public String ip;

	public InstallationInfo() {

	}

	public InstallationInfo(String dataLocation, String ip) {
		this.dataLocation = dataLocation;
		this.ip = ip;
	}

}
