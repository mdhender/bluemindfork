package net.bluemind.central.reverse.proxy.model.common;

import java.util.Set;

import com.google.common.base.MoreObjects;

public class DomainInfo {

	public String uid;

	public Set<String> aliases;

	public DomainInfo() {
	}

	public DomainInfo(String uid, Set<String> aliases) {
		this.uid = uid;
		this.aliases = aliases;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DomainInfo.class).add("uid", uid).add("aliases", aliases).toString();
	}
}
