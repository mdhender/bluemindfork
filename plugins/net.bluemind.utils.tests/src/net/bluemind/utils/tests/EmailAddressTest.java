package net.bluemind.utils.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.mail.internet.AddressException;

import org.junit.Test;

import net.bluemind.utils.EmailAddress;

public class EmailAddressTest {

	@Test
	public void testPersonalRepeatedInAddress() throws AddressException {
		String email = "n <ned@inventati.org>";
		EmailAddress mboxFrom = new EmailAddress(email);
		assertEquals("n", mboxFrom.getPersonal());
		assertEquals("ned@inventati.org", mboxFrom.getAddress());

		email = "alwaysdata <no-reply+fr@alwaysdata.com>";
		mboxFrom = new EmailAddress(email);
		assertEquals("alwaysdata", mboxFrom.getPersonal());
		assertEquals("no-reply+fr@alwaysdata.com", mboxFrom.getAddress());

		email = "chloe.com <refunds@eboutique.chloe.com>";
		mboxFrom = new EmailAddress(email);
		assertEquals("chloe.com", mboxFrom.getPersonal());
		assertEquals("refunds@eboutique.chloe.com", mboxFrom.getAddress());
	}

	@Test
	public void testNoPersonal() throws AddressException {
		String email = "<refunds@eboutique.chloe.com>";
		EmailAddress mboxFrom = new EmailAddress(email);
		mboxFrom = new EmailAddress(email);
		// net.bluemind.exchange.ews.delegates.oxwsolps.DoSearch check for null
		assertNull(mboxFrom.getPersonal());
		assertEquals("refunds@eboutique.chloe.com", mboxFrom.getAddress());

		email = "refunds@eboutique.chloe.com";
		mboxFrom = new EmailAddress(email);
		mboxFrom = new EmailAddress(email);
		// net.bluemind.exchange.ews.delegates.oxwsolps.DoSearch check for null
		assertNull(mboxFrom.getPersonal());
		assertEquals("refunds@eboutique.chloe.com", mboxFrom.getAddress());
	}

	@Test
	public void testWithinSingleQuote() throws AddressException {
		String email = "'n <ned@inventati.org>'";
		EmailAddress mboxFrom = new EmailAddress(email);
		assertEquals("n", mboxFrom.getPersonal());
		assertEquals("ned@inventati.org", mboxFrom.getAddress());

		email = "'<ned@inventati.org>'";
		mboxFrom = new EmailAddress(email);
		assertNull(mboxFrom.getPersonal());
		assertEquals("ned@inventati.org", mboxFrom.getAddress());

		email = "'ned@inventati.org'";
		mboxFrom = new EmailAddress(email);
		assertNull(mboxFrom.getPersonal());
		assertEquals("ned@inventati.org", mboxFrom.getAddress());
	}

	@Test(expected = javax.mail.internet.AddressException.class)
	public void testNoAddress() throws AddressException {
		String email = "BACQ Ludovic [MIDI2I]";
		EmailAddress mboxFrom = new EmailAddress(email);
	}

	@Test(expected = javax.mail.internet.AddressException.class)
	public void testInvalidAddress() throws AddressException {
		String email = "<Microsoft Outlook>";
		EmailAddress mboxFrom = new EmailAddress(email);
	}

	@Test(expected = javax.mail.internet.AddressException.class)
	public void testEmptyAddress() throws AddressException {
		String email = "<>";
		EmailAddress mboxFrom = new EmailAddress(email);
	}
}
