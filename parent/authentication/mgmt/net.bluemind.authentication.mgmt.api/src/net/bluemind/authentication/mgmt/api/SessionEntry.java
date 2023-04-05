package net.bluemind.authentication.mgmt.api;

import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SessionEntry {
	public long created;
	public String email;
	public String domainUid;
	public String userUid;
	public String origin;
	public List<String> remoteAddresses;

	public static SessionEntry build(long created, String email, String domainUid, String userUid, String origin,
			List<String> remoteAddresses) {
		SessionEntry sessionEntry = new SessionEntry();
		sessionEntry.created = created;
		sessionEntry.email = email;
		sessionEntry.domainUid = domainUid;
		sessionEntry.userUid = userUid;
		sessionEntry.origin = origin;
		sessionEntry.remoteAddresses = remoteAddresses;

		return sessionEntry;
	}
}
