package net.bluemind.delivery.rules.tests;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.RawField;

import net.bluemind.core.sendmail.SendmailHelper;

public class MessageBuilder {

	private Date date;
	private String subject;
	private String from;
	private List<String> to = new ArrayList<>();
	private List<String> cc = new ArrayList<>();
	private List<String> bcc = new ArrayList<>();
	private Map<String, String> headers = new HashMap<>();
	private String bodyHtml;
	private String bodyText;

	public MessageBuilder(String subject) {
		this.subject = subject;
	}

	public MessageBuilder subject(String subject) {
		this.subject = subject;
		return this;
	}

	public MessageBuilder date(Date date) {
		this.date = date;
		return this;
	}

	public MessageBuilder from(String from) {
		this.from = from;
		return this;
	}

	public MessageBuilder returnPath(String from) {
		this.from = from;
		return this;
	}

	public MessageBuilder to(List<String> to) {
		this.to = to;
		return this;
	}

	public MessageBuilder to(String... to) {
		this.to = Arrays.asList(to);
		return this;
	}

	public MessageBuilder cc(List<String> cc) {
		this.cc = cc;
		return this;
	}

	public MessageBuilder cc(String... cc) {
		this.cc = Arrays.asList(cc);
		return this;
	}

	public MessageBuilder bcc(List<String> bcc) {
		this.bcc = bcc;
		return this;
	}

	public MessageBuilder bcc(String... bcc) {
		this.bcc = Arrays.asList(bcc);
		return this;
	}

	public MessageBuilder header(String name, String value) {
		this.headers.put(name, value);
		return this;
	}

	public MessageBuilder content(String bodyText, String bodyHtml) {
		this.bodyText = bodyText;
		this.bodyHtml = bodyHtml;
		return this;
	}

	public MessageImpl build() {
		var message = new MessageImpl();
		message.setSubject(subject);
		message.setDate(date);
		if (from != null) {
			message.setFrom(SendmailHelper.formatAddress(null, from));
		}
		message.setTo(mailboxList(to));
		message.setCc(mailboxList(cc));
		message.setBcc(mailboxList(bcc));
		headers.forEach((name, value) -> message.getHeader().addField(new RawField(name, value)));

		if (bodyHtml != null || bodyText != null) {
			MultipartImpl relatedMultipart = new MultipartImpl("related");
			BodyPart relatedBody = new BodyPart();
			relatedBody.setMultipart(relatedMultipart);

			Multipart mixedMultipart = new MultipartImpl("mixed");
			message.setMultipart(mixedMultipart);
			mixedMultipart.addBodyPart(relatedBody);

			if (bodyText != null) {
				BodyPart textBodyPart = new BodyPart();
				TextBody textBody = textBodyInUTF8(bodyText);
				textBodyPart.setContentTransferEncoding("base64");
				textBodyPart.setText(textBody, "plain");
				relatedMultipart.addBodyPart(textBodyPart);
			}

			if (bodyHtml != null) {
				BodyPart htmlBodyPart = new BodyPart();
				TextBody htmlBody = textBodyInUTF8(bodyHtml);
				htmlBodyPart.setContentTransferEncoding("base64");
				htmlBodyPart.setText(htmlBody, "html");
				relatedMultipart.addBodyPart(htmlBodyPart);
			}
		}

		return message;
	}

	private MailboxList mailboxList(List<String> emails) {
		List<org.apache.james.mime4j.dom.address.Mailbox> mailboxes = emails.stream()
				.map(email -> SendmailHelper.formatAddress(null, email)).toList();
		return new MailboxList(mailboxes, true);
	}

	protected TextBody textBodyInUTF8(String content) {
		try {
			return new BasicBodyFactory().textBody(content, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported encoding");
		}
	}

}
