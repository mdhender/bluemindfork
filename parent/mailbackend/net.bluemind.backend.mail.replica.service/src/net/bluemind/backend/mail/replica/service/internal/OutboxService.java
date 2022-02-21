package net.bluemind.backend.mail.replica.service.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.MessageWriter;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import io.netty.buffer.ByteBufInputStream;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.mail.api.IItemsTransfer;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.sendmail.FailedRecipient;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mime4j.common.Mime4JHelper;

public class OutboxService implements IOutbox {

	private static final Logger logger = LoggerFactory.getLogger(OutboxService.class);

	private final BmContext context;
	private final String domainUid;
	private final ItemValue<Mailbox> mailboxItem;
	private final IServiceProvider serviceProvider;
	private final RBACManager rbac;

	private ISendmail mailer;

	public OutboxService(BmContext context, String domainUid, ItemValue<Mailbox> mailboxItem, ISendmail mailer) {
		this.context = context;
		this.domainUid = domainUid;
		this.mailboxItem = mailboxItem;
		this.serviceProvider = context.getServiceProvider();
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(mailboxItem.uid));

		this.mailer = mailer;
	}

	@Override
	public TaskRef flush() {
		rbac.check(Verb.Write.name());
		return serviceProvider.instance(ITasksManager.class).run(monitor -> {
			IMailboxFolders mailboxFoldersService = serviceProvider.instance(IMailboxFoldersByContainer.class,
					IMailReplicaUids.subtreeUid(domainUid, mailboxItem));

			ItemValue<MailboxFolder> outboxFolder = mailboxFoldersService.byName("Outbox");
			String outboxUid = outboxFolder.uid;
			ItemValue<MailboxFolder> sentFolder = mailboxFoldersService.byName("Sent");

			IMailboxItems mailboxItemsService = serviceProvider.instance(IMailboxItems.class, outboxUid);
			long enumerate = System.currentTimeMillis();
			List<ItemValue<MailboxItem>> mails = retrieveOutboxItems(mailboxItemsService);
			int mailCount = mails.size();
			enumerate = System.currentTimeMillis() - enumerate;
			logger.info("[{}] Flushing {} outbox item(s), enumerate took {}ms.",
					context.getSecurityContext().getSubject(), mails.size(), enumerate);
			monitor.begin(mails.size(), "FLUSHING OUTBOX - have " + mailCount + " mails to send.");

			AuthUser user = serviceProvider.instance(IAuthentication.class).getCurrentUser();

			List<CompletableFuture<Void>> promises = new ArrayList<>(mailCount);
		 List<FlushResult> flushResults = new ArrayList<>(mailCount);
			mails.forEach(item -> promises.add(flushOne(monitor, mailboxFoldersService, outboxFolder, sentFolder,
					mailboxItemsService, user, flushResults, item)));

			try {
				CompletableFuture.allOf(Iterables.toArray(promises, CompletableFuture.class)).thenAccept(finished -> {
					logger.info("[{}] flushed {}", context.getSecurityContext().getSubject(), mailCount);
					monitor.end(true, "FLUSHING OUTBOX finished successfully",
							String.format("{\"result\": %s}", JsonUtils.asString(flushResults)));
				}).get(30, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				monitor.end(false, "FLUSHING OUTBOX - timeout reached", "{\"result\": \"" + e + "\"}");
				logger.warn("FLUSHING OUTBOX - timeout reached", e);
			} catch (Exception e) {
				monitor.end(false, "FLUSHING OUTBOX - finished in error", "{\"result\": \"" + e + "\"}");
				logger.error("FLUSHING OUTBOX - finished in error", e);
			}
		});
	}

	private CompletableFuture<Void> flushOne(IServerTaskMonitor monitor, IMailboxFolders mailboxFoldersService,
			ItemValue<MailboxFolder> outboxFolder, ItemValue<MailboxFolder> sentFolder,
			IMailboxItems mailboxItemsService, AuthUser user, List<FlushResult> flushResults,
			ItemValue<MailboxItem> item) {
		return SyncStreamDownload.read(mailboxItemsService.fetchComplete(item.value.imapUid)).thenAccept(buf -> {
			InputStream in = new ByteBufInputStream(buf.duplicate());
			InputStream forSend = new ByteBufInputStream(buf);
			try (Message msg = Mime4JHelper.parse(in)) {
				if (msg.getFrom() == null) {
					org.apache.james.mime4j.dom.address.Mailbox from = SendmailHelper.formatAddress(user.displayName,
							user.value.defaultEmail().address);
					msg.setFrom(from);
				}
				String fromMail = msg.getFrom().iterator().next().getAddress();
				MailboxList rcptTo = allRecipients(msg);
				send(user.value.login, forSend, fromMail, rcptTo, msg);
				moveToSent(item, sentFolder, outboxFolder, flushResults);
				monitor.progress(1, "FLUSHING OUTBOX - mail " + msg.getMessageId() + " sent and moved in Sent folder.");
			} catch (Exception e) {
				throw new ServerFault(e);
			}
		});
	}

	private void send(String login, InputStream forSend, String fromMail, MailboxList rcptTo, Message relatedMsg)
			throws Exception {
		SendmailCredentials creds = SendmailCredentials.as(String.format("%s@%s", login, domainUid),
				context.getSecurityContext().getSessionId());
		SendmailResponse sendmailResponse = mailer.send(creds, fromMail, domainUid, rcptTo, forSend);

		if (!sendmailResponse.getFailedRecipients().isEmpty()) {
			sendNonDeliveryReport(sendmailResponse.getFailedRecipients(), creds, fromMail, relatedMsg);
		} else if (!sendmailResponse.isOk()) {
			throw new Exception(sendmailResponse.toString());
		} 
	}

	/**
	 * Move to default Sent folder or the one given in the X-BM-SENT-FOLDER header.
	 * Fall back to default Sent folder if an error occurs.
	 */
	private void moveToSent(ItemValue<MailboxItem> item, ItemValue<MailboxFolder> sentFolder,
			ItemValue<MailboxFolder> outboxFolder, List<FlushResult> flushResults) {
		Optional<String> xBmSentFolder = extractXBmSentFolder(item);
		FlushResult flushResult;
		if (xBmSentFolder.isPresent()) {
			try {
				flushResult = moveTo(item, outboxFolder.uid, xBmSentFolder.get());
			} catch (ServerFault e) {
				logger.warn(String.format(
						"Could not move sent messages to separate Sent folder %s, fall back to default Sent folder.",
						xBmSentFolder.get()));
				flushResult = moveTo(item, outboxFolder.uid, sentFolder.uid);
			}
		} else {
			flushResult = moveTo(item, outboxFolder.uid, sentFolder.uid);
		}

		if (flushResult != null) {
			flushResults.add(flushResult);
		}
	}

	private FlushResult moveTo(ItemValue<MailboxItem> item, String sourceUid, String targetUid) {
		IItemsTransfer itemsTransferService = serviceProvider.instance(IItemsTransfer.class, sourceUid, targetUid);
		List<ItemIdentifier> targetItems = itemsTransferService.move(Arrays.asList(item.internalId));
		if (targetItems == null || targetItems.isEmpty()) {
			return null;
		}
		FlushResult flushResult = new FlushResult();
		flushResult.setSourceInternalId(item.internalId);
		flushResult.setSourceFolderUid(sourceUid);
		flushResult.setDestinationInternalId(targetItems.get(0).id);
		flushResult.setDestinationFolderUid(targetUid);
		return flushResult;
	}

	private Optional<String> extractXBmSentFolder(ItemValue<MailboxItem> item) {
		return item.value.body.headers.stream().filter(header -> header.name.equalsIgnoreCase("X-BM-SENT-FOLDER"))
				.findFirst().map(MessageBody.Header::firstValue);
	}

	private void sendNonDeliveryReport(List<FailedRecipient> failedRecipients, SendmailCredentials creds, String sender,
			Message relatedMsg) {
		String from = "noreply@" + domainDefaultAlias();
		String toLocalPart = sender.split("@")[0];
		String toDomainPart = sender.split("@")[1];
		List<org.apache.james.mime4j.dom.address.Mailbox> rcpt = Arrays
				.asList(new org.apache.james.mime4j.dom.address.Mailbox(toLocalPart, toDomainPart));
		MailboxList rcptTo = new MailboxList(rcpt, true);

		MessageImpl message = createNonDeliveryReportMessage(failedRecipients, relatedMsg);
		mailer.send(creds, from, domainUid, rcptTo, message);
	}

	private MessageImpl createNonDeliveryReportMessage(List<FailedRecipient> failedRecipients, Message relatedMsg) {
		MessageImpl message = new MessageImpl();
		message.setSubject("Undelivered Mail Returned to Sender");
		String recipientsErrorMsg = "";
		for (int i = 0; i < failedRecipients.size(); i++) {
			FailedRecipient failedRcpt = failedRecipients.get(i);
			recipientsErrorMsg += "\r\n <" + failedRcpt.recipient + ">: " + failedRcpt.message;
		}
		String content = new StringBuilder().append("This is the mail system \n")
				.append("I'm sorry to have to inform you that your message \"").append(relatedMsg.getSubject())
				.append("\" could not\n").append("be delivered to one or more recipients. It's attached below.\n")
				.append("For further assistance, please send mail to postmaster.\n")
				.append("If you do so, please include this problem report. You can\n")
				.append("delete your own text from the attached returned message.\n").append("The mail system\n\n")
				.append(recipientsErrorMsg).toString();
		try {
			// text/plain part
			BodyPart bodyPart = new BodyPart();
			TextBody textBody = new BasicBodyFactory().textBody(content, "UTF-8");
			bodyPart.setText(textBody);

			// add related message as attachment
			BodyPart rfc822 = new BodyPart();
			Header header = new DefaultMessageBuilder().newHeader();
			header.setField(Fields.contentType("message/rfc822"));
			rfc822.setHeader(header);
			rfc822.setFilename(relatedMsg.getSubject() + ".eml");
			MessageWriter writer = MessageServiceFactory.newInstance().newMessageWriter();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			writer.writeMessage(relatedMsg, out);
			rfc822.setBody(new BasicBodyFactory().binaryBody(out.toByteArray()));

			Multipart mp = new MultipartImpl("report");
			mp.addBodyPart(bodyPart);
			mp.addBodyPart(rfc822);
			message.setMultipart(mp);
		} catch (IOException | MimeException e) {
			logger.error("cant build message to warn sender..", e);
		}
		return message;
	}

	private String domainDefaultAlias() {
		return serviceProvider.instance(IDomains.class).get(domainUid).value.defaultAlias;
	}

	private MailboxList allRecipients(Message m) {
		LinkedList<org.apache.james.mime4j.dom.address.Mailbox> rcpt = new LinkedList<>();
		AddressList tos = m.getTo();
		if (tos != null) {
			rcpt.addAll(tos.flatten());
		}
		AddressList ccs = m.getCc();
		if (ccs != null) {
			rcpt.addAll(ccs.flatten());
		}
		AddressList bccs = m.getBcc();
		if (bccs != null) {
			rcpt.addAll(bccs.flatten());
		}
		if (rcpt.isEmpty()) {
			throw new ServerFault("Empty recipients list.");
		}
		return new MailboxList(rcpt, true);
	}

	private List<ItemValue<MailboxItem>> retrieveOutboxItems(IMailboxItems mailboxItemsService) {
		SortDescriptor sortDescriptor = new SortDescriptor();
		Field mailDate = new Field();
		mailDate.column = "internal_date";
		mailDate.dir = Direction.Desc;
		sortDescriptor.fields = Arrays.asList(mailDate);

		List<Long> mailsIds = mailboxItemsService.sortedIds(sortDescriptor);
		return mailboxItemsService.multipleById(mailsIds).stream()
				.filter(item -> item.value.body.headers.stream()
						.anyMatch(header -> header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)))
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unused")
	private final class FlushResult {
		private long sourceInternalId;
		private String sourceFolderUid;
		private long destinationInternalId;
		private String destinationFolderUid;

		public long getSourceInternalId() {
			return sourceInternalId;
		}

		public void setSourceInternalId(long sourceInternalId) {
			this.sourceInternalId = sourceInternalId;
		}

		public String getSourceFolderUid() {
			return sourceFolderUid;
		}

		public void setSourceFolderUid(String sourceFolderUid) {
			this.sourceFolderUid = sourceFolderUid;
		}

		public long getDestinationInternalId() {
			return destinationInternalId;
		}

		public void setDestinationInternalId(long destinationInternalId) {
			this.destinationInternalId = destinationInternalId;
		}

		public String getDestinationFolderUid() {
			return destinationFolderUid;
		}

		public void setDestinationFolderUid(String destinationFolderUid) {
			this.destinationFolderUid = destinationFolderUid;
		}

	}

}
