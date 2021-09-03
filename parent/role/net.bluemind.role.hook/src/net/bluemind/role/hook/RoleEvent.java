package net.bluemind.role.hook;

import java.util.Collections;
import java.util.Set;

import net.bluemind.directory.api.BaseDirEntry;

public class RoleEvent {
	public final String domainUid;
	public final String uid;
	public final BaseDirEntry.Kind kind;
	public final Set<String> roles;

	public RoleEvent(String domainUid, String uid, BaseDirEntry.Kind kind, Set<String> roles) {
		this.domainUid = domainUid;
		this.uid = uid;
		this.kind = kind;
		this.roles = Collections.unmodifiableSet(roles);
	}

}
