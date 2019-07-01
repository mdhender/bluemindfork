package net.bluemind.backend.cyrus.utils;

import org.apache.jsieve.Arguments;
import org.apache.jsieve.SieveContext;
import org.apache.jsieve.exception.SieveException;
import org.apache.jsieve.mail.MailAdapter;
import org.apache.jsieve.tests.ExecutableTest;

public class ImapFlags implements ExecutableTest {

	@Override
	public boolean execute(MailAdapter mail, Arguments arguments, SieveContext context) throws SieveException {
		// TODO Auto-generated method stub
		return false;
	}

}
