package net.bluemind.backend.mail.replica.service.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooks;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Communications.Email;
import net.bluemind.addressbook.api.VCardQuery;
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
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.sendmail.FailedRecipient;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.SendmailResponse;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
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
	private final Sanitizer sortDescSanitizer;

	private ISendmail mailer;

	public OutboxService(BmContext context, String domainUid, ItemValue<Mailbox> mailboxItem, ISendmail mailer) {
		this.context = context;
		this.domainUid = domainUid;
		this.mailboxItem = mailboxItem;
		this.serviceProvider = context.getServiceProvider();
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(mailboxItem.uid));
		this.sortDescSanitizer = new Sanitizer(context);

		this.mailer = mailer;
	}

	@Override
	public TaskRef flush() {
		rbac.check(Verb.Write.name());
		return serviceProvider.instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
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

			FlushContext ctx = new FlushContext(monitor, mailboxFoldersService, outboxFolder, sentFolder,
					mailboxItemsService, user);

			flushAll(monitor, mails, mailCount, user, ctx);
		}));
	}

	private void flushAll(IServerTaskMonitor monitor, List<ItemValue<MailboxItem>> mails, int mailCount, AuthUser user,
			FlushContext ctx) {
		List<CompletableFuture<FlushInfo>> promises = mails.stream().map(item -> flushOne(ctx, item))
				.collect(Collectors.toList());

		CompletableFuture.allOf(Iterables.toArray(promises, CompletableFuture.class)).thenRun(() -> {
			List<FlushResult> flushResults = promises.stream().map(this::safeGet) //
					.filter(ret -> ret.flushResult.isPresent()) //
					.map(ret -> ret.flushResult.get()) //
					.collect(Collectors.toList());
			Set<RecipientInfo> collectedRecipients = promises.stream().map(this::safeGet) //
					.flatMap(ret -> ret.collectedRecipients.stream()).collect(Collectors.toSet());
			int requestedDSNs = promises.stream().map(this::safeGet).map(fi -> fi.requestedDSN ? 1 : 0).reduce(0,
					(a, b) -> a + b);

			addRecipientsToCollectedContacts(user.uid, collectedRecipients);
			logger.info("[{}] flushed {}", context.getSecurityContext().getSubject(), mailCount);
			monitor.end(true, "FLUSHING OUTBOX finished successfully", String.format(
					"{\"result\": %s, \"requestedDSNs\": %d}", JsonUtils.asString(flushResults), requestedDSNs));
		}).exceptionally(e -> {
			monitor.end(false, "FLUSHING OUTBOX - finished in error", "{\"result\": \"" + e + "\"}");
			logger.error("FLUSHING OUTBOX - finished in error", e);
			return null;
		});
	}

	private CompletableFuture<FlushInfo> flushOne(FlushContext ctx, ItemValue<MailboxItem> item) {
		return SyncStreamDownload.read(ctx.mailboxItemsService.fetchComplete(item.value.imapUid)).thenApply(buf -> {
			InputStream in = new ByteBufInputStream(buf.duplicate());
			InputStream forSend = new ByteBufInputStream(buf);
			FlushInfo ret = new FlushInfo();
			try (Message msg = Mime4JHelper.parse(in)) {
				if (msg.getFrom() == null) {
					org.apache.james.mime4j.dom.address.Mailbox fromCtx = SendmailHelper
							.formatAddress(ctx.user.displayName, ctx.user.value.defaultEmail().address);
					msg.setFrom(fromCtx);
				}
				String fromMail = msg.getFrom().iterator().next().getAddress();
				MailboxList rcptTo = allRecipients(msg);
				SendmailResponse sendmailResponse = send(ctx.user.value.login, forSend, fromMail, rcptTo, msg,
						requestDSN(item.value));
				ret.requestedDSN = sendmailResponse.getRequestedDSNs() > 0;
				boolean moveToSent = !isMDN(item.value);
				ret.flushResult = moveToSent ? moveToSent(item, ctx.sentFolder, ctx.outboxFolder)
						: Optional.ofNullable(remove(item, ctx.outboxFolder));
				ret.collectedRecipients = rcptTo.stream()
						.map(rcpt -> new RecipientInfo(rcpt.getAddress(), rcpt.getName(), rcpt.getLocalPart()))
						.collect(Collectors.toSet());
				ctx.monitor.progress(1,
						String.format(
								"FLUSHING OUTBOX - mail %s sent"
										+ (moveToSent ? "and moved in Sent folder." : ". Requested DSN: %b"),
								msg.getMessageId(), ret.requestedDSN));
			} catch (Exception e) {
				logger.warn("ItemId {}: ", item.internalId, e);
				throw new ServerFault(e);
			}
			return ret;
		});
	}

	/**
	 * @return <code>true</code> if <code>mailboxItem</code> is a Message
	 *         Disposition Notification (rfc8098)
	 */
	private boolean isMDN(MailboxItem mailboxItem) {
		return "multipart/report".equalsIgnoreCase(mailboxItem.body.structure.mime)
				&& mailboxItem.body.structure.children.stream()
						.anyMatch(child -> child.mime.contains("/disposition-notification"));
	}

	/**
	 * @return <code>true</code> if a Delivery Status Notification is requested for
	 *         <code>mailboxItem</code> (rfc1891)
	 */
	private boolean requestDSN(MailboxItem mailboxItem) {
		return mailboxItem.flags.stream().anyMatch(itemFlag -> "BmDSN".equalsIgnoreCase(itemFlag.flag));
	}

	private void addRecipientsToCollectedContacts(String uid, Set<RecipientInfo> collectedRecipients) {
		IAddressBooks allContactsService = serviceProvider.instance(IAddressBooks.class);
		IAddressBook collectedContactsService = serviceProvider.instance(IAddressBook.class,
				IAddressBookUids.collectedContactsUserAddressbook(uid));
		String queryString = "value.kind: 'individual' AND value.communications.emails.value:";

		collectedRecipients.forEach(recipient -> {
			VCardQuery query = VCardQuery.create(queryString + recipient.email);
			query.escapeQuery = true;
			if (allContactsService.search(query).total == 0) {
				addRecipientToCollectedContacts(collectedContactsService, recipient);
			}
		});

	}

	private void addRecipientToCollectedContacts(IAddressBook service, RecipientInfo recipient) {
		VCard card = recipientToVCard(recipient);
		service.create(UUID.randomUUID().toString(), card);
	}

	private VCard recipientToVCard(RecipientInfo recipient) {
		VCard card = new VCard();

		card.identification.name = VCard.Identification.Name.create(recipient.familyNames, recipient.givenNames, null,
				null, null, null);

		List<Email> emails = Arrays.asList(VCard.Communications.Email.create(recipient.email));
		card.communications.emails = emails;
		return card;
	}

	private FlushInfo safeGet(CompletableFuture<FlushInfo> info) {
		try {
			return info.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private SendmailResponse send(String login, InputStream forSend, String fromEnvelop, MailboxList rcptTo,
			Message relatedMsg, boolean requestDSN) {
		SendmailCredentials creds = SendmailCredentials.as(String.format("%s@%s", login, domainUid),
				context.getSecurityContext().getSessionId());
		String from = creds.notAdminAndNotCurrentUser(fromEnvelop) ? creds.loginAtDomain : fromEnvelop;
		SendmailResponse sendmailResponse = mailer.send(creds, from, domainUid, rcptTo, forSend, requestDSN);

		if (!sendmailResponse.getFailedRecipients().isEmpty()) {
			sendNonDeliveryReport(sendmailResponse.getFailedRecipients(), creds, fromEnvelop, relatedMsg);
		} else if (!sendmailResponse.isOk()) {
			if (sendmailResponse.code == 503) {
				sendNonDeliveryAclReport(creds, fromEnvelop, relatedMsg);
			} else {
				throw new ServerFault(sendmailResponse.toString());
			}
		}

		return sendmailResponse;

	}

	/**
	 * Move to default Sent folder or the one given in the X-BM-SENT-FOLDER header.
	 * Fall back to default Sent folder if an error occurs.
	 */
	private Optional<FlushResult> moveToSent(ItemValue<MailboxItem> item, ItemValue<MailboxFolder> sentFolder,
			ItemValue<MailboxFolder> outboxFolder) {
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

		return Optional.ofNullable(flushResult);
	}

	private FlushResult moveTo(ItemValue<MailboxItem> item, String sourceUid, String targetUid) {
		IItemsTransfer itemsTransferService = serviceProvider.instance(IItemsTransfer.class, sourceUid, targetUid);
		List<ItemIdentifier> targetItems = itemsTransferService.move(Arrays.asList(item.internalId));
		if (targetItems == null || targetItems.isEmpty()) {
			return null;
		}
		return buildFlushResult(item.internalId, sourceUid, targetItems.get(0).id, targetUid);
	}

	private FlushResult remove(ItemValue<MailboxItem> item, ItemValue<MailboxFolder> outboxFolder) {
		IMailboxItems mailboxItemsService = serviceProvider.instance(IMailboxItems.class, outboxFolder.uid);
		mailboxItemsService.deleteById(item.internalId);
		return buildFlushResult(item.internalId, outboxFolder.uid, item.internalId, outboxFolder.uid);
	}

	private FlushResult buildFlushResult(long sourceInternalId, String sourceFolderUid, long targetInternalId,
			String targetFolderUid) {
		FlushResult flushResult = new FlushResult();
		flushResult.setSourceInternalId(sourceInternalId);
		flushResult.setSourceFolderUid(sourceFolderUid);
		flushResult.setDestinationInternalId(targetInternalId);
		flushResult.setDestinationFolderUid(targetFolderUid);
		return flushResult;
	}

	private Optional<String> extractXBmSentFolder(ItemValue<MailboxItem> item) {
		return item.value.body.headers.stream().filter(header -> header.name.equalsIgnoreCase("X-BM-SENT-FOLDER"))
				.findFirst().map(MessageBody.Header::firstValue);
	}

	private void sendNonDeliveryReport(List<FailedRecipient> failedRecipients, SendmailCredentials creds, String sender,
			Message relatedMsg) {
		MessageImpl message = createNonDeliveryReportMessage(failedRecipients, relatedMsg);
		sendIt(creds, sender, message);
	}

	private void sendNonDeliveryAclReport(SendmailCredentials creds, String sender, Message relatedMsg) {
		MessageImpl message = createNonDeliveryAclReportMessage(relatedMsg, sender,
				creds.notAdminAndNotCurrentUser(sender) ? creds.loginAtDomain : sender);
		sendIt(creds, sender, message);
	}

	private void sendIt(SendmailCredentials creds, String sender, MessageImpl message) {
		String from = "noreply@" + domainDefaultAlias();
		String to = creds.notAdminAndNotCurrentUser(sender) ? creds.loginAtDomain : sender;
		String toLocalPart = to.split("@")[0];
		String toDomainPart = to.split("@")[1];
		List<org.apache.james.mime4j.dom.address.Mailbox> rcpt = Arrays
				.asList(new org.apache.james.mime4j.dom.address.Mailbox(toLocalPart, toDomainPart));
		MailboxList rcptTo = new MailboxList(rcpt, true);

		mailer.send(creds, from, domainUid, rcptTo, message);
	}

	private MessageImpl createNDRMessageImpl(String content, String subject, Message relatedMsg) {
		MessageImpl message = new MessageImpl();
		message.setSubject(subject);

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

	private MessageImpl createNonDeliveryAclReportMessage(Message relatedMsg, String to, String sender) {
		StringBuilder content = new StringBuilder().append("This is the mail system \n")
				.append("I'm sorry to have to inform you that your message \"").append(relatedMsg.getSubject())
				.append("\" could not\n").append("be delivered because of insufficient delegation rights.\n");

		if (!sender.equals(to)) {
			content.append("\n" + sender + " has no sufficient delegation rights to send messages using " + to
					+ " email address. \n");
		}
		content.append("The mail system\n\n");

		return createNDRMessageImpl(content.toString(), "Undelivered Mail Returned to Sender", relatedMsg);
	}

	private MessageImpl createNonDeliveryReportMessage(List<FailedRecipient> failedRecipients, Message relatedMsg) {
		StringBuilder recipientsErrorMsg = new StringBuilder();
		for (int i = 0; i < failedRecipients.size(); i++) {
			FailedRecipient failedRcpt = failedRecipients.get(i);
			recipientsErrorMsg.append("\r\n <" + failedRcpt.recipient + ">: " + failedRcpt.message);
		}
		String content = new StringBuilder().append("This is the mail system \n")
				.append("I'm sorry to have to inform you that your message \"").append(relatedMsg.getSubject())
				.append("\" could not\n").append("be delivered to one or more recipients. It's attached below.\n")
				.append("For further assistance, please send mail to postmaster.\n")
				.append("If you do so, please include this problem report. You can\n")
				.append("delete your own text from the attached returned message.\n").append("The mail system\n\n")
				.append(recipientsErrorMsg).toString();

		return createNDRMessageImpl(content, "Undelivered Mail Returned to Sender", relatedMsg);
	}

	private String domainDefaultAlias() {
		return serviceProvider.instance(IDomains.class).get(domainUid).value.defaultAlias;
	}

	private MailboxList allRecipients(Message m) throws Exception {
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
			throw new Exception("Empty recipients list.");
		}
		return new MailboxList(rcpt, true);
	}

	private List<ItemValue<MailboxItem>> retrieveOutboxItems(IMailboxItems mailboxItemsService) {
		SortDescriptor sortDescriptor = new SortDescriptor();
		sortDescSanitizer.create(sortDescriptor);

		List<Long> mailsIds = mailboxItemsService.sortedIds(sortDescriptor);
		return mailboxItemsService.multipleGetById(mailsIds).stream()
				.filter(item -> item.value.body.headers.stream()
						.anyMatch(header -> header.name.equals(MailApiHeaders.X_BM_DRAFT_REFRESH_DATE)))
				.collect(Collectors.toList());
	}

	static class RecipientInfo {
		final String email;
		final String givenNames;
		final String familyNames;

		public RecipientInfo(String email, String fullName, String localPart) {
			this.email = email;

			String[] names;
			if (fullName == null) {
				names = Arrays.asList(localPart.split("\\.")).stream().map(this::captitalize).toArray(String[]::new);
			} else {
				names = fullName.split(" ");
			}
			this.givenNames = names[0];
			this.familyNames = String.join(" ", Arrays.copyOfRange(names, 1, names.length));
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((email == null) ? 0 : email.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RecipientInfo other = (RecipientInfo) obj;
			if (email == null) {
				if (other.email != null)
					return false;
			} else if (!email.equals(other.email))
				return false;
			return true;
		}

		private String captitalize(String str) {
			return Character.toUpperCase(str.charAt(0)) + str.substring(1);
		}

	}

	static class FlushContext {
		final IServerTaskMonitor monitor;
		final IMailboxFolders mailboxFoldersService;
		final ItemValue<MailboxFolder> outboxFolder;
		final ItemValue<MailboxFolder> sentFolder;
		final IMailboxItems mailboxItemsService;
		final AuthUser user;

		public FlushContext(IServerTaskMonitor monitor, IMailboxFolders mailboxFoldersService,
				ItemValue<MailboxFolder> outboxFolder, ItemValue<MailboxFolder> sentFolder,
				IMailboxItems mailboxItemsService, AuthUser user) {
			this.monitor = monitor;
			this.mailboxFoldersService = mailboxFoldersService;
			this.outboxFolder = outboxFolder;
			this.sentFolder = sentFolder;
			this.mailboxItemsService = mailboxItemsService;
			this.user = user;
		}
	}

	static class FlushInfo {
		public boolean requestedDSN;
		Optional<FlushResult> flushResult;
		Set<RecipientInfo> collectedRecipients;
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
