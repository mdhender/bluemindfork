package net.bluemind.delivery.rules;

import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition.contains;
import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition.equal;
import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition.exists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.dom.field.UnstructuredField;
import org.apache.james.mime4j.field.UnstructuredFieldImpl;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.flags.WellKnownFlags;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.lmtp.common.FreezableDeliveryContent;
import net.bluemind.delivery.lmtp.common.FreezableDeliveryContent.SerializedMessage;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.MailFilterRule.Trigger;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleAction;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionAddHeaders;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCategorize;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCopy;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionCustom;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionFollowUp;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMarkAsDeleted;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMarkAsImportant;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMarkAsRead;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionMove;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionPrioritize;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRedirect;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionRemoveHeaders;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionReply;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionSetFlags;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionTransfer;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionUncategorize;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionUnfollow;

public class RuleEngine {
	private static final Logger logger = LoggerFactory.getLogger(RuleEngine.class);

	private final IServiceProvider serviceProvider;
	private final ISendmail mailer;
	private final DeliveryContent originalContent;
	private final FieldValueProvider fieldValueProvider;
	private final ParameterValueProvider parameterValueProvider;
	private final MailboxVacationSendersCache.Factory vacationCacheFactory;

	private static final List<IMailFilterRuleCustomAction> CUSTOM_ACTIONS = load();

	private static List<IMailFilterRuleCustomAction> load() {
		RunnableExtensionLoader<IMailFilterRuleCustomAction> rel = new RunnableExtensionLoader<>();
		return rel.loadExtensionsWithPriority("net.bluemind.delivery.rules", "action", "action", "impl");

	}

	public RuleEngine(IServiceProvider serviceProvider, ISendmail mailer, DeliveryContent content,
			MailboxVacationSendersCache.Factory vacationCacheFactory) {
		this.serviceProvider = serviceProvider;
		this.mailer = mailer;
		this.originalContent = content;
		this.fieldValueProvider = new FieldValueMessageProvider(originalContent.message(), originalContent.size());
		this.parameterValueProvider = new ParameterValueCoreProvider(content.box(), serviceProvider);
		this.vacationCacheFactory = vacationCacheFactory;
	}

	private ResolvedBox originalBox() {
		return originalContent.box();
	}

	private MailboxRecord originalRecord() {
		return originalContent.mailboxRecord();
	}

	private Message originalMessage() {
		return originalContent.message();
	}

	private String originalSubtree() {
		return IMailReplicaUids.subtreeUid(originalBox().dom.uid, originalBox().mbox);
	}

	private String originalPartition() {
		return CyrusPartition.forServerAndDomain(originalBox().entry.dataLocation, originalBox().dom.uid).name;
	}

	public DeliveryContent apply(List<MailFilterRule> rules) {
		addVacationSpecificConditions(rules);
		return applyActionsOf(matchingRules(rules));
	}

	private void addVacationSpecificConditions(List<MailFilterRule> rules) {
		rules.stream() //
				.filter(rule -> MailFilterRule.Type.VACATION.equals(rule.type) && rule.active) //
				.findFirst() //
				.ifPresent(vacation -> {
					vacation.conditions.add(contains("from.email", Arrays.asList("noreply@", "no-reply@")).not());
					vacation.conditions.add(exists("headers.x-dspam-result").not());
					vacation.conditions.add(exists("headers.spam").not());
					vacation.conditions.add(contains("headers.precedence", Arrays.asList("bulk", "list")).not());
					vacation.conditions.add(equal(Arrays.asList("to.email", "cc.email", "bcc.email"),
							Arrays.asList("BM_DYNAMIC_ADDRESSES_ME")));
				});
	}

	private List<MailFilterRule> matchingRules(List<MailFilterRule> rules) {
		return rules.stream() //
				.filter(rule -> {
					boolean match = rule.active && rule.trigger == Trigger.IN
							&& rule.match(fieldValueProvider, parameterValueProvider);
					if (!match && rule.type == MailFilterRule.Type.VACATION) {
						vacationCacheFactory.clear(originalBox().mbox.uid);
					}
					return match;
				}).toList();
	}

	private DeliveryContent applyActionsOf(List<MailFilterRule> rules) {
		logger.info("[rules] applying {} rules on {}", rules.size(), originalContent);
		return rules.stream() //
				.sequential() //
				.reduce(originalContent, this::applyActionOf, (result1, result2) -> result2);
	}

	private DeliveryContent applyActionOf(DeliveryContent previousContent, MailFilterRule rule) {
		if (previousContent.isEmpty() || previousContent.stop()) {
			String cause = previousContent.isEmpty() ? "message discarded" : "previous action ask to stop";
			logger.info("[rules] stop applying rule on {}: {}", originalContent, cause);
			return previousContent;
		}
		logger.info("[rules] applying rule ({} actions) on {}", rule.actions.size(), originalContent);
		boolean isVacation = (rule.type == MailFilterRule.Type.VACATION);
		DeliveryContent nextContent = previousContent.withStop(rule.stop);
		return rule.actions.stream().sequential() //
				.reduce(nextContent, //
						(content, action) -> applyAction(content, action, isVacation), //
						(result1, result2) -> result2);
	}

	private DeliveryContent applyAction(DeliveryContent nextContent, MailFilterRuleAction action, boolean isVacation) {
		logger.info("[rules] applying rule action {} on {}", action.name, originalContent);
		return switch (action.name) {
		case ADD_HEADER -> addHeaders(nextContent, (MailFilterRuleActionAddHeaders) action);
		case CATEGORIZE -> addHeaders(nextContent, (MailFilterRuleActionCategorize) action);
		case COPY -> copy(nextContent, (MailFilterRuleActionCopy) action);
		case DISCARD -> nextContent.withoutMessage();
		case FOLLOW_UP -> followUp(nextContent, (MailFilterRuleActionFollowUp) action);
		case MARK_AS_DELETED -> setFlags(nextContent, (MailFilterRuleActionMarkAsDeleted) action);
		case MARK_AS_IMPORTANT -> setFlags(nextContent, (MailFilterRuleActionMarkAsImportant) action);
		case MARK_AS_READ -> setFlags(nextContent, (MailFilterRuleActionMarkAsRead) action);
		case MOVE -> move(nextContent, (MailFilterRuleActionMove) action);
		case PRIORITIZE -> addHeaders(nextContent, (MailFilterRuleActionPrioritize) action);
		case REDIRECT -> redirect(nextContent, (MailFilterRuleActionRedirect) action);
		case REMOVE_HEADERS -> removeHeaders(nextContent, (MailFilterRuleActionRemoveHeaders) action);
		case REPLY -> reply(nextContent, (MailFilterRuleActionReply) action, isVacation);
		case SET_FLAGS -> setFlags(nextContent, (MailFilterRuleActionSetFlags) action);
		case TRANSFER -> transfer(nextContent, (MailFilterRuleActionTransfer) action);
		case UNCATEGORIZE -> removeHeaders(nextContent, (MailFilterRuleActionUncategorize) action);
		case UNFOLLOW -> removeHeaders(nextContent, (MailFilterRuleActionUnfollow) action);
		case CUSTOM -> customAction(nextContent, (MailFilterRuleActionCustom) action);
		};
	}

	private DeliveryContent addHeaders(DeliveryContent nextContent, MailFilterRuleActionAddHeaders addHeaders) {
		addHeaders.headers.forEach((name, value) -> {
			RawField raw = new RawField(name, value);
			UnstructuredField parsed = UnstructuredFieldImpl.PARSER.parse(raw, DecodeMonitor.SILENT);
			logger.info("[rules] adding header {}:{} [{}]", name, value, nextContent);
			originalMessage().getHeader().addField(parsed);
		});
		return nextContent;
	}

	private DeliveryContent copy(DeliveryContent nextContent, MailFilterRuleActionCopy copy) {
		String subtree = originalSubtree();
		IDbReplicatedMailboxes treeApi = serviceProvider.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		ItemValue<MailboxReplica> copyFolder = treeApi.byReplicaName(copy.folder());
		String partition = originalPartition();
		try {
			FreezableDeliveryContent copiedContent = FreezableDeliveryContent.copy(nextContent);
			SerializedMessage serializedMessage = copiedContent.serializedMessage();

			logger.info("[rules] copying into {} [{}]", copyFolder.value, copiedContent.content());

			IDbMessageBodies bodiesUpload = serviceProvider.instance(IDbMessageBodies.class, partition);
			Stream stream = VertxStream.stream(Buffer.buffer(serializedMessage.buffer()));
			bodiesUpload.create(serializedMessage.guid(), stream);

			AppendTx appendTx = treeApi.prepareAppend(copyFolder.internalId, 1);
			MailboxRecord rec = new MailboxRecord();
			rec.conversationId = originalRecord().conversationId;
			rec.messageBody = originalRecord().messageBody;
			rec.imapUid = appendTx.imapUid;
			rec.modSeq = appendTx.modSeq;
			rec.flags = new ArrayList<>();
			rec.internalDate = new Date();
			rec.lastUpdated = rec.internalDate;
			rec.conversationId = rec.internalDate.getTime();
			IDbMailboxRecords recs = serviceProvider.instance(IDbMailboxRecords.class, copyFolder.uid);
			recs.create(rec.imapUid + ".", rec);
		} catch (IOException e) {
			logger.error("[rule] failed to serialize message for {}, skipping copy into {}", //
					nextContent, copy.folder());
		}
		return nextContent;
	}

	private DeliveryContent followUp(DeliveryContent nextContent, MailFilterRuleActionFollowUp followUp) {
		// TODO
		return nextContent;
	}

	private DeliveryContent setFlags(DeliveryContent nextContent, MailFilterRuleActionSetFlags setFlags) {
		originalRecord().flags = new ArrayList<>(originalRecord().flags);
		setFlags.flags.forEach(flag -> {
			logger.info("[rules] flagging with {} [{}]", flag, nextContent);
			originalRecord().flags.add(WellKnownFlags.resolve(flag));
		});
		setFlags.internalFlags.stream().filter(flag -> flag.equals("\\Expunged")).findFirst().ifPresent(flag -> {
			logger.info("[rules] flagging (internal) with {} [{}]", flag, nextContent);
			originalRecord().internalFlags.add(MailboxRecord.InternalFlag.expunged);
		});
		return nextContent;
	}

	private DeliveryContent move(DeliveryContent nextContent, MailFilterRuleActionMove move) {
		String subtree = originalSubtree();
		IDbReplicatedMailboxes treeApi = serviceProvider.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		ItemValue<MailboxReplica> newFolder = treeApi.byReplicaName(move.folder());
		logger.info("[rules] moving to {} [{}]", move.folder(), nextContent);
		return nextContent.withFolder(newFolder);
	}

	private DeliveryContent redirect(DeliveryContent nextContent, MailFilterRuleActionRedirect redirect) {
		List<Mailbox> mailboxes = redirect.emails().stream().map(email -> SendmailHelper.formatAddress(null, email))
				.toList();
		MailboxList to = new MailboxList(mailboxes, true);
		String from = originalMessage().getFrom().iterator().next().getAddress();
		logger.info("[rules] redirecting to {} (keep copy:{}) [{}]", stringify(mailboxes), redirect.keepCopy,
				nextContent);
		mailer.send(SendmailCredentials.asAdmin0(), from, originalBox().dom.uid, to, originalMessage());
		return (redirect.keepCopy) ? nextContent : nextContent.withoutMessage();
	}

	private DeliveryContent removeHeaders(DeliveryContent nextContent,
			MailFilterRuleActionRemoveHeaders removeHeaders) {
		removeHeaders.headerNames.forEach(name -> {
			logger.info("[rules] removing header {} [{}]", name, nextContent);
			originalMessage().getHeader().removeFields(name);
		});
		return nextContent;
	}

	private DeliveryContent reply(DeliveryContent nextContent, MailFilterRuleActionReply reply, boolean isVacation) {
		if (!isVacation) {
			return doReply(nextContent, reply);
		}
		MailboxVacationSendersCache recipients = vacationCacheFactory.get(originalBox().mbox.uid);
		AddressList addressList = originalMessage().getReplyTo();
		String sender = (addressList == null || addressList.isEmpty())
				? originalMessage().getFrom().stream().findFirst().map(Mailbox::getAddress).orElse(null)
				: addressList.flatten().stream().findFirst().map(Mailbox::getAddress).orElse(null);
		return recipients.ifMissingDoGetOrElseGet(sender, () -> {
			logger.info("[rules][vacation] must reply to {} [{}]", sender, nextContent);
			return doReply(nextContent, reply);
		}, () -> {
			logger.info("[rules][vacation] skip reply to {} [{}]", sender, nextContent);
			return nextContent;
		});
	}

	private DeliveryContent doReply(DeliveryContent nextContent, MailFilterRuleActionReply reply) {
		MessageCreator creator = new MessageCreator(originalBox(), originalMessage());
		Message replyMessage = creator.newMessageWithOriginalCited(originalMessage().getFrom(), "Re", reply.subject,
				reply.plainBody, reply.htmlBody, true);
		String sender = originalMessage().getFrom().stream().findFirst().map(Mailbox::getAddress).orElse(null);
		logger.info("[rules] replying to {} [{}]", sender, nextContent);
		mailer.send(SendmailCredentials.asAdmin0(), originalBox().dom.uid, replyMessage);

		return nextContent;
	}

	private DeliveryContent transfer(DeliveryContent nextContent, MailFilterRuleActionTransfer transfer) {
		List<Mailbox> mailboxes = transfer.emails.stream().map(email -> SendmailHelper.formatAddress(null, email))
				.toList();
		MailboxList to = new MailboxList(mailboxes, true);
		MessageCreator creator = new MessageCreator(originalBox(), originalMessage());
		Message transferMessage = (!transfer.asAttachment) //
				? creator.newMessageWithOriginalCited(to, "Fwd", null, "", "", false) //
				: creator.newMessageWithOriginalAttached(to);
		logger.info("[rules] transferring to {} (keep copy:{}, as attachment:{}) [{}]", stringify(mailboxes),
				transfer.keepCopy, transfer.asAttachment, nextContent);
		mailer.send(SendmailCredentials.asAdmin0(), originalBox().dom.uid, transferMessage);
		return (transfer.keepCopy) ? nextContent : nextContent.withoutMessage();
	}

	private DeliveryContent customAction(DeliveryContent nextContent, MailFilterRuleActionCustom custom) {
		return CUSTOM_ACTIONS.stream() //
				.filter(customAction -> custom.kind != null && custom.kind.equals(customAction.kind())) //
				.findFirst() //
				.map(customAction -> {
					logger.info("[rules] applying a custom action: kind:{}, parameters:{} [{}]", custom.kind,
							custom.parameters, nextContent);
					return customAction.applyTo(nextContent, custom);
				}) //
				.orElse(nextContent);
	}

	private String stringify(List<Mailbox> mailboxes) {
		return mailboxes.stream().map(Mailbox::getAddress).collect(Collectors.joining(","));
	}

}
