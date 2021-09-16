package net.bluemind.central.reverse.proxy.model.mapper;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class DirInfo {

	public static class DirEmail {
		public final String address;
		public final boolean allAliases;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public DirEmail(@JsonProperty("address") String address, @JsonProperty("allAliases") boolean allAliases) {
			this.address = address;
			this.allAliases = allAliases;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper("E").add("mail", address).add("all", allAliases).toString();
		}
	}

	public final String domainUid;

	public final Collection<DirEmail> emails;

	public final String dataLocation;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DirInfo(@JsonProperty("domainUid") String domainUid, @JsonProperty("emails") Collection<DirEmail> logins,
			@JsonProperty("dataLocation") String dataLocation) {
		this.domainUid = domainUid;
		this.emails = logins;
		this.dataLocation = dataLocation;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DirInfo.class).add("loc", dataLocation).add("dom", domainUid)
				.add("mails", emails).toString();
	}

}
