/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.api.flags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.api.BMApi;

/**
 * IMAP flag as defined in RFC3501 (https://tools.ietf.org/html/rfc3501#section-2.3.2) 
 */
@BMApi(version = "3")
public class SystemFlag extends MailboxItemFlag {

	/**
	 * numerical representation of a {@link SystemFlag}
	 */
	public int value;
	
	public SystemFlag() {}
	
	private SystemFlag(String flag, int value) {
		super(flag);
		this.value = value;
		super.isSystem = true;
	}
	
	/**
	 * Mark a {@link MailboxItem} as answered
	 */
	@BMApi(version = "3")
	public static class AnsweredFlag extends SystemFlag {
		public AnsweredFlag() {
			super("\\Answered", 1 << 0);
		}
	}
	
	/**
	 * Mark a {@link MailboxItem} as flagged / important
	 */
	@BMApi(version = "3")
	public static class FlaggedFlag extends SystemFlag {
		public FlaggedFlag() {
			super("\\Flagged", 1 << 1);
		}
	}
	
	/**
	 * Mark a {@link MailboxItem} as deleted
	 */
	@BMApi(version = "3")
	public static class DeletedFlag extends SystemFlag {
		public DeletedFlag() {
			super("\\Deleted", 1 << 2);
		}
	}
	
	/**
	 * Mark a {@link MailboxItem} as draft
	 */
	@BMApi(version = "3")
	public static class DraftFlag extends SystemFlag {
		public DraftFlag() {
			super("\\Draft", 1 << 3);
		}
	}
	
	/**
	 * Mark a {@link MailboxItem} as seen
	 */
	@BMApi(version = "3")
	public static class SeenFlag extends SystemFlag {
		public SeenFlag() {
			super("\\Seen", 1 << 4);
		}
	}
	
	/**
	 * @param flags
	 * @return numerical representation of a {@link SystemFlag} list 
	 */
	public static int valueOf(Iterable<SystemFlag> flags) {
		int ret = 0;
		for (SystemFlag sf : flags) {
			ret |= sf.value;
		}
		return ret;
	}
	
	/**
	 * @return all kind of {@link SystemFlag} 
	 */
	public static List<SystemFlag> all() {
		return Arrays.asList(new AnsweredFlag(), new DeletedFlag(), new FlaggedFlag(), new DraftFlag(), 
				new SeenFlag());
	}
	
	/**
	 * @param value numerical representation of a {@link SystemFlag} list 
	 * @return a {@link SystemFlag} list 
	 */
	public static List<MailboxItemFlag> of(int value) {
		List<MailboxItemFlag> ret = new ArrayList<>();
		for (SystemFlag sf : SystemFlag.all()) {
			if ((sf.value & value) == sf.value) {
				ret.add(sf);
			}
		}
		return ret;
	}
}