package net.bluemind.backend.mail.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.Ack;

@BMApi(version = "3")
public class ImapAck extends Ack {
	public long imapUid;

	public ImapAck() {
	}

	private ImapAck(long version, long imapUid) {
		this.version = version;
		this.imapUid = imapUid;
	}

	public static ImapAck create(long version, long imapUid) {
		return new ImapAck(version, imapUid);
	}
}
