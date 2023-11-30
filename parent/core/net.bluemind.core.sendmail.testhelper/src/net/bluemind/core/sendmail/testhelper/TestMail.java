package net.bluemind.core.sendmail.testhelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;

public class TestMail {
	public Message message;
	public String from;
	public Set<String> to = new HashSet<>();

	public static TestMail fromMessage(Message m) {
		TestMail testMail = new TestMail();
		testMail.message = m;
		testMail.from = m.getFrom().get(0).getAddress();
		Iterator<Address> it = m.getTo().iterator();
		while (it.hasNext()) {
			testMail.to.add(((Mailbox) it.next()).getAddress());
		}

		return testMail;
	}
}
