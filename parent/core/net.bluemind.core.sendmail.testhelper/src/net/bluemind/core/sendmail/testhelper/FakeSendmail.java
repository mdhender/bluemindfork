package net.bluemind.core.sendmail.testhelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;

import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Mail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailResponse;

public class FakeSendmail implements ISendmail {
	public boolean mailSent = false;
	public List<TestMail> messages = new ArrayList<TestMail>();

	public Set<String> messagesTo() {
		return messages.stream().flatMap(m -> m.to.stream()).collect(Collectors.toSet());
	}

	public Set<String> messagesFrom() {
		return messages.stream().map(m -> m.from).collect(Collectors.toSet());
	}

	@Override
	public SendmailResponse send(Mail m) {
		mailSent = true;
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(Mailbox sender, Message m) {
		mailSent = true;
		messages.add(TestMail.fromMessage(m));
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String domainUid, Message m) {
		mailSent = true;
		messages.add(TestMail.fromMessage(m));
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, Message m) {
		mailSent = true;
		return SendmailResponse.success();
	}

	@Override
	public SendmailResponse send(SendmailCredentials creds, String fromEmail, String userDomain, MailboxList rcptTo,
			Message m) {
		mailSent = true;

		TestMail tm = new TestMail();
		tm.from = fromEmail;
		for (Mailbox mbox : rcptTo) {
			tm.to.add(mbox.getAddress());
		}

		tm.message = m;
		messages.add(tm);
		return SendmailResponse.success();
	}
}
