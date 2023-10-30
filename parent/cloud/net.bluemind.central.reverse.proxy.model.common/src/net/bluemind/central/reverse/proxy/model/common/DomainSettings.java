package net.bluemind.central.reverse.proxy.model.common;

import com.google.common.base.MoreObjects;

public class DomainSettings {

	public String domainUid;

	public String mailRoutingRelay;

	public boolean mailForwardUnknown;

	public DomainSettings() {
	}

	public DomainSettings(String domainUid, String mailRoutingRelay, boolean mailForwardUnknown) {
		this.domainUid = domainUid;
		this.mailRoutingRelay = mailRoutingRelay;
		this.mailForwardUnknown = mailForwardUnknown;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DomainSettings.class).add("domainUid", domainUid)
				.add("mailRoutingRelay", mailRoutingRelay).add("mailForwardUnknown", mailForwardUnknown).toString();
	}
}
