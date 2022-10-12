package net.bluemind.delivery.rules;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.stream.Field;

import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleField;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.utils.FileUtils;

public class FieldValueMessageProvider implements FieldValueProvider {

	private final Message message;
	private final Long size;

	public FieldValueMessageProvider(Message message, long size) {
		this.message = message;
		this.size = size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T provides(MailFilterRuleField<T> field) {
		T value;
		switch (field.field()) {
		case FROM_EMAIL:
			value = (T) extractEmails(message.getFrom());
			break;
		case FROM_RAW:
			value = (T) extractRecipients(message.getFrom());
			break;
		case TO_EMAIL:
			value = (T) extractEmails(message.getTo());
			break;
		case TO_RAW:
			value = (T) extractRecipients(message.getFrom());
			break;
		case CC_EMAIL:
			value = (T) extractEmails(message.getCc());
			break;
		case CC_RAW:
			value = (T) extractRecipients(message.getCc());
			break;
		case BCC_EMAIL:
			value = (T) extractEmails(message.getBcc());
			break;
		case BCC_RAW:
			value = (T) extractRecipients(message.getBcc());
			break;
		case SUBJECT:
			value = (T) Arrays.asList(message.getSubject());
			break;
		case PART_CONTENT:
			value = (T) Arrays.asList(extractContent());
			break;
		case SIZE:
			value = (T) size;
			break;
		case DATE:
			value = (T) message.getDate();
			break;
		case HEADERS:
			String[] tokens = field.name().split("\\.");
			value = (tokens.length > 1) ? (T) extractHeaders(tokens[1]) : (T) Collections.emptyList();
			break;
		case ATTACHMENTS_COUNT:
			value = (T) countAttachment();
			break;
		default:
			value = null;
		}
		return value;
	}

	private List<String> extractEmails(AddressList addresses) {
		return (addresses != null) ? extractEmails(addresses.flatten()) : Collections.emptyList();
	}

	private List<String> extractEmails(MailboxList mailboxes) {
		return extractMailboxes(mailboxes).map(Mailbox::getAddress).toList();
	}

	private List<String> extractRecipients(AddressList addresses) {
		return (addresses != null) ? extractRecipients(addresses.flatten()) : Collections.emptyList();
	}

	private List<String> extractRecipients(MailboxList mailboxes) {
		return extractMailboxes(mailboxes).map( //
				mailbox -> (mailbox.getName() == null || mailbox.getName().isBlank()) //
						? mailbox.getAddress() //
						: mailbox.getName() + " <" + mailbox.getAddress() + ">" //
		).toList();
	}

	private Stream<Mailbox> extractMailboxes(MailboxList mailboxes) {
		if (mailboxes == null) {
			return Stream.empty();
		}
		return mailboxes.stream().filter(mailbox -> !">".equals(mailbox.getAddress()));
	}

	private List<String> extractHeaders(String headerName) {
		return message.getHeader().getFields().stream() //
				.filter(field -> field.getName().equalsIgnoreCase(headerName)) //
				.map(Field::getBody) //
				.toList();
	}

	private String extractContent() {
		return entitiesMatching("text").stream().map(this::extractEntityContent).collect(Collectors.joining(" "));
	}

	private List<Entity> entitiesMatching(String mimeType) {
		if (!message.isMultipart()) {
			return isEntityMatching(message, mimeType) ? Arrays.asList(message) : Collections.emptyList();
		}

		List<Entity> parts = ((Multipart) message.getBody()).getBodyParts();
		List<AddressableEntity> addressableParts = Mime4JHelper.expandTree(parts);
		return addressableParts.stream().filter(entity -> isEntityMatching(entity, mimeType)).map(Entity.class::cast)
				.toList();
	}

	private String extractEntityContent(Entity entity) {
		TextBody tb = (TextBody) entity.getBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(tb.getInputStream(), out, true);
			String charset = Optional.of(entity.getCharset()).orElse("utf-8");
			String content = new String(out.toByteArray(), charset);
			return (entity.getMimeType().endsWith("html")) ? StringEscapeUtils.unescapeHtml4(content) : content;
		} catch (IOException ioe) {
			return "";
		}
	}

	private boolean isEntityMatching(Entity entity, String mimeType) {
		String mime = entity.getMimeType();
		return mime != null && !mime.endsWith("/") && mime.startsWith(mimeType);
	}

	private Long countAttachment() {
		if (!message.isMultipart()) {
			return 0l;
		}

		List<Entity> parts = ((Multipart) message.getBody()).getBodyParts();
		return Mime4JHelper.expandTree(parts).stream().filter(Mime4JHelper::isAttachment).count();
	}
}
