package net.bluemind.milter.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Group;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;
import com.sendmail.jilter.JilterEOMActions;
import com.sendmail.jilter.JilterHandler;
import com.sendmail.jilter.JilterStatus;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.mailflow.api.ExecutionMode;
import net.bluemind.mailflow.api.MailRuleActionAssignment;
import net.bluemind.mailflow.api.MailflowRouting;
import net.bluemind.mailflow.common.api.SendingAs;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.mailflow.rbe.MailflowRuleEngine;
import net.bluemind.mailflow.rbe.RuleAction;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.milter.ClientContext;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.IMilterListener.Status;
import net.bluemind.milter.IMilterListenerFactory;
import net.bluemind.milter.MilterHeaders;
import net.bluemind.milter.MilterInstanceID;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterAction;
import net.bluemind.milter.action.MilterPreAction;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.mime4j.common.Mime4JHelper;

public class MilterHandler implements JilterHandler {

	private static final Logger logger = LoggerFactory.getLogger(MilterHandler.class);
	private static final Registry registry = MetricsRegistry.get();
	private static final IdFactory idFactory = new IdFactory(MetricsRegistry.get(), MilterHandler.class);

	static {
		logger.info("JMX stats registered.");
	}

	private MessageAccumulator accumulator;
	private boolean messageModified;

	private ArrayList<IMilterListener> listeners;

	public MilterHandler(Collection<IMilterListenerFactory> mlfc) {
		listeners = new ArrayList<>(mlfc.size());
		for (IMilterListenerFactory mlf : mlfc) {
			listeners.add(mlf.create());
		}
	}

	private JilterStatus getJilterStatus(IMilterListener.Status status) {
		switch (status) {
		case DISCARD:
			return JilterStatus.SMFIS_DISCARD;
		case REJECT:
			return JilterStatus.SMFIS_REJECT;
		case CONTINUE:
		default:
			return JilterStatus.SMFIS_CONTINUE;
		}
	}

	@Override
	public JilterStatus connect(String hostname, InetAddress hostaddr, Properties properties) {
		logger.debug("connect {} {}", hostname, hostaddr);
		accumulator = new MessageAccumulator();
		accumulator.connect(hostname, hostaddr, properties);
		return JilterStatus.SMFIS_CONTINUE;
	}

	@Override
	public JilterStatus helo(String helohost, Properties properties) {
		logger.debug("helo");
		accumulator.helo(properties);
		return JilterStatus.SMFIS_CONTINUE;
	}

	@Override
	public JilterStatus envfrom(String[] argv, Properties properties) {
		logger.debug("envfrom");
		accumulator.envfrom(argv, properties);

		IMilterListener.Status ret = IMilterListener.Status.CONTINUE;

		for (IMilterListener listener : listeners) {
			ret = listener.onEnvFrom(argv[0]);

			if (ret != IMilterListener.Status.CONTINUE) {
				break;
			}
		}

		return getJilterStatus(ret);
	}

	@Override
	public JilterStatus envrcpt(String[] argv, Properties properties) {
		logger.debug("envrcpt");
		accumulator.envrcpt(argv, properties);

		return forEachListener(listener -> {
			return listener.onEnvRcpt(argv[0]);
		});

	}

	private void forEachActions(JilterEOMActions eomActions) {
		UpdatedMailMessage modifiedMail = new UpdatedMailMessage(accumulator.getProperties(), accumulator.getMessage());
		if (accumulator.getMessage().getHeader().getField(MilterHeaders.HANDLED) == null) {
			MilterPreActionsRegistry.get().forEach(action -> applyPreAction(action, modifiedMail));
			logger.debug("Applied {} milter pre-actions", MilterPreActionsRegistry.get().size());

			int appliedActions = applyActions(modifiedMail);
			logger.debug("Applied {} milter actions", appliedActions);
			modifiedMail.newHeaders.add(new RawField(MilterHeaders.HANDLED, MilterInstanceID.get()));
			modifiedMail.newHeaders.add(new RawField(MilterHeaders.TIMESTAMP, Long.toString(MQ.clusterTime())));
		}

		applyMailModifications(eomActions, modifiedMail);
	}

	private void applyMailModifications(JilterEOMActions eomActions, UpdatedMailMessage modifiedMail) {
		if (!modifiedMail.bodyChangedBy.isEmpty()) {
			logger.debug("replacing body ({})", modifiedMail.bodyChangedBy);
			File out = null;
			try {
				out = File.createTempFile("milter", ".eml");

				try (OutputStream outStream = Files.newOutputStream(out.toPath(), StandardOpenOption.TRUNCATE_EXISTING,
						StandardOpenOption.WRITE)) {
					Mime4JHelper.serializeBody(modifiedMail.getBody(), outStream);
					FileChannel asChannel = FileChannel.open(out.toPath(), StandardOpenOption.READ);
					long fileLength = out.length();
					MappedByteBuffer asByteBuffer = asChannel.map(MapMode.READ_ONLY, 0, fileLength);
					// update mail Content-Type header
					eomActions.chgheader(FieldName.CONTENT_TYPE, 1,
							modifiedMail.getMessage().getHeader().getField(FieldName.CONTENT_TYPE).getBody());
					eomActions.replacebody(asByteBuffer);
					asChannel.close();
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			} finally {
				if (out != null) {
					out.delete();
				}
			}
		}

		if (!modifiedMail.removeHeaders.isEmpty()) {
			logger.debug("removing header ({})", modifiedMail.headerChangedBy);
			for (String header : modifiedMail.removeHeaders) {
				try {
					eomActions.chgheader(header, 1, null);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		if (!modifiedMail.newHeaders.isEmpty()) {
			logger.debug("adding header ({})", modifiedMail.headerChangedBy);
			for (RawField rf : modifiedMail.newHeaders) {
				try {
					eomActions.addheader(rf.getName(), rf.getBody());
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		modifiedMail.envelopSender.ifPresent(envelopSender -> updateEnvelopSender(eomActions, envelopSender));

		if (!modifiedMail.addRcpt.isEmpty()) {
			logger.debug("Add recipients {}", modifiedMail.addRcpt);
			modifiedMail.addRcpt.forEach(r -> {
				try {
					eomActions.addrcpt(r);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			});
		}

		if (!modifiedMail.removeRcpt.isEmpty()) {
			logger.debug("Remove recipients {}", modifiedMail.removeRcpt);
			modifiedMail.removeRcpt.forEach(r -> {
				try {
					eomActions.delrcpt(r);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			});
		}
	}

	private void updateEnvelopSender(JilterEOMActions eomActions, String envelopSender) {
		logger.debug("Update envelop sender from {} to {}", accumulator.getEnvelope().getSender(), envelopSender);
		try {
			eomActions.chgfrom(envelopSender);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void applyPreAction(MilterPreAction action, UpdatedMailMessage modifiedMail) {
		logger.debug("Executing pre-action {}", action.getIdentifier());
		try {
			messageModified = messageModified || action.execute(modifiedMail);
		} catch (RuntimeException e) {
			registry.counter(idFactory.name("preActionsFails")).increment();
			throw e;
		}
	}

	private int applyActions(UpdatedMailMessage modifiedMail) {
		Optional<Integer> executedActions = Optional.empty();

		try {
			executedActions = getSenderDomain(accumulator.getEnvelope().getSender())
					.map(d -> applyActions(d, MailflowRouting.OUTGOING, modifiedMail));

			if (!executedActions.isPresent()) {
				executedActions = getRecipientDomain(accumulator.getEnvelope().getRecipients())
						.map(d -> applyActions(d, MailflowRouting.INCOMING, modifiedMail));
			}
		} catch (Exception e) {
			logger.warn("Error while applying milter actions", e);
		}

		return executedActions.orElse(0);
	}

	private Integer applyActions(ItemValue<Domain> domain, MailflowRouting mailflowRouting,
			UpdatedMailMessage modifiedMail) {
		Integer executedActions = 0;

		IClientContext mailflowContext = new ClientContext(domain);
		List<MailRuleActionAssignment> storedRules = RuleAssignmentCache
				.getStoredRuleAssignments(mailflowContext, domain.uid).stream()
				.filter(rule -> rule.routing == mailflowRouting || rule.routing == MailflowRouting.ALL)
				.collect(Collectors.toList());

		List<RuleAction> matches = new MailflowRuleEngine(mailflowContext).evaluate(storedRules, toBmMessage());
		for (RuleAction ruleAction : matches) {
			executedActions++;
			ExecutionMode mode = executeAction(ruleAction, mailflowContext, modifiedMail);
			if (mode == ExecutionMode.STOP_AFTER_EXECUTION) {
				logger.debug("Stopping execution of Milter actions after ruleAssignment {}", ruleAction.assignment.uid);
				return executedActions;
			}
		}

		return executedActions;
	}

	private Optional<ItemValue<Domain>> getRecipientDomain(List<SmtpAddress> recipients) {
		if (recipients.isEmpty()) {
			logger.warn("No recipients found");
			return Optional.empty();
		}

		ItemValue<Domain> domain = DomainAliasCache.getDomain(recipients.get(0).getDomainPart());
		if (null == domain) {
			logger.warn("Cannot find domain/alias of recipient {}", recipients.get(0));
			return Optional.empty();
		}

		if (recipients.stream().map(recipient -> DomainAliasCache.getDomain(recipient.getDomainPart()))
				.anyMatch(d -> !d.uid.equals(domain.uid))) {
			logger.warn("Recipients are not in the same BlueMind domain {}", domain.uid);
			return Optional.empty();
		}

		return Optional.of(domain);
	}

	private Optional<ItemValue<Domain>> getSenderDomain(SmtpAddress sender) {
		return Optional.ofNullable(DomainAliasCache.getDomain(sender.getDomainPart()));
	}

	private net.bluemind.mailflow.common.api.Message toBmMessage() {
		net.bluemind.mailflow.common.api.Message msg = new net.bluemind.mailflow.common.api.Message();
		msg.sendingAs = new SendingAs();
		msg.sendingAs.from = accumulator.getMessage().getFrom().get(0).getAddress();
		if (null != accumulator.getMessage().getSender()) {
			msg.sendingAs.sender = accumulator.getMessage().getSender().getAddress();
		} else {
			msg.sendingAs.sender = msg.sendingAs.from;
		}
		AddressList to = accumulator.getMessage().getTo();
		if (null != to) {
			msg.to = addressListToEmail(to);
		}
		AddressList cc = accumulator.getMessage().getCc();
		if (null != cc) {
			msg.cc = addressListToEmail(cc);
		}

		msg.recipients = accumulator.getEnvelope().getRecipients().stream().map(r -> r.getEmailAddress())
				.collect(Collectors.toList());
		msg.subject = accumulator.getMessage().getSubject();
		return msg;
	}

	private List<String> addressListToEmail(AddressList addressList) {
		return addressList.stream().map(this::addressToEmail).flatMap(l -> Stream.of(l.toArray(new String[0])))
				.collect(Collectors.toList());
	}

	private List<String> addressToEmail(Address address) {
		List<String> addresses = new ArrayList<>();
		if (address instanceof Group) {
			Group group = (Group) address;
			group.getMailboxes().forEach(a -> addresses.add(a.getAddress()));
		} else {
			Mailbox mb = (Mailbox) address;
			addresses.add(mb.getAddress());
		}
		return addresses;
	}

	private ExecutionMode executeAction(RuleAction ruleAssignment, IClientContext mailflowContext,
			UpdatedMailMessage modifiedMail) {
		Optional<MilterAction> action = MilterActionsRegistry.get(ruleAssignment.assignment.actionIdentifier);
		if (!action.isPresent()) {
			logger.warn("Unable to find registered action {}", ruleAssignment.assignment.actionIdentifier);
		} else {
			logger.debug("Executing action {}", ruleAssignment.assignment.actionIdentifier);
			try {
				action.get().execute(modifiedMail, ruleAssignment.assignment.actionConfiguration,
						ruleAssignment.rule.data, mailflowContext);
			} catch (RuntimeException e) {
				registry.counter(idFactory.name("actionsFails")).increment();
				throw e;
			}
			messageModified = true;
		}
		return ruleAssignment.assignment.mode;
	}

	private JilterStatus forEachListener(Function<IMilterListener, IMilterListener.Status> func) {
		Status ret = IMilterListener.Status.CONTINUE;
		for (IMilterListener listener : listeners) {
			Status listenerRet = func.apply(listener);
			if (listenerRet != null) {
				ret = listenerRet;
			}

			if (ret != IMilterListener.Status.CONTINUE) {
				break;
			}
		}

		return getJilterStatus(ret);
	}

	@Override
	public JilterStatus header(String headerf, String headerv) {
		logger.debug("header");
		accumulator.header(headerf, headerv);

		return forEachListener(listener -> listener.onHeader(headerf, headerv));
	}

	@Override
	public JilterStatus eoh() {
		logger.debug("eoh");
		accumulator.eoh();

		return forEachListener(listener -> listener.onEoh());
	}

	@Override
	public JilterStatus body(ByteBuffer bodyp) {
		logger.debug("body");
		accumulator.body(bodyp);

		return forEachListener(listener -> listener.onBody(bodyp));
	}

	@Override
	public JilterStatus eom(JilterEOMActions eomActions, Properties properties) {
		logger.debug("eom");
		accumulator.done(properties);

		forEachActions(eomActions);

		JilterStatus ret = forEachListener(
				listener -> listener.onMessage(accumulator.getEnvelope(), accumulator.getMessage()));

		accumulator.reset();
		return ret;
	}

	@Override
	public JilterStatus abort() {
		logger.debug("abort");
		accumulator.reset();
		return JilterStatus.SMFIS_CONTINUE;
	}

	@Override
	public JilterStatus close() {
		logger.debug("close");
		accumulator.reset();
		return JilterStatus.SMFIS_CONTINUE;
	}

	@Override
	public int getSupportedProcesses() {
		int supported = PROCESS_CONNECT | PROCESS_BODY | PROCESS_ENVFROM | PROCESS_ENVRCPT | PROCESS_HEADER
				| PROCESS_HELO;
		logger.debug("supportedProcesses: {}", Integer.toBinaryString(supported));
		return supported;
	}

	@Override
	public int getRequiredModifications() {
		logger.debug("reqMods");

		return !messageModified ? SMFIF_NONE : SMFIF_CHGBODY;
	}

	public static void init() {
		// force static init
	}

}
