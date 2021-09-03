package net.bluemind.role.hook;

import java.util.Set;

import net.bluemind.directory.api.BaseDirEntry;

public class AdminRoleEvent extends RoleEvent {
	public final String dirUid;

	public AdminRoleEvent(String domainUid, String uid, String dirUid, BaseDirEntry.Kind kind, Set<String> roles) {
		super(domainUid, uid, kind, roles);
		this.dirUid = dirUid;
	}

}
