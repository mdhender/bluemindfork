package net.bluemind.imap.sieve.commands;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.imap.sieve.SieveArg;
import net.bluemind.imap.sieve.SieveCommand;
import net.bluemind.imap.sieve.SieveResponse;

public class GetScript extends SieveCommand<String> {

	private String name;

	public GetScript(String name) {
		this.name = name;
	}

	@Override
	public void responseReceived(SieveResponse rs) {
		logger.debug("getscript {} response received.", name);
		if (commandSucceeded(rs)) {
			retVal = rs.getLines().get(0);
		} else {
			logger.error("error " + rs.getMessageResponse());
		}
	}

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> args = new ArrayList<SieveArg>(1);
		args.add(new SieveArg(("GETSCRIPT \"" + name + "\"").getBytes(), false));
		return args;
	}

}
