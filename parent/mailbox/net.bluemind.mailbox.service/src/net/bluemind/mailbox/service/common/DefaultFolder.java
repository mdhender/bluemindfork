package net.bluemind.mailbox.service.common;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class DefaultFolder {
	public static class Status {
		public Set<DefaultFolder> missing = new HashSet<>();
		public Set<DefaultFolder> invalidSpecialuse = new HashSet<>();
		public Set<DefaultFolder> fixed = new HashSet<>();

		public boolean isOk() {
			return missing.size() == 0 && invalidSpecialuse.size() == 0;
		}
	}

	public static final Set<DefaultFolder> USER_FOLDERS = ImmutableSet.of(new DefaultFolder("Sent", "Sent"),
			new DefaultFolder("Trash", "Trash"), new DefaultFolder("Drafts", "Drafts"),
			new DefaultFolder("Junk", "Junk"), new DefaultFolder("Outbox"), new DefaultFolder("Templates"));

	public static final Set<String> USER_FOLDERS_NAME = USER_FOLDERS.stream().map(df -> df.name)
			.collect(ImmutableSet.toImmutableSet());

	public static final Set<DefaultFolder> MAILSHARE_FOLDERS = ImmutableSet.of(new DefaultFolder("Sent", "Sent"));
	public static final Set<String> MAILSHARE_FOLDERS_NAME = MAILSHARE_FOLDERS.stream().map(f -> f.name)
			.collect(ImmutableSet.toImmutableSet());

	public final String name;
	public final String specialuse;

	private DefaultFolder(String name, String specialUseFlag) {
		this.name = name;
		this.specialuse = specialUseFlag;
	}

	private DefaultFolder(String name) {
		this.name = name;
		this.specialuse = null;
	}

	/**
	 * Check if specialuse annotation value are equals
	 * 
	 * Take care of optional \ leading char, case insensitive compares...
	 * 
	 * @param su
	 * @return
	 */
	public boolean specialuseEquals(String su) {
		if (specialuse == null) {
			return su == null;
		}

		if (su == null) {
			return false;
		}

		int start = 0;
		if (su.charAt(start) == '\\') {
			start = 1;
		}

		return specialuse.equalsIgnoreCase(su.substring(start));
	}
}
