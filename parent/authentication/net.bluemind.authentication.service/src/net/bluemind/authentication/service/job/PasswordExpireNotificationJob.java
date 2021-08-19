package net.bluemind.authentication.service.job;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.AbstractEntity;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import net.bluemind.common.freemarker.FreeMarkerMsg;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.user.api.IUserSettings;

public class PasswordExpireNotificationJob implements IScheduledJob {
	private static final Logger logger = LoggerFactory.getLogger(PasswordExpireNotificationJob.class);

	public static final String JID = "net.bluemind.authentication.service.job.PasswordExpireNotificationJob";

	private IScheduler scheduler;
	private IScheduledJobRunId rid;

	private final List<Integer> notificationIntervals = Arrays.asList(1, 2, 3, 4, 5, 10);

	private String domainName;
	private Report report;

	private class Report {
		private class IntervalReport {
			public Integer usersToNotify = 0;
			public Integer usersNotifiedByMail = 0;
			public Integer usersNotNotifiedByMail = 0;
			public Integer usersNotificationError = 0;
		}

		public JobExitStatus jobExitStatus = JobExitStatus.SUCCESS;
		public Map<Integer, IntervalReport> reportByInterval = notificationIntervals.stream()
				.collect(Collectors.toMap(ni -> ni, ni -> new IntervalReport()));

		public JobExitStatus getStatus() {
			if (jobExitStatus == JobExitStatus.FAILURE) {
				return jobExitStatus;
			}

			if (reportByInterval.values().stream().anyMatch(report -> report.usersNotificationError != 0)) {
				return JobExitStatus.COMPLETED_WITH_WARNINGS;
			}

			return jobExitStatus;
		}
	}

	@Override
	public void tick(IScheduler sched, boolean forced, String domainName, Date startDate) throws ServerFault {
		if ("global.virt".equals(domainName)) {
			return;
		}

		if (!forced) {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(startDate);
			if (gc.get(Calendar.MINUTE) != 0 || gc.get(Calendar.HOUR_OF_DAY) != 1) {
				return;
			}
		}

		logger.info("Run password expire notification job at: " + startDate.toString());

		rid = sched.requestSlot(domainName, this, startDate);
		if (rid == null) {
			return;
		}

		this.scheduler = sched;
		this.domainName = domainName;

		Optional<Integer> passwordLifetime = getDomainPasswordExpirationSetting(domainName);
		if (!passwordLifetime.isPresent()) {
			sched.info(rid, "en", String.format("Password expiration is not set on domain %s", domainName));
			sched.info(rid, "fr",
					String.format("L'expiration du mot de passe n'est pas configuré pour le domaine %s", domainName));
			sched.finish(rid, JobExitStatus.SUCCESS);
			return;
		}

		report = new Report();
		notificationIntervals.stream()
				.forEach(notificationInterval -> process(passwordLifetime.get(), notificationInterval));
		report.reportByInterval.entrySet().stream().forEach(this::logReport);
		sched.finish(rid, report.getStatus());
	}

	private void logReport(Map.Entry<Integer, PasswordExpireNotificationJob.Report.IntervalReport> report) {
		scheduler.info(rid, "en", String.format(
				"%d passwords expire in %d days - %d notified by mail, %d not notified by mail, %d notifications in error",
				report.getValue().usersToNotify, report.getKey(), report.getValue().usersNotifiedByMail,
				report.getValue().usersNotNotifiedByMail, report.getValue().usersNotificationError));
		scheduler.info(rid, "fr", String.format(
				"%d mot de passe expir(ent) dans %d days - %d notifé(s) par mail, %d non notifié(s) par mail, %d notification(s) en erreur",
				report.getValue().usersToNotify, report.getKey(), report.getValue().usersNotifiedByMail,
				report.getValue().usersNotNotifiedByMail, report.getValue().usersNotificationError));
	}

	private void process(Integer passwordLifetime, Integer notificationInterval) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		Set<String> userUids;
		try {
			userUids = getUserUids(provider, passwordLifetime, notificationInterval);
		} catch (SQLException e) {
			scheduler.error(rid, "en", String.format("Unable to request database: %s", e.getMessage()));
			scheduler.error(rid, "fr", String.format("Impossible de requêter la base de données: %s", e.getMessage()));
			logger.error("Unable to request database", e);
			report.jobExitStatus = JobExitStatus.FAILURE;
			return;
		}

		report.reportByInterval.get(notificationInterval).usersToNotify = userUids.size();

		Optional<String> externalUrl = Optional.ofNullable(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(ISystemConfiguration.class).getValues().values.get(SysConfKeys.external_url.name()));
		userUids.stream().forEach(userUid -> processUser(provider, externalUrl, notificationInterval, userUid));
	}

	private void processUser(ServerSideServiceProvider provider, Optional<String> externalUrl,
			Integer notificationInterval, String userUid) {
		DirEntry dirEntry;
		try {
			dirEntry = provider.instance(IDirectory.class, domainName).findByEntryUid(userUid);
		} catch (ServerFault sf) {
			scheduler.error(rid, "en", String.format("Unable get user UID: %s, %s", userUid, sf.getMessage()));
			scheduler.error(rid, "fr", String.format("Utilisateur UID: %s non trouvé: %s", userUid, sf.getMessage()));
			logger.error("Unable get user UID: {}", userUid, sf);
			return;
		}

		if (Strings.isNullOrEmpty(dirEntry.email)) {
			report.reportByInterval.get(notificationInterval).usersNotNotifiedByMail++;
			scheduler.info(rid, "en", String.format(
					"No email set for user '%s', mail notification not sent. Password will expire in %d day(s).",
					Strings.isNullOrEmpty(dirEntry.displayName) ? dirEntry.entryUid : dirEntry.displayName,
					notificationInterval));
			scheduler.info(rid, "fr", String.format(
					"L'utilisateur '%s' n'a pas d'email, pas de notification par mail envoyée. Le mot de passe expirera dans %s jour(s)",
					Strings.isNullOrEmpty(dirEntry.displayName) ? dirEntry.entryUid : dirEntry.displayName,
					notificationInterval));
			return;
		}

		String lang = null;
		try {
			lang = provider.instance(IUserSettings.class, domainName).get(userUid).get("lang");
		} catch (ServerFault sf) {
		}

		Locale locale = Strings.isNullOrEmpty(lang) ? Locale.ENGLISH : new Locale(lang);

		try {
			sendNotification(externalUrl, notificationInterval, dirEntry, locale);
			report.reportByInterval.get(notificationInterval).usersNotifiedByMail++;
		} catch (ServerFault | IOException | TemplateException | MimeException e) {
			report.reportByInterval.get(notificationInterval).usersNotificationError++;
			scheduler.error(rid, "en",
					String.format("Unable to send mail notification to: %s. Password will expire in %d day(s) - %s",
							Strings.isNullOrEmpty(dirEntry.email) ? dirEntry.entryUid : dirEntry.email,
							notificationInterval, e.getMessage()));
			scheduler.error(rid, "fr", String.format(
					"Erreur lors de la notification par mail de l'utilisateur: %s. Le mot de passe expirera dans %s jour(s) - %s",
					Strings.isNullOrEmpty(dirEntry.email) ? dirEntry.entryUid : dirEntry.email, notificationInterval,
					e.getMessage()));
			logger.error("Unable to send password notification to user UID: {}", userUid, e);
		}
	}

	private void sendNotification(Optional<String> externalUrl, Integer notificationInterval, DirEntry dirEntry,
			Locale locale) throws IOException, TemplateException, MimeException {
		Mailbox from = SendmailHelper.formatAddress("BlueMind", String.format("no-reply@%s", domainName));
		Mailbox recipient = SendmailHelper.formatAddress(dirEntry.displayName, dirEntry.email);

		MessagesResolver resolver = new MessagesResolver(ResourceBundle.getBundle("expireNotification", locale));

		Map<String, Object> data = new HashMap<>();
		data.put("msg", new FreeMarkerMsg(resolver));
		data.put("notificationInterval", notificationInterval);
		externalUrl.ifPresent(eu -> data.put("externalUrl", eu));

		Configuration cfg = new Configuration();
		cfg.setClassForTemplateLoading(this.getClass(), "/");

		StringWriter sw = new StringWriter();
		cfg.getTemplate("expireNotification.ftl", locale).process(data, sw);
		sw.flush();

		BodyPart body = createTextPart(sw.toString());

		MessageImpl message = new MessageImpl();
		message.setDate(new Date());
		message.setSender(from);
		message.setFrom(from);
		message.setTo(recipient);
		message.setSubject(String.format("%s %s",
				resolver.translate("expireNotificationSubject",
						new Object[] { dirEntry.displayName, notificationInterval }),
				notificationInterval > 1 ? resolver.translate("days", new Object[0])
						: resolver.translate("day", new Object[0])));

		MessageBuilder builder = MessageServiceFactory.newInstance().newMessageBuilder();
		Header header = builder.newHeader();
		header.setField(Fields.contentType("text/html; charset=UTF-8;"));
		header.setField(Fields.contentTransferEncoding("quoted-printable"));
		body.setHeader(header);

		message.setMultipart(createMixedBody(body));
		new Sendmail().send(from, message);
	}

	private BodyPart createTextPart(String text) {
		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		TextBody body = bodyFactory.textBody(text, StandardCharsets.UTF_8);

		BodyPart bodyPart = new BodyPart();
		bodyPart.setText(body);

		return bodyPart;
	}

	private Multipart createMixedBody(BodyPart body) {
		MessageImpl alternativeMessage = new MessageImpl();
		Multipart createAlternativePart = createMultipart(body, "alternative");
		alternativeMessage.setMultipart(createAlternativePart);
		MessageImpl createAlternativeMessage = alternativeMessage;
		return createMultipart(createAlternativeMessage, "mixed");
	}

	private Multipart createMultipart(AbstractEntity bodyPart, String subType) {
		Multipart multipart = new MultipartImpl(subType);
		multipart.addBodyPart(bodyPart);
		return multipart;
	}

	private Set<String> getUserUids(ServerSideServiceProvider provider, Integer passwordLifetime,
			Integer notificationInterval) throws SQLException {
		Set<String> userUids = new HashSet<>();

		String query = String.format("SELECT tci.uid AS uid FROM t_domain_user tdu " //
				+ "INNER JOIN t_container_item tci ON tci.id=tdu.item_id " //
				+ "INNER JOIN t_container tc ON tc.id=tci.container_id "//
				+ "WHERE NOT tdu.archived "//
				+ "AND date(tdu.password_lastchange) + interval '%s days' = current_date + interval '%s days' "//
				+ "AND tc.domain_uid='%s'", passwordLifetime, notificationInterval, domainName);

		try (Connection conn = provider.getContext().getDataSource().getConnection();
				PreparedStatement st = conn.prepareStatement(query);
				ResultSet rs = st.executeQuery()) {
			while (rs.next()) {
				userUids.add(rs.getString("uid"));
			}
		}

		return userUids;
	}

	private Optional<Integer> getDomainPasswordExpirationSetting(String domainName) {
		Optional<Integer> passwordLifetime = Optional.empty();
		try {
			Integer passwordLifetimeSetting = Integer.valueOf(ServerSideServiceProvider
					.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainName).get()
					.get(DomainSettingsKeys.password_lifetime.name()));
			if (passwordLifetimeSetting > 0) {
				passwordLifetime = Optional.of(passwordLifetimeSetting);
			}
		} catch (NumberFormatException nfe) {
		}
		return passwordLifetime;
	}

	@Override
	public JobKind getType() {
		return JobKind.MULTIDOMAIN;
	}

	@Override
	public String getDescription(String locale) {
		if ("fr".equals(locale)) {
			return "Notifie par mail les utilisateurs dont le mot de passe arrive à expiration";
		} else {
			return "Send mail notification to user whose password will expire";
		}
	}

	@Override
	public String getJobId() {
		return JID;
	}

	@Override
	public Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

}
