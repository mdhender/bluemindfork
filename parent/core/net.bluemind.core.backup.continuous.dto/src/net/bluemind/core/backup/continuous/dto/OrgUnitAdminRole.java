package net.bluemind.core.backup.continuous.dto;

import java.util.Set;

import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.OrgUnit;

public class OrgUnitAdminRole extends DirEntryRole {

	public String dirUid;
	public OrgUnit orgUnit;

	public OrgUnitAdminRole() {

	}

	public OrgUnitAdminRole(BaseDirEntry.Kind kind, Set<String> roles, String dirUid, OrgUnit orgUnit) {
		super(kind, roles);
		this.dirUid = dirUid;
		this.orgUnit = orgUnit;
	}
}
