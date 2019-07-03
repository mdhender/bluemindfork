package net.bluemind.backend.cyrus.utils;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.ExecutableCommand;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

public class Vacation implements ExecutableCommand {

	@Override
	public Object execute(MailAdapter adapter, Arguments args, Block arg2, SieveContext arg3) throws SieveException {

		for (Argument arg : args.getArgumentList()) {
			System.out.println("arg " + arg.getValue());
		}
		// TODO Auto-generated method stub
		return null;
	}

}
