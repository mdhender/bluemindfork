package net.bluemind.core.backup.continuous.restore.domains.crud.directories;

import com.google.common.base.MoreObjects;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.mailbox.api.Mailbox;

public class FullDirEntry<T> {
	public DirEntry entry;
	public VCard vcard;
	public Mailbox mailbox;

	public T value;

	@Override
	public String toString() {
		return MoreObjects.toStringHelper("DE").add("entry", entry).add("vcard", vcard).add("mbox", mailbox).toString();
	}
}