package net.bluemind.central.reverse.proxy.model.common;

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
			return MoreObjects.toStringHelper("E").add("mail", address).add("allAliases", allAliases).toString();
		}
	}

	public final String domainUid;

	public final String entryUid;

	public final String kind;

	public final boolean archived;

	public final String mailboxName;

	public final String routing;

	public final Collection<DirEmail> emails;

	public final String dataLocation;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DirInfo(@JsonProperty("domainUid") String domainUid, @JsonProperty("entryUid") String entryUid,
			@JsonProperty("kind") String kind, @JsonProperty("archived") boolean archived,
			@JsonProperty("mailboxName") String mailboxName, @JsonProperty("routing") String routing,
			@JsonProperty("emails") Collection<DirEmail> emails, @JsonProperty("dataLocation") String dataLocation) {
		this.domainUid = domainUid;
		this.entryUid = entryUid;
		this.kind = kind;
		this.archived = archived;
		this.mailboxName = mailboxName;
		this.routing = routing;
		this.emails = emails;
		this.dataLocation = dataLocation;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(DirInfo.class).add("domainUid", domainUid).add("entryUid", entryUid)
				.add("loc", dataLocation).add("archived", archived).add("kind", kind).add("mailboxName", mailboxName)
				.add("routing", routing).add("dom", domainUid).add("mails", emails).toString();
	}

}
