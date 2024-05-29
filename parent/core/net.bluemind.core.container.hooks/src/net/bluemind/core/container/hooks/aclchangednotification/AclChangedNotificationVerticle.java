package net.bluemind.core.container.hooks.aclchangednotification;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.stream.RawField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.configfile.core.CoreConfig;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.hooks.aclchangednotification.AclDiff.AclStatus;
import net.bluemind.core.container.model.BaseContainerDescriptor;
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
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.user.api.IUserSettings;

public class AclChangedNotificationVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(AclChangedNotificationVerticle.class);
	private static final Queue<AclChangedMsg> ACL_CHANGED_MSGS = new ConcurrentLinkedQueue<>();
	public static final String ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS = "bm.acl.changed.notification.collect";
	public static final String ACL_CHANGED_NOTIFICATION_TEARDOWN_BUS_ADDRESS = "bm.acl.changed.notification.teardown";

	@Override
	public void start() throws Exception {
		int queueMaxSize = CoreConfig.get().getInt(CoreConfig.AclChangedNotification.QUEUE_SIZE);
		Duration delay = CoreConfig.get().getDuration(CoreConfig.AclChangedNotification.DELAY);
		vertx.setPeriodic(delay.toMillis(), d -> sendMessages());
		vertx.eventBus().consumer(ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS,
				(Message<LocalJsonObject<AclChangedMsg>> message) -> {
					try {
						ACL_CHANGED_MSGS.add(message.body().getValue());
						if (ACL_CHANGED_MSGS.size() == queueMaxSize) {
							sendMessages();
						}
					} catch (Exception e) {
						logger.error("Unable to add {} to the queue", AclChangedMsg.class.getName(), e);
					}
				});
		vertx.eventBus().consumer(ACL_CHANGED_NOTIFICATION_TEARDOWN_BUS_ADDRESS, m -> sendMessages());
	}

	private Collection<AclChangeInfo> processQueue() {
		Map<String, AclChangeInfo> aclChangeInfos = new HashMap<>();
		while (!ACL_CHANGED_MSGS.isEmpty()) {
			AclChangedMsg aclChangedMsg = ACL_CHANGED_MSGS.poll();
			aclChangedMsg.changes().stream().filter(acd -> isValidTargetUser(acd.subject(), aclChangedMsg.domainUid()))
					.forEach(acd -> {
						String key = AclChangeInfo.key(aclChangedMsg.domainUid(), aclChangedMsg.sourceUserId(),
								acd.subject());
						AclChangeInfo aclChangeInfo = aclChangeInfos.computeIfAbsent(key,
								k -> new AclChangeInfo(aclChangedMsg.domainUid(), aclChangedMsg.sourceUserId(),
										acd.subject(), aclChangedMsg.isItsOwnContainer()));
						NewVerbs newVerbs = aclChangeInfo.newVerbsByContainer
								.computeIfAbsent(aclChangedMsg.containerUid(),
										k -> new NewVerbs(aclChangedMsg.containerUid(), aclChangedMsg.containerName(),
												aclChangedMsg.containerType(),
												aclChangedMsg.containerOwnerDisplayname()));
						newVerbs.diffVerb = acd;
					});
		}
		return aclChangeInfos.values();
	}

	private static DirEntry fetchUserDirEntry(BmContext bmContext, String domainUid, String userUid) {
		return bmContext.provider().instance(IDirectory.class, domainUid).findByEntryUid(userUid);
	}

	private static Optional<String> fetchUserDomainUid(BmContext bmContext, String domainUid, String userUid) {
		if ("global.virt".equals(domainUid)) {
			IContainers containerService = bmContext.provider().instance(IContainers.class);
			try {
				BaseContainerDescriptor targetUserContainer = containerService
						.getLight(IMailboxAclUids.uidForMailbox(userUid));
				return Optional.ofNullable(targetUserContainer.domainUid);
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.NOT_FOUND) {
					return Optional.empty();
				}
				throw e;
			}
		}
		return Optional.of(domainUid);
	}

	private static boolean isValidTargetUser(String targetUserUid, String domainUid) {
		BmContext bmContext = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		Optional<String> domainUserUid = fetchUserDomainUid(bmContext, domainUid, targetUserUid);
		if (domainUserUid.isPresent()) {
			DirEntry targetUser = fetchUserDirEntry(bmContext, domainUserUid.get(), targetUserUid);
			return targetUser != null && targetUser.email != null && targetUser.displayName != null;
		}
		return false;
	}

	private void sendMessages() {
		processQueue().forEach(aclChangeInfo -> {
			try {
				toMails(aclChangeInfo).forEach(mail -> {
					vertx.eventBus().publish(SendMailAddress.SEND, new LocalJsonObject<>(mail));
				});
			} catch (IOException | TemplateException e) {
				logger.error("Unable to send ACL changed messages", e);
			}
		});
	}

	private List<Mail> toMails(AclChangeInfo aclChangeInfo) throws IOException, TemplateException {
		Mail m = new Mail();
		BmContext bmContext = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		IDirectory dirService = bmContext.provider().instance(IDirectory.class, aclChangeInfo.domainUid);
		Optional<String> userDomainUid = fetchUserDomainUid(bmContext, aclChangeInfo.domainUid,
				aclChangeInfo.targetUserId);
		if (userDomainUid.isEmpty()) {
			return Collections.emptyList();
		}
		DirEntry targetUser = fetchUserDirEntry(bmContext, userDomainUid.get(), aclChangeInfo.targetUserId);
		DirEntry sourceUser = dirService.findByEntryUid(aclChangeInfo.sourceUserId);

		if (targetUser.email == null || sourceUser == null) {
			return Collections.emptyList();
		}

		Mailbox from = buildFrom(aclChangeInfo.domainUid, targetUser);
		m.from = from;
		m.sender = from;
		m.to = SendmailHelper.formatAddress(targetUser.displayName, targetUser.email);

		String targetDomainUid = getTargetDomainUid(aclChangeInfo.domainUid, userDomainUid.get());
		IUserSettings settingService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUserSettings.class, targetDomainUid);
		String lang = settingService.get(targetUser.entryUid).get("lang");
		Locale locale = lang == null || lang.isEmpty() ? Locale.ENGLISH : Locale.forLanguageTag(lang);
		MessagesResolver resolver = new MessagesResolver(
				ResourceBundle.getBundle("OSGI-INF/l10n/aclChangedNotification", locale));

		m.subject = resolver.translate("subject", new Object[] { getActorName(sourceUser, resolver) });

		Map<String, Object> ftlDatas = prepareFtlData(aclChangeInfo, sourceUser, lang, resolver, m);

		m.html = applyTemplate(ftlDatas, locale);

		List<Mail> mailsForExpandedGroup = buildMailsForExpandedGroup(bmContext, targetDomainUid, targetUser,
				dirService, m);
		return mailsForExpandedGroup.isEmpty() ? Collections.singletonList(m) : mailsForExpandedGroup;
	}

	private Map<String, Object> prepareFtlData(AclChangeInfo aclChangeInfo, DirEntry sourceUser, String lang,
			MessagesResolver resolver, Mail m) throws IOException, TemplateException {

		Map<String, Object> ftlDatas = new HashMap<>();

		if (aclChangeInfo.hasVerbsWithStatus(AclStatus.ADDED)) {
			ftlDatas.putAll(prepareFtlDataHeaders(resolver, sourceUser, AclStatus.ADDED));
		}
		if (aclChangeInfo.hasVerbsWithStatus(AclStatus.REMOVED)) {
			ftlDatas.putAll(prepareFtlDataHeaders(resolver, sourceUser, AclStatus.REMOVED));
		}
		if (aclChangeInfo.hasVerbsWithStatus(AclStatus.UPDATED)) {
			ftlDatas.putAll(prepareFtlDataHeaders(resolver, sourceUser, AclStatus.UPDATED));
		}

		Map<String, String> folderUidType = new HashMap<>();

		aclChangeInfo.newVerbsByContainer.values().stream() //
				.filter(newVerbs -> newVerbs.diffVerb.newVerb() != null || newVerbs.diffVerb.oldVerb() != null) //
				.forEach(newVerbs -> {
					buildFtlData(ftlDatas, newVerbs, resolver, lang,
							aclChangeInfo.isItsOwnContainer ? "default" : "other");
					addContainerTypeHeader(m, newVerbs, folderUidType);
				});

		List<RawField> list = folderUidType.entrySet().stream().map(e -> e.getKey() + "; type=" + e.getValue())
				.map(h -> new RawField("X-BM-FolderUid", h)).toList();
		m.headers.addAll(list);

		return ftlDatas;
	}

	private Map<String, Object> prepareFtlDataHeaders(MessagesResolver resolver, DirEntry sourceUser,
			AclStatus status) {

		Map<String, Object> ftlData = new HashMap<>();
		ftlData.put("desc", resolver.translate("desc", new Object[] { getActorName(sourceUser, resolver) }));
		switch (status) {
		case ADDED: {
			ftlData.put("appPermissionsAdd", new HashMap<>());
			ftlData.put("tableHeadAdd", resolver.translate("tableHead.add", null));
			break;
		}
		case REMOVED: {
			ftlData.put("appPermissionsDelete", new HashMap<>());
			ftlData.put("tableHeadDelete", resolver.translate("tableHead.delete", null));
			break;
		}
		case UPDATED: {
			ftlData.put("appPermissionsUpdate", new HashMap<>());
			ftlData.put("tableHeadUpdate", resolver.translate("tableHead.update", null));
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + status);
		}
		return ftlData;
	}

	private String getActorName(DirEntry sourceUser, MessagesResolver resolver) {
		return sourceUser.email.equals("admin0@global.virt") ? resolver.translate("user.admin", null)
				: sourceUser.displayName;
	}

	private static String getTargetDomainUid(String domainUid, String targetDomainUid) {
		return "global.virt".equals(domainUid) ? targetDomainUid : domainUid;
	}

	private String applyTemplate(Map<String, Object> ftlData, Locale locale) throws TemplateException, IOException {
		StringWriter sw = new StringWriter();
		Configuration cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
		cfg.setClassForTemplateLoading(this.getClass(), "/templates");
		cfg.getTemplate("AclChangedNotification.ftl", locale).process(ftlData, sw);
		sw.flush();
		return sw.toString();
	}

	private void buildFtlData(Map<String, Object> ftlData, NewVerbs newVerbs, MessagesResolver resolver, String lang,
			String targetKind) {

		String appPermissionsKeyName = newVerbs.getPermissionKeyName();

		Object[] params = new Object[] { newVerbs.containerOwnerDisplayname };
		String target = resolver.translate("target." + targetKind + "." + newVerbs.containerType, params);

		@SuppressWarnings("unchecked")
		Map<String, List<Permission>> appPermissions = (Map<String, List<Permission>>) ftlData
				.get(appPermissionsKeyName);
//		if (appPermissions == null) {
//			return;
//		}
		String app = resolver.translate("app." + newVerbs.containerType, null);
		List<Permission> permissions = appPermissions.computeIfAbsent(app, k -> new ArrayList<>());
		String oldlevel = newVerbs.diffVerb.oldVerb() != null
				? resolver.translate(newVerbs.diffVerb.oldVerb().name(), null)
				: null;
		String newlevel = newVerbs.diffVerb.newVerb() != null
				? resolver.translate(newVerbs.diffVerb.newVerb().name(), null)
				: null;
		Permission appPermission = new Permission(newlevel, oldlevel, target);
		permissions.add(appPermission);
	}

	public record Permission(String level, String oldlevel, String target) {
	}

	private void addContainerTypeHeader(Mail m, NewVerbs newVerbs, Map<String, String> folderUidType) {
		if (newVerbs.containerType.equals(IMailboxAclUids.TYPE)) {
			m.headers.add(new RawField("X-BM-MailboxSharing", newVerbs.containerUid));
		} else {
			folderUidType.put(newVerbs.containerUid, newVerbs.containerType);
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
		String noreply = (de != null && de.email != null) && de.email.contains("@")
				? "no-reply@" + de.email.split("@")[1]
				: "no-reply@" + domainUid;
		return SendmailHelper.formatAddress(noreply, noreply);
	}

	private class AclChangeInfo {
		String domainUid;
		String sourceUserId;
		String targetUserId;
		boolean isItsOwnContainer;

		Map<String, NewVerbs> newVerbsByContainer = new HashMap<>();

		public AclChangeInfo(String domainUid, String sourceUserId, String targetUserId, boolean isItsOwnContainer) {
			this.domainUid = domainUid;
			this.sourceUserId = sourceUserId;
			this.targetUserId = targetUserId;
			this.isItsOwnContainer = isItsOwnContainer;
		}

		static String key(String domainUid, String sourceUserId, String targetUserId) {
			return String.join("#", domainUid, sourceUserId, targetUserId);
		}

		public boolean hasVerbsWithStatus(AclStatus status) {
			return newVerbsByContainer.entrySet().stream().anyMatch(e -> e.getValue().diffVerb.status() == status);
		}

		@Override
		public String toString() {
			return "AclChangeInfo [domainUid=" + domainUid + ", sourceUserId=" + sourceUserId + ", targetUserId="
					+ targetUserId + ", isItsOwnContainer=" + isItsOwnContainer + ", newVerbsByContainer="
					+ newVerbsByContainer + "]";
		}
	}

	private class NewVerbs {
		String containerUid;
		String containerName;
		String containerOwnerDisplayname;
		String containerType;
		AclDiff diffVerb;

		public NewVerbs(String containerUid, String containerName, String containerType,
				String containerOwnerDisplayname) {
			this.containerUid = containerUid;
			this.containerName = containerName;
			this.containerType = containerType;
			this.containerOwnerDisplayname = containerOwnerDisplayname;
		}

		@Override
		public String toString() {
			return "NewVerbs [containerUid=" + containerUid + ", containerName=" + containerName + ", containerType="
					+ containerType + ", containerOwnerDisplayname=" + containerOwnerDisplayname + ", diffVerb="
					+ diffVerb.toString() + "]";
		}

		public String getPermissionKeyName() {
			switch (diffVerb.status()) {
			case ADDED: {
				return "appPermissionsAdd";
			}
			case UPDATED: {
				return "appPermissionsUpdate";
			}
			case REMOVED: {
				return "appPermissionsDelete";
			}
			default:
				throw new IllegalArgumentException("Unexpected value: " + diffVerb.status());
			}
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
