package net.bluemind.core.backup.continuous.dto;

import net.bluemind.mailbox.api.MailFilter;

public class MailboxMailFilter {

	public String uid;
	public boolean isDomain;
	public MailFilter filter;

	public MailboxMailFilter() {

	}

	public MailboxMailFilter(String uid, boolean isDomain, MailFilter filter) {
		this.uid = uid;
		this.isDomain = isDomain;
		this.filter = filter;
	}

}
