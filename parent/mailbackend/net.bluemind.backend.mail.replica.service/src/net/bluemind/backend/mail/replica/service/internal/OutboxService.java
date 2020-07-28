package net.bluemind.backend.mail.replica.service.internal;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.james.mime4j.dom.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import io.netty.buffer.ByteBufInputStream;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFoldersByContainer;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.IOutbox;
import net.bluemind.backend.mail.api.ImportMailboxItemSet;
import net.bluemind.backend.mail.api.ImportMailboxItemSet.MailboxItemId;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus;
import net.bluemind.backend.mail.api.ImportMailboxItemsStatus.ImportedMailboxItem;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.utils.JsonUtils;
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
		return serviceProvider.instance(ITasksManager.class).run(this::send);
	}

	public void send(IServerTaskMonitor monitor) {
		IMailboxFolders mailboxFoldersService = serviceProvider.instance(IMailboxFoldersByContainer.class,
				IMailReplicaUids.subtreeUid(domainUid, mailboxItem));

		ItemValue<MailboxFolder> outboxFolder = mailboxFoldersService.byName("Outbox");
		String outboxUid = outboxFolder.uid;
		long outboxInternalId = outboxFolder.internalId;
		long sentInternalId = mailboxFoldersService.byName("Sent").internalId;

		IMailboxItems mailboxItemsService = serviceProvider.instance(IMailboxItems.class, outboxUid);
		List<ItemValue<MailboxItem>> mails = retrieveOutboxItems(mailboxItemsService);
		monitor.begin(mails.size(), "FLUSHING OUTBOX - have " + mails.size() + " mails to send.");

		AuthUser user = serviceProvider.instance(IAuthentication.class).getCurrentUser();

		List<CompletableFuture<Void>> promises = new ArrayList<>();
		final List<ImportedMailboxItem> importedMailboxItems = new ArrayList<>(mails.size());
		mails.forEach(item -> {
			promises.add(
					SyncStreamDownload.read(mailboxItemsService.fetchComplete(item.value.imapUid)).thenAccept(buf -> {
						InputStream in = new ByteBufInputStream(buf);
						try (Message msg = Mime4JHelper.parse(in)) {

							if (msg.getFrom() == null) {
								org.apache.james.mime4j.dom.address.Mailbox from = SendmailHelper
										.formatAddress(user.displayName, user.value.defaultEmail().address);
								msg.setFrom(from);
							}

							mailer.send(SendmailCredentials.as(String.format("%s@%s", user.value.login, domainUid),
									context.getSecurityContext().getSessionId()), domainUid, msg);

							final ImportMailboxItemsStatus importMailboxItemsStatus = mailboxFoldersService
									.importItems(sentInternalId, ImportMailboxItemSet.moveIn(outboxInternalId,
											Arrays.asList(MailboxItemId.of(item.internalId)), null));
							final List<ImportedMailboxItem> doneIds = importMailboxItemsStatus.doneIds;
							if (doneIds != null && !doneIds.isEmpty()) {
								importedMailboxItems.addAll(doneIds);
							}
							monitor.progress(1,
									"FLUSHING OUTBOX - mail " + msg.getMessageId() + " sent and moved in Sent folder.");
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}));
		});

		try {
			CompletableFuture.allOf(Iterables.toArray(promises, CompletableFuture.class)).thenAccept(finished -> {
				monitor.end(true, "FLUSHING OUTBOX finished successfully",
						String.format("{\"result\": %s}", JsonUtils.asString(importedMailboxItems)));
			}).get(30, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			monitor.end(false, "FLUSHING OUTBOX - timeout reached", "{\"result\": \"" + e + "\"}");
			logger.warn("FLUSHING OUTBOX - timeout reached", e);
		} catch (Exception e) {
			monitor.end(false, "FLUSHING OUTBOX - finished in error", "{\"result\": \"" + e + "\"}");
			logger.error("FLUSHING OUTBOX - finished in error", e);
		}
	}

	private List<ItemValue<MailboxItem>> retrieveOutboxItems(IMailboxItems mailboxItemsService) {
		SortDescriptor sortDescriptor = new SortDescriptor();
		Field mailDate = new Field();
		mailDate.column = "internal_date";
		mailDate.dir = Direction.Desc;
		sortDescriptor.fields = Arrays.asList(mailDate);

		List<Long> mailsIds = mailboxItemsService.sortedIds(sortDescriptor);
		return mailboxItemsService.multipleById(mailsIds);
	}

}
