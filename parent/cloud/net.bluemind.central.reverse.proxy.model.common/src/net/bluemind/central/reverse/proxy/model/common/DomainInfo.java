package net.bluemind.central.reverse.proxy.model.common;

import java.util.Set;

public class DomainInfo {

	public String uid;

	public Set<String> aliases;

	public DomainInfo() {

	}

	public DomainInfo(String uid, Set<String> aliases) {
		this.uid = uid;
		this.aliases = aliases;
	}

}
