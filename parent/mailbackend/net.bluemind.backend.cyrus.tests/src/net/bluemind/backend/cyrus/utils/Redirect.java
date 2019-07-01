package net.bluemind.backend.cyrus.utils;

import org.apache.jsieve.Argument;
import org.apache.jsieve.Arguments;
import org.apache.jsieve.Block;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.StringListArgument;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.commands.AbstractActionCommand;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.ActionRedirect;
import org.apache.jsieve.mail.MailAdapter;

public class Redirect extends AbstractActionCommand {

	@Override
	protected Object executeBasic(MailAdapter mail, Arguments arguments, Block block, SieveContext context)
			throws SieveException {
		Argument t = arguments.getArgumentList().get(0);

		if (t instanceof TagArgument) {
			t = arguments.getArgumentList().get(1);
		}

		if (t instanceof StringListArgument) {
			String recipient = ((StringListArgument) t).getList().get(0);

			mail.addAction(new ActionRedirect(recipient));
		}
		return null;

	}

	/**
	 * @see org.apache.jsieve.commands.AbstractCommand#validateArguments(Arguments,
	 *      SieveContext)
	 */
	protected void validateArguments(Arguments arguments, SieveContext context) throws SieveException {
	}

}
