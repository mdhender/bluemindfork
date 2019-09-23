/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.reminder.job;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.MessageBuilder;
import org.apache.james.mime4j.dom.MessageServiceFactory;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import freemarker.template.TemplateException;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.utils.Memoizer;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.scheduler.IScheduledJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUserSettings;

public class ReminderJob implements IScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ReminderJob.class);
	ISendmail mailer = new Sendmail();
	private List<IAlarmSupport<ICalendarElement>> jobImplementations;

	private final static Map<String, BaseContainerDescriptor> containers = new ConcurrentHashMap<>();

	public static Map<String, Set<String>> pendingUserUidsByDomain;

	private static final boolean FAST_MODE = new File("/etc/bm/fast.reminders").exists();

	@Override
	public void tick(IScheduler sched, boolean plannedExecution, String domainName, Date startDate) {
		if (jobImplementations == null) {
			jobImplementations = getJobImplementations();
			logger.debug("Found {} ReminderJob implementations", jobImplementations.size());
		}

		IScheduledJobRunId rid = null;

		try {
			rid = sched.requestSlot(domainName, this, startDate);

			Calendar start = Calendar.getInstance();
			start.setTimeZone(TimeZone.getTimeZone("UTC"));
			start.set(Calendar.SECOND, 0);
			start.set(Calendar.MILLISECOND, 0);

			BmDateTime dtalarm = BmDateTimeWrapper.fromTimestamp(start.getTime().getTime(), "UTC", Precision.DateTime);
			logger.debug("running ReminderJob with date {}", dtalarm.iso8601);

			List<ItemValue<Domain>> domains = getDomains();

			if (!isValid(pendingUserUidsByDomain, domains)) {
				initPendingUids(domains);
			}

			int count = 0;

			for (ItemValue<Domain> d : domains) {
				logger.debug("searching reminders for domain {}", d.uid);
				int dCount = executeReminderJobs(sched, rid, d, dtalarm, jobImplementations);
				logger.debug("{} reminders for domain {}", dCount, d.uid);
				count += dCount;
			}

			sched.info(rid, "en", "" + count + " entities processed.");
			sched.info(rid, "fr", "" + count + " entitées traîtés.");
			sched.finish(rid, JobExitStatus.SUCCESS);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			if (rid != null) {
				sched.finish(rid, JobExitStatus.FAILURE);
			}
		}

	}

	private boolean isValid(Map<String, Set<String>> pendingUserUidsByDomain, List<ItemValue<Domain>> domains) {
		if (pendingUserUidsByDomain == null) {
			return false;
		}
		for (ItemValue<Domain> d : domains) {
			if (!pendingUserUidsByDomain.containsKey(d.uid)) {
				return false;
			}
		}
		return true;
	}

	private void initPendingUids(List<ItemValue<Domain>> domains) {
		pendingUserUidsByDomain = Maps.newConcurrentMap();
		DirEntryQuery q = new DirEntryQuery();
		q.kindsFilter = Arrays.asList(Kind.USER);
		for (ItemValue<Domain> d : domains) {
			Set<String> userUids = Sets.newConcurrentHashSet();
			userUids.addAll(getDirService(d).search(q).values.stream().map(de -> de.value.entryUid)
					.collect(Collectors.toSet()));
			pendingUserUidsByDomain.put(d.uid, userUids);
		}
	}

	List<ItemValue<Domain>> getDomains() {
		return getDomainService().all().stream().filter(domain -> !domain.uid.equals("global.virt"))
				.collect(Collectors.toList());
	}

	IDomains getDomainService() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
	}

	<T extends ICalendarElement> List<IAlarmSupport<T>> getJobImplementations() {
		return new RunnableExtensionLoader<IAlarmSupport<T>>().loadExtensions("net.bluemind.reminder", "job",
				"job_provider", "implementation");
	}

	private <T extends ICalendarElement> int executeReminderJobs(IScheduler sched, IScheduledJobRunId rid,
			final ItemValue<Domain> d, BmDateTime dtalarm, List<IAlarmSupport<T>> implementations) throws Exception {

		Iterator<String> it = pendingUserUidsByDomain.get(d.uid).iterator();
		while (it.hasNext()) {
			String entryUid = it.next();
			try {
				if (FAST_MODE) {
					initDefaultContainerList(d, implementations, entryUid);
				} else {
					initAllContainersList(d, implementations, entryUid);
				}
			} catch (Exception e) {
				logger.error(String.format("failed to notify %s@%s", entryUid, d.uid), e);
				sched.warn(rid, "en", "Erreur during reminder notification :" + e.getMessage());
			}
			it.remove();
		}

		int count = 0;
		IMailboxes mailboxesService = getMailboxesService(d);
		Function<String, ItemValue<net.bluemind.mailbox.api.Mailbox>> userSearch = Memoizer
				.memoize(uid -> mailboxesService.getComplete(uid));
		Function<String, Map<String, String>> userSettingsSearch = Memoizer.memoize(uid -> getUserSettings(d, uid));

		for (Iterator<Entry<String, BaseContainerDescriptor>> iterator = containers.entrySet().iterator(); iterator
				.hasNext();) {
			BaseContainerDescriptor cd = iterator.next().getValue();
			for (IAlarmSupport<T> alarmSupport : implementations) {
				try {
					if (cd.type.equals(alarmSupport.getContainerType()) && cd.domainUid.equals(d.uid)) {

						alarmSupport.initContainerItemsCache(dtalarm, cd);
						List<Reminder<T>> reminders = alarmSupport.getReminder(dtalarm, cd);
						if (reminders.isEmpty()) {
							continue;
						}
						Map<String, String> settings = userSettingsSearch.apply(cd.owner);
						ItemValue<net.bluemind.mailbox.api.Mailbox> user = userSearch.apply(cd.owner);
						count += sendReminders(sched, rid, d, user, settings, reminders, alarmSupport);
						break;
					}
				} catch (Exception e) {
					logger.error("Impossible to send reminder for alarm support container: {}", cd, e);
					iterator.remove();
				}
			}

		}

		return count;
	}

	private <T extends ICalendarElement> void initDefaultContainerList(final ItemValue<Domain> d,
			List<IAlarmSupport<T>> implementations, String entryUid) {
		logger.debug("searching reminders for user {}@{}", entryUid, d.uid);
		for (IAlarmSupport<T> alarmSupport : implementations) {
			BaseContainerDescriptor containerDescriptor = new BaseContainerDescriptor();
			containerDescriptor.owner = entryUid;
			containerDescriptor.domainUid = d.uid;

			String containerType = alarmSupport.getContainerType();
			if (containerType.equals(ICalendarUids.TYPE)) {
				containerDescriptor.uid = ICalendarUids.defaultUserCalendar(entryUid);
				containerDescriptor.ownerDirEntryPath = d.uid + "/users/" + entryUid;
				containerDescriptor.type = "calendar";
			} else if (containerType.equals(ITodoUids.TYPE)) {
				containerDescriptor.uid = ITodoUids.defaultUserTodoList(entryUid);
				containerDescriptor.type = "todolist";
			} else {
				continue;
			}

			containers.put(containerDescriptor.uid, containerDescriptor);
		}

	}

	private <T extends ICalendarElement> void initAllContainersList(final ItemValue<Domain> d,
			List<IAlarmSupport<T>> implementations, String entryUid) throws Exception {
		logger.debug("searching reminders for user {}@{}", entryUid, d.uid);
		for (IAlarmSupport<T> alarmSupport : implementations) {
			List<BaseContainerDescriptor> containerDescriptors = getContainers(entryUid, alarmSupport);
			for (BaseContainerDescriptor containerDescriptor : containerDescriptors) {

				boolean syncReminder = true;
				if (containerDescriptor.settings.containsKey("syncReminders")) {
					syncReminder = containerDescriptor.settings.get("syncReminders").equals("true");
				}

				if (syncReminder) {
					containers.put(containerDescriptor.uid, containerDescriptor);
				}
			}
		}
	}

	IDirectory getDirService(ItemValue<Domain> d) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDirectory.class, d.uid);
	}

	IMailboxes getMailboxesService(ItemValue<Domain> d) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, d.uid);
	}

	<T extends ICalendarElement> List<BaseContainerDescriptor> getContainers(String userUid,
			IAlarmSupport<T> alarmSupport) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class)
				.allLight(ContainerQuery.ownerAndType(userUid, alarmSupport.getContainerType()));
	}

	Map<String, String> getUserSettings(ItemValue<Domain> d, String userUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUserSettings.class, d.uid)
				.get(userUid);
	}

	private <T extends ICalendarElement> int sendReminders(IScheduler sched, IScheduledJobRunId rid,
			ItemValue<Domain> domain, ItemValue<net.bluemind.mailbox.api.Mailbox> userMailbox,
			Map<String, String> settings, List<Reminder<T>> reminders, IAlarmSupport<T> alarmSupport) {

		Mailbox from = SendmailHelper.formatAddress(userMailbox.displayName, userMailbox.value.defaultEmail().address);
		Mailbox sender = from;
		Mailbox to = SendmailHelper.formatAddress(userMailbox.displayName, userMailbox.value.defaultEmail().address);

		for (Reminder<T> reminder : reminders) {
			Map<String, Object> data = alarmSupport.extractEntityDataToMap(reminder.entity.value, reminder.valarm);
			String subject = alarmSupport.buildSubject(settings, data);
			sendMessage(new Mailboxes(from, sender, to), subject, data, settings, reminder.entity.value, alarmSupport);
			sendToMQ(domain, userMailbox, reminder.entity, reminder.valarm, alarmSupport);
			alarmSupport.logSchedInfo(to, reminder, sched, rid);
		}

		return reminders.size();
	}

	/**
	 * @param at
	 * @param from
	 * @param sender
	 * @param to
	 * @param subject
	 * @param data
	 * @param userPrefs
	 * @param entity
	 * @throws ServerFault
	 */
	<T extends ICalendarElement> void sendMessage(Mailboxes mboxes, String subject, Map<String, Object> data,
			Map<String, String> userPrefs, T entity, IAlarmSupport<T> alarmSupport) {

		String dateFormat = tryGet(userPrefs, "yyyy-MM-dd", "date_format", "date", "dateformat");
		String timeFormat = tryGet(userPrefs, "HH:mm", "time_format", "timeformat", "time");

		data.put("datetime_format", dateFormat + " " + timeFormat);
		data.put("time_format", timeFormat);
		data.put("date_format", "EEE, MMMM dd, yyyy");
		if ("fr".equals(userPrefs.get("lang"))) {
			data.put("date_format", "EEEE d MMMM yyyy");
		}

		TimeZone tz = TimeZone.getTimeZone(userPrefs.get("timezone"));
		data.put("timezone", tz.getID());

		if (entity.timezone() != null && !entity.timezone().equals(userPrefs.get("timezone"))) {
			data.put("tz", tz.getDisplayName(new Locale(userPrefs.get("lang"))));
		}

		try (Message message = getMessage(mboxes, subject, userPrefs.get("lang"), data, alarmSupport)) {

			logger.info("send reminder to {} for entity {}", mboxes.to.getAddress(), entity.summary);

			mailer.send(mboxes.from, message);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String tryGet(Map<String, String> userPrefs, String defaultValue, String... keys) {
		for (int i = 0; i < keys.length; i++) {
			if (null != userPrefs.get(keys[i]) && userPrefs.get(keys[i]).trim().length() > 0) {
				return userPrefs.get(keys[i]);
			}
		}
		return defaultValue;
	}

	/**
	 * @param from
	 * @param sender
	 * @param to
	 * @param subject
	 * @param locale
	 * @param data
	 * @return
	 * @throws TemplateException
	 * @throws IOException
	 * @throws MimeException
	 */
	private <T extends ICalendarElement> Message getMessage(Mailboxes mboxes, String subject, String locale,
			Map<String, Object> data, IAlarmSupport<? extends ICalendarElement> alarmSupport) {
		try {
			Mail m = new Mail();
			m.from = mboxes.from;
			m.sender = mboxes.sender;
			m.to = mboxes.to;
			m.html = alarmSupport.buildBody(locale, data);
			m.subject = subject;
			return m.getMessage();
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	/**
	 *
	 */
	private class Mail {

		public Mailbox from;
		public Mailbox sender;
		public Mailbox to;
		public String subject;
		public BodyPart html;

		public Mail() {
		}

		public Message getMessage() throws MimeException {
			MessageBuilder builder = MessageServiceFactory.newInstance().newMessageBuilder();

			MessageImpl m = new MessageImpl();
			m.setDate(new Date());
			m.setSubject(subject);
			m.setSender(sender);
			m.setFrom(from);
			m.setTo(to);

			Header h = builder.newHeader();
			h.setField(Fields.contentType("text/html; charset=UTF-8;"));
			h.setField(Fields.contentTransferEncoding("quoted-printable"));
			html.setHeader(h);

			Multipart alternative = new MultipartImpl("alternative");
			alternative.addBodyPart(html);

			MessageImpl alternativeMessage = new MessageImpl();
			alternativeMessage.setMultipart(alternative);

			BodyPart alternativePart = new BodyPart();
			alternativePart.setMessage(alternativeMessage);

			Multipart mixed = new MultipartImpl("mixed");
			mixed.addBodyPart(alternativeMessage);

			m.setMultipart(mixed);

			return m;
		}
	}

	/**
	 * @param occurrence
	 * @param entity
	 * @param user
	 * @param valarm
	 * @param attendee
	 * @throws HornetQException
	 */
	<T extends ICalendarElement> void sendToMQ(ItemValue<Domain> domain,
			ItemValue<net.bluemind.mailbox.api.Mailbox> targetBox, ItemValue<T> entity, VAlarm valarm,
			IAlarmSupport<T> alarmSupport) {
		OOPMessage msg = MQ.newMessage();
		msg.putStringProperty("latd", targetBox.value.name + "@" + domain.uid);
		msg.putStringProperty("operation", "reminder");
		alarmSupport.addMQProperties(msg, entity, valarm);
		VertxPlatform.eventBus().send(ReminderNotifications.addressForMailbox(targetBox.uid), msg.toJson());
	}

	@Override
	public JobKind getType() {
		return JobKind.GLOBAL;
	}

	@Override
	public String getDescription(String locale) {
		// TODO locale?
		return "Génère les mails de rappels des rendez-vous et tâches";
	}

	@Override
	public String getJobId() {
		return "net.bluemind.reminder.job.ReminderJob";
	}

	public static class Mailboxes {
		public final Mailbox from;
		public final Mailbox sender;
		public final Mailbox to;

		public Mailboxes(Mailbox from, Mailbox sender, Mailbox to) {
			this.from = from;
			this.sender = sender;
			this.to = to;
		}
	}

	@Override
	public Set<String> getLockedResources() {
		return Collections.emptySet();
	}

	@Override
	public boolean supportsScheduling() {
		return true;
	}

	public void addPendingUid(String cUid) {
		BaseContainerDescriptor cd = containers.computeIfAbsent(cUid, key -> {
			BaseContainerDescriptor tmp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IContainers.class).get(key);
			return BaseContainerDescriptor.create(tmp.uid, tmp.name, tmp.owner, tmp.type, tmp.domainUid,
					tmp.defaultContainer);
		});

		if (pendingUserUidsByDomain == null) {
			initPendingUids(getDomains());
		}

		Set<String> userUids = pendingUserUidsByDomain.computeIfAbsent(cd.domainUid,
				key -> Sets.newConcurrentHashSet());
		userUids.add(cd.owner);
		pendingUserUidsByDomain.put(cd.domainUid, userUids);
	}
}
