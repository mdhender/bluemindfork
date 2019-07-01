package net.bluemind.backend.cyrus.utils;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.ExecutableCommand;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;

public class Discard implements ExecutableCommand {

	@Override
	public Object execute(MailAdapter mailAdapter, Arguments arg1, Block arg2, SieveContext context)
			throws SieveException {
		mailAdapter.addAction(new DiscardAction());
		context.getCommandStateManager().setImplicitKeep(false);
		return null;
	}

}
