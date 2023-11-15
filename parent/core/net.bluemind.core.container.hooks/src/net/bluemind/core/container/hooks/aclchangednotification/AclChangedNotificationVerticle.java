package net.bluemind.core.container.hooks.aclchangednotification;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.configfile.core.CoreConfig;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Mail;
import net.bluemind.core.sendmail.SendMailAddress;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.i18n.labels.I18nLabels;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.user.api.IUserSettings;

public class AclChangedNotificationVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(AclChangedNotificationVerticle.class);
	private static final Queue<AclChangedMsg> ACL_CHANGED_MSGS = new ConcurrentLinkedQueue<>();;
	private static int queueMaxSize;
	public static final String ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS = "bm.acl.changed.notification.collect";
	public static final String ACL_CHANGED_NOTIFICATION_TEARDOWN_BUS_ADDRESS = "bm.acl.changed.notification.teardown";

	static {
		try {
			queueMaxSize = CoreConfig.get().getInt(CoreConfig.AclChangedNotification.QUEUE_SIZE);
		} catch (Exception e) {
			queueMaxSize = 65535;
		}
	}

	@Override
	public void start() throws Exception {
		Duration delay;
		try {
			delay = CoreConfig.get().getDuration(CoreConfig.AclChangedNotification.DELAY);
		} catch (Exception e) {
			delay = Duration.ofMinutes(1);
		}
		vertx.setPeriodic(delay.toMillis(), d -> sendMessages());
		vertx.eventBus().consumer(ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS,
				(Message<LocalJsonObject<AclChangedMsg>> message) -> {
					try {
						ACL_CHANGED_MSGS.add(message.body().getValue());
						if (ACL_CHANGED_MSGS.size() == queueMaxSize) {
							sendMessages();
						}
					} catch (Exception e) {
						logger.error("Unable to add " + AclChangedMsg.class.getName() + " to the queue.");
					}
				});
		vertx.eventBus().consumer(ACL_CHANGED_NOTIFICATION_TEARDOWN_BUS_ADDRESS, m -> sendMessages());
	}

	private Collection<AclChangeInfo> processQueue() {
		Map<String, AclChangeInfo> aclChangeInfos = new HashMap<>();
		while (!ACL_CHANGED_MSGS.isEmpty()) {
			AclChangedMsg aclChangedMsg = ACL_CHANGED_MSGS.poll();
			aclChangedMsg.diff().forEach(ace -> {
				String key = AclChangeInfo.key(aclChangedMsg.domainUid(), aclChangedMsg.sourceUserId(), ace.subject);
				AclChangeInfo aclChangeInfo = aclChangeInfos.computeIfAbsent(key,
						k -> new AclChangeInfo(aclChangedMsg.domainUid(), aclChangedMsg.sourceUserId(), ace.subject));
				NewVerbs newVerbs = aclChangeInfo.newVerbsByContainer.computeIfAbsent(aclChangedMsg.containerUid(),
						k -> new NewVerbs(aclChangedMsg.containerUid(), aclChangedMsg.containerName(),
								aclChangedMsg.containerType(), aclChangedMsg.defaultContainer()));
				newVerbs.verbs.add(ace.verb);
			});
		}
		return aclChangeInfos.values();
	}

	private void sendMessages() {
		processQueue().forEach(aclChangeInfo -> {
			try {
				toMails(aclChangeInfo).forEach(mail -> {
					vertx.eventBus().publish(SendMailAddress.SEND, new LocalJsonObject<>(mail));
				});
			} catch (IOException | TemplateException e) {
				logger.error("Unable to send ACL Changed messages.", e);
			}
		});
	}

	private List<Mail> toMails(AclChangeInfo aclChangeInfo) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Mail m = new Mail();
		BmContext bmContext = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		IDirectory dirService = bmContext.provider().instance(IDirectory.class, aclChangeInfo.domainUid);
		DirEntry targetUser = dirService.findByEntryUid(aclChangeInfo.targetUserId);
		DirEntry sourceUser = dirService.findByEntryUid(aclChangeInfo.sourceUserId);

		Mailbox from = buildFrom(aclChangeInfo.domainUid, targetUser);
		m.from = from;
		m.sender = from;
		m.to = SendmailHelper.formatAddress(targetUser.displayName, targetUser.email);

		IUserSettings settingService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, aclChangeInfo.domainUid);
		String lang = settingService.get(targetUser.entryUid).get("lang");
		Locale locale = lang == null || lang.isEmpty() ? Locale.ENGLISH : Locale.forLanguageTag(lang);
		MessagesResolver resolver = new MessagesResolver(ResourceBundle.getBundle("aclChangedNotification", locale));
		m.subject = resolver.translate("subject", new Object[] { sourceUser.displayName });

		Map<String, Object> ftlData = prepareFtlData(resolver, sourceUser);

		aclChangeInfo.newVerbsByContainer.values().forEach(newVerbs -> {
			buildFtlData(ftlData, newVerbs, resolver, lang, sourceUser);
			addContainerTypeHeader(m, newVerbs);
		});

		m.html = applyTemplate(ftlData, locale);

		List<Mail> mailsForExpandedGroup = buildMailsForExpandedGroup(bmContext, aclChangeInfo.domainUid, targetUser,
				dirService, m);
		return mailsForExpandedGroup.isEmpty() ? Collections.singletonList(m) : mailsForExpandedGroup;
	}

	private Map<String, Object> prepareFtlData(MessagesResolver resolver, DirEntry sourceUser) {
		Map<String, Object> ftlData = new HashMap<>();
		ftlData.put("appPermissions", new HashMap<>());
		ftlData.put("desc", resolver.translate("desc", new Object[] { sourceUser.displayName }));
		ftlData.put("tableHead", resolver.translate("tableHead", null));
		return ftlData;
	}

	private String applyTemplate(Map<String, Object> ftlData, Locale locale) throws TemplateNotFoundException,
			MalformedTemplateNameException, ParseException, TemplateException, IOException {
		StringWriter sw = new StringWriter();
		Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(this.getClass(), "/");
		cfg.getTemplate("AclChangedNotification.ftl", locale).process(ftlData, sw);
		sw.flush();
		return sw.toString();
	}

	private void buildFtlData(Map<String, Object> ftlData, NewVerbs newVerbs, MessagesResolver resolver, String lang,
			DirEntry sourceUser) {
		@SuppressWarnings("unchecked")
		Map<String, List<Permission>> appPermissions = (Map<String, List<Permission>>) ftlData.get("appPermissions");
		String containerName = I18nLabels.getInstance().translate(lang, newVerbs.containerName);
		String app = resolver.translate("app." + newVerbs.containerType, null);
		List<Permission> permissions = appPermissions.computeIfAbsent(app, k -> new ArrayList<>());
		String targetKind = newVerbs.containerIsDefault ? "default" : "other";
		Object[] params = new Object[] { containerName };
		String target = resolver.translate("target." + targetKind + "." + newVerbs.containerType, params);
		newVerbs.verbs.forEach(verb -> {
			String level = resolver.translate(verb.name(), null);
			Permission appPermission = new Permission(level, target);
			permissions.add(appPermission);
		});
	}

	public record Permission(String level, String target) {
	}

	private void addContainerTypeHeader(Mail m, NewVerbs newVerbs) {
		switch (newVerbs.containerType) {
		case IMailboxAclUids.TYPE:
			m.headers.add(new RawField("X-BM-MailboxSharing", newVerbs.containerUid));
			break;
		default:
			m.headers.add(new RawField("X-BM-FolderUid", newVerbs.containerUid));
			m.headers.add(new RawField("X-BM-FolderType", newVerbs.containerType));
		}
	}

	private List<Mail> buildMailsForExpandedGroup(BmContext bmContext, String domainUid, DirEntry targetUser,
			IDirectory dirService, Mail originalMail) {
		if (targetUser.kind == Kind.GROUP && (targetUser.email == null || targetUser.email.isEmpty())) {
			IGroup groupService = bmContext.provider().instance(IGroup.class, domainUid);

			List<Member> members = groupService.getExpandedUserMembers(targetUser.entryUid);
			return members.stream().map(member -> {
				Mail shallowCopy = null;
				try {
					shallowCopy = (Mail) originalMail.clone();
				} catch (CloneNotSupportedException e) {
					logger.error("Unexpected error while cloning Mail.");
				}
				DirEntry user = dirService.findByEntryUid(member.uid);
				shallowCopy.to = SendmailHelper.formatAddress(user.displayName, user.email);
				return shallowCopy;
			}).collect(Collectors.toList());
		} else {
			return Collections.emptyList();
		}
	}

	private Mailbox buildFrom(String domainUid, DirEntry de) {
		String noreply = de != null && de.email.contains("@") ? "no-reply@" + de.email.split("@")[1]
				: "no-reply@" + domainUid;
		return SendmailHelper.formatAddress(noreply, noreply);
	}

	private class AclChangeInfo {
		String domainUid;
		String sourceUserId;
		String targetUserId;

		Map<String, NewVerbs> newVerbsByContainer = new HashMap<>();

		public AclChangeInfo(String domainUid, String sourceUserId, String targetUserId) {
			this.domainUid = domainUid;
			this.sourceUserId = sourceUserId;
			this.targetUserId = targetUserId;
		}

		static String key(String domainUid, String sourceUserId, String targetUserId) {
			return String.join("#", domainUid, sourceUserId, targetUserId);
		}

		@Override
		public String toString() {
			return "AclChangeInfo [domainUid=" + domainUid + ", sourceUserId=" + sourceUserId + ", targetUserId="
					+ targetUserId + ", newVerbsByContainer=" + newVerbsByContainer + "]";
		}

	}

	private class NewVerbs {
		String containerUid;
		String containerName;
		String containerType;
		boolean containerIsDefault;
		Set<Verb> verbs = new HashSet<>();

		public NewVerbs(String containerUid, String containerName, String containerType, boolean containerIsDefault) {
			this.containerUid = containerUid;
			this.containerName = containerName;
			this.containerType = containerType;
			this.containerIsDefault = containerIsDefault;
		}

		@Override
		public String toString() {
			return "NewVerbs [containerUid=" + containerUid + ", containerName=" + containerName + ", containerType="
					+ containerType + ", containerIsDefault=" + containerIsDefault + ", verbs=" + verbs + "]";
		}

	}

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new AclChangedNotificationVerticle();
		}
	}

}
