package net.bluemind.imap.endpoint.cmd;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public class AuthenticateCommand extends AnalyzedCommand {

	private final String mech;

	protected AuthenticateCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(false);
		try {
			CommandReader cr = new CommandReader(flat);
			cr.command("authenticate");
			this.mech = cr.nextString();
		} catch (Exception e) {
			throw new EndpointRuntimeException("Cannot split '" + flat.fullCmd + "'");
		}
	}

	public String mech() {
		return mech;
	}

}
