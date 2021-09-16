package net.bluemind.core.backup.continuous.dto;

import java.util.Set;

import net.bluemind.directory.api.BaseDirEntry;

public class DirEntryRole {

	public BaseDirEntry.Kind kind;
	public Set<String> roles;

	public DirEntryRole() {

	}

	public DirEntryRole(BaseDirEntry.Kind kind, Set<String> roles) {
		this.kind = kind;
		this.roles = roles;
	}
}
