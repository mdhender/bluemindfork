package net.bluemind.imap.endpoint.driver;

import com.google.common.base.MoreObjects;

public class ImapIdSet {

	public enum IdKind {
		SEQ_NUM, UID
	}

	public IdKind setStyle;
	public String serializedSet;

	public ImapIdSet(IdKind kind, String set) {
		this.setStyle = kind;
		this.serializedSet = set;
	}

	public static ImapIdSet sequences(String set) {
		return new ImapIdSet(IdKind.SEQ_NUM, set);
	}

	public static ImapIdSet uids(String set) {
		return new ImapIdSet(IdKind.UID, set);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(ImapIdSet.class).add("k", setStyle).add("s", serializedSet).toString();
	}

}
