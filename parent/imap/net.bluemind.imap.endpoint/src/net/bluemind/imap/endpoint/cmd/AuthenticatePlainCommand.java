package net.bluemind.imap.endpoint.cmd;

import java.util.Base64;

public class AuthenticatePlainCommand extends AnalyzedCommand {

	private byte[] payload;

	protected AuthenticatePlainCommand(RawImapCommand raw) {
		super(raw);
		String b64 = raw.cmd();
		this.payload = Base64.getDecoder().decode(b64);
	}

	public byte[] payload() {
		return payload;
	}

}
