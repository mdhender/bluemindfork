package net.bluemind.backend.mail.replica.service.tests;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Mail;

public class FakeSendmail implements ISendmail {

	@Override
	public void send(String fromEmail, String userDomain, Message m) throws ServerFault {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(Mail m) throws ServerFault {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(Mailbox from, Message m) throws ServerFault {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(String domainUid, Message m) throws ServerFault {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(String fromEmail, String userDomain, MailboxList rcptTo, Message m) throws ServerFault {
		// TODO Auto-generated method stub
		
	}

}
