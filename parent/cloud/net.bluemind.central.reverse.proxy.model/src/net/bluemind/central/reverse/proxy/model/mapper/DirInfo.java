package net.bluemind.central.reverse.proxy.model.mapper;

import java.util.Collection;

public class DirInfo {

	public static class DirEmail {
		public String address;
		public boolean allAliases;

		public DirEmail() {

		}

		public DirEmail(String address, boolean allAliases) {
			this.address = address;
			this.allAliases = allAliases;
		}
	}

	public String domainUid;

	public Collection<DirEmail> emails;

	public String dataLocation;

	public DirInfo() {

	}

	public DirInfo(String domainUid, Collection<DirEmail> logins, String dataLocation) {
		this.domainUid = domainUid;
		this.emails = logins;
		this.dataLocation = dataLocation;
	}

}
