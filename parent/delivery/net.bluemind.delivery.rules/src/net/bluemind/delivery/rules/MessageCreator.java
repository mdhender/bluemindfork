package net.bluemind.delivery.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.LenientFieldParser;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class MessageCreator {
	private static final Logger logger = LoggerFactory.getLogger(MessageCreator.class);

	private final ResolvedBox senderBox;
	private final Message originalMessage;
	private final BasicBodyFactory bodyFactory;

	public MessageCreator(ResolvedBox senderBox, Message originalMessage) {
		this.senderBox = senderBox;
		this.originalMessage = originalMessage;
		this.bodyFactory = new BasicBodyFactory();
	}

	public Message newMessageWithOriginalAttached(MailboxList to) {
		String subject = originalMessage.getSubject();
		MessageImpl msg = createNewMessage(to, "Fwd: " + subject);

		Multipart mixedMultipart = new MultipartImpl("mixed");
		msg.setMultipart(mixedMultipart);

		addOriginalMessageAsAttachment(mixedMultipart);

		return msg;
	}

	private void addOriginalMessageAsAttachment(Multipart mixedMultipart) {
		try {
			InputStream data = Mime4JHelper.asStream(originalMessage);
			BodyPart bpa = new BodyPart();
			BinaryBody bb = new BasicBodyFactory().binaryBody(data);
			bpa.setBody(bb, "message/rfc822");
			bpa.setContentTransferEncoding("base64");
			bpa.setFilename("forward.eml");
			mixedMultipart.addBodyPart(bpa);
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	record OriginalContentAndParts(Entity content, List<Entity> parts) {
	}

	record NewRelatedAndMixedParts(List<Entity> related, List<Entity> mixed) {
	}

	public Message newMessageWithOriginalCited(MailboxList to, String subjectPrefix, String subject, String txtContent,
			String htmlContent, boolean isReply) {
		subject = Strings.isNullOrEmpty(subject) ? subjectPrefix + ": " + originalMessage.getSubject() : subject;

		MessageImpl msg = createNewMessage(to, subject);

		MultipartImpl relatedMultipart = new MultipartImpl("related");
		BodyPart relatedBody = new BodyPart();
		relatedBody.setMultipart(relatedMultipart);

		Multipart mixedMultipart = new MultipartImpl("mixed");
		msg.setMultipart(mixedMultipart);
		mixedMultipart.addBodyPart(relatedBody);

		OriginalContentAndParts originalParts = extractOriginalParts(!isReply);
		NewRelatedAndMixedParts parts = buildNewParts(txtContent, htmlContent, originalParts);
		parts.related.stream().forEach(relatedMultipart::addBodyPart);
		parts.mixed.stream().forEach(mixedMultipart::addBodyPart);

		if (isReply) {
			addReplySpecificHeaders(msg);
		}

		return msg;
	}

	private MessageImpl createNewMessage(MailboxList to, String subject) {
		MessageImpl msg = new MessageImpl();
		msg.setSubject(subject);
		msg.setDate(new Date());
		msg.createMessageId(senderBox.dom.value.defaultAlias);
		Mailbox mailbox = SendmailHelper.formatAddress(senderBox.mbox.value.name,
				senderBox.mbox.value.defaultEmail().address);
		msg.setFrom(mailbox);
		msg.setSender(mailbox);
		msg.setTo(to);
		return msg;
	}

	private OriginalContentAndParts extractOriginalParts(boolean keepAttachments) {
		List<Entity> attachments = new ArrayList<>();
		if (!originalMessage.isMultipart()) {
			return new OriginalContentAndParts(originalMessage, attachments);
		}

		List<Entity> parts = ((Multipart) originalMessage.getBody()).getBodyParts();
		List<AddressableEntity> addressableParts = Mime4JHelper.expandTree(parts);
		Entity part = null;
		for (Entity e : addressableParts) {
			if (e.getMimeType() != null && !Mime4JHelper.isAttachment(e) && //
					("text/html".equals(e.getMimeType()) || ("text/plain".equals(e.getMimeType()) && part == null))) {
				part = e;
			} else if (Mime4JHelper.isAttachment(e) && keepAttachments) {
				attachments.add(e);
			}
		}

		return new OriginalContentAndParts(part, attachments);
	}

	private NewRelatedAndMixedParts buildNewParts(String textContent, String htmlContent,
			OriginalContentAndParts parts) {
		boolean isReplyInHtml = (htmlContent != null);
		String reply = (isReplyInHtml) ? htmlContent : textContent;
		String replyWithContext = (parts.content() != null) //
				? Mime4JHelper.insertQuotePart(isReplyInHtml, reply, parts.content)
				: reply;

		BodyPart htmlBody = new BodyPart();
		TextBody textBody = textBodyInUTF8(replyWithContext);
		htmlBody.setContentTransferEncoding("base64");
		htmlBody.setText(textBody, isReplyInHtml ? "html" : "plain");

		List<Entity> related = new ArrayList<>();
		List<Entity> mixed = new ArrayList<>();
		related.add(htmlBody);
		parts.parts.stream().forEach(part -> {
			Field cid = part.getHeader().getField("Content-ID");
			if (cid != null) {
				related.add(part);
			} else {
				mixed.add(part);
			}
		});

		return new NewRelatedAndMixedParts(related, mixed);
	}

	private void addReplySpecificHeaders(Message replyMessage) {
		// https://www.ietf.org/rfc/rfc2822.txt
		String messageId = originalMessage.getMessageId();
		if (messageId != null) {
			addHeader(replyMessage, "In-Reply-To", messageId);
		}

		Field referencesField = originalMessage.getHeader().getField("References");
		Field inReplyToField = originalMessage.getHeader().getField("In-Reply-To");
		String references = "";
		if (referencesField != null) {
			references = referencesField.getBody() + " ";
		} else if (inReplyToField != null && !Strings.isNullOrEmpty(inReplyToField.getBody())
				&& inReplyToField.getBody().trim().indexOf(">") == inReplyToField.getBody().trim().lastIndexOf(">")) {
			references = inReplyToField.getBody() + " ";
		}
		String newReferences = (messageId != null) ? references + messageId : references;
		addHeader(replyMessage, "References", newReferences.trim());
	}

	private void addHeader(Message message, String name, String value) {
		try {
			message.getHeader().addField(LenientFieldParser.parse(name + ":" + value));
		} catch (MimeException e) {
			logger.warn("Unable to add header '{}={}' to message", name, value, e);
		}
	}

	private TextBody textBodyInUTF8(String content) {
		try {
			return bodyFactory.textBody(content, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported encoding");
		}
	}
}
