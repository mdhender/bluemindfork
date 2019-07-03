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
package net.bluemind.calendar.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BodyPart;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import freemarker.template.TemplateException;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventQuery;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.calendar.helper.mail.Messages;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.calendar.occurrence.OccurrenceHelper;
import net.bluemind.common.freemarker.MessagesResolver;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.reminder.job.IAlarmSupport;
import net.bluemind.reminder.job.Reminder;
import net.bluemind.reminder.job.ReminderJob;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public class CalendarAlarmSupport implements IAlarmSupport<VEvent> {

	private static final Logger logger = LoggerFactory.getLogger(CalendarAlarmSupport.class);
	public static final String BODY_TEMPLATE = "EventAlert.ftl";
	public static final String SUBJECT_TEMPLATE = "EventSubjectAlert.ftl";

	public static final ConcurrentMap<String, List<ItemValue<VEventSeries>>> eventsOfTheDay = new ConcurrentHashMap<>();
	private static DateTime day;

	static {
		if (VertxPlatform.getPlatformManager() == null) {
			logger.error("vertx platform not available !");
		} else {
			VertxPlatform.eventBus().registerHandler(CalendarHookAddress.CHANGED, msg -> {
				JsonObject o = (JsonObject) msg.body();
				String cUid = o.getString("container");
				eventsOfTheDay.remove(cUid);

				new ReminderJob().addPendingUid(cUid);
			});
		}
	}

	@Override
	public String getContainerType() {
		return ICalendarUids.TYPE;
	}

	@Override
	public List<Reminder<VEvent>> getReminder(BmDateTime dtalarm, BaseContainerDescriptor containerDescriptor)
			throws ServerFault {
		prepareForDate(dtalarm);
		String containerUid = containerDescriptor.uid;
		List<ItemValue<VEventSeries>> events = eventsOfTheDay.get(containerUid);
		if (events == null) {
			return new ArrayList<Reminder<VEvent>>();
		}

		List<net.bluemind.calendar.api.Reminder> ret = new LinkedList<>();
		for (ItemValue<VEventSeries> event : events) {
			if (event.value.main != null && event.value.main.hasAlarm()) {
				for (VAlarm valarm : event.value.main.alarm) {
					BmDateTime expected = BmDateTimeWrapper.fromTimestamp(
							new BmDateTimeWrapper(dtalarm).toUTCTimestamp() - (valarm.trigger * 1000),
							event.value.main.dtstart.timezone);
					VEventOccurrence occs = OccurrenceHelper.getOccurrence(event, expected);
					// do not add reminder for event exceptions
					// exceptions are processed below
					if (occs != null && event.value.occurrence(expected) == null
							&& attends(occs, containerDescriptor.ownerDirEntryPath)) {
						ret.add(net.bluemind.calendar.api.Reminder.create(ItemValue.create(event.uid, occs), valarm));
					}
				}
			}
			if (event.value.occurrences != null && !event.value.occurrences.isEmpty()) {
				for (VEventOccurrence occ : event.value.occurrences) {
					if (!occ.hasAlarm()) {
						continue;
					}
					for (VAlarm valarm : occ.alarm) {
						BmDateTime expected = BmDateTimeWrapper.fromTimestamp(
								new BmDateTimeWrapper(dtalarm).toUTCTimestamp() - (valarm.trigger * 1000),
								occ.dtstart.timezone);
						if (expected.equals(occ.dtstart) && attends(occ, containerDescriptor.ownerDirEntryPath)) {
							ret.add(net.bluemind.calendar.api.Reminder.create(ItemValue.create(event.uid, occ),
									valarm));
						}
					}

				}
			}
		}

		return toReminder(ret);
	}

	private boolean attends(VEventOccurrence occ, String dir) {
		final String owner = "bm://" + dir;
		return occ.attendees.isEmpty() || owner.equals(occ.organizer.dir) || occ.attendees.stream()
				.anyMatch(a -> a.partStatus != ParticipationStatus.Declined && owner.equals(a.dir));
	}

	private void prepareForDate(BmDateTime dtalarm) {
		DateTime currentDate = new BmDateTimeWrapper(dtalarm).toJodaTime().withTimeAtStartOfDay();
		if (day == null || !currentDate.isEqual(day)) {
			day = currentDate;
			eventsOfTheDay.clear();
		}
	}

	private List<ItemValue<VEventSeries>> loadEvents(String containerUid) {
		ICalendar calendar = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICalendar.class,
				containerUid);
		VEventQuery q = VEventQuery.create(BmDateTimeWrapper.create(day, Precision.DateTime),
				BmDateTimeWrapper.create(day.withFieldAdded(DurationFieldType.hours(), 48), Precision.DateTime));
		ListResult<ItemValue<VEventSeries>> events = calendar.search(q);
		return events.values.stream().filter(item -> item.value.hasAlarm()).collect(Collectors.toList());
	}

	private List<Reminder<VEvent>> toReminder(List<net.bluemind.calendar.api.Reminder> reminder) {
		return reminder.stream().map((elem) -> {
			return new Reminder<VEvent>(null, elem.vevent, elem.valarm);
		}).collect(Collectors.toList());
	}

	@Override
	public String buildSubject(Map<String, String> settings, Map<String, Object> data) {
		String lang = settings.get("lang");
		if (lang == null) {
			lang = "fr";
		}
		Locale l = new Locale(lang);
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventAlertMessages(l));

		String subject = new CalendarMailHelper().buildSubject(SUBJECT_TEMPLATE, lang, resolver, data);
		return subject;
	}

	@Override
	public Map<String, Object> extractEntityDataToMap(VEvent entity, VAlarm valarm) {
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(entity, valarm);
		return data;
	}

	@Override
	public void logSchedInfo(Mailbox to, Reminder<VEvent> reminder, IScheduler scheduler, IScheduledJobRunId rid) {
		scheduler.info(rid, "fr",
				"Mail de rappel envoyé à " + to.getAddress() + " pour l'évènement " + reminder.entity.value.summary);
		scheduler.info(rid, "en",
				"Reminder email sent to " + to.getAddress() + " for the event " + reminder.entity.value.summary);
	}

	@Override
	public BodyPart buildBody(String locale, Map<String, Object> data) throws IOException, TemplateException {
		Locale l = new Locale(locale);
		MessagesResolver resolver = new MessagesResolver(Messages.getEventDetailMessages(l),
				Messages.getEventAlertMessages(l));
		return new CalendarMailHelper().buildBody(BODY_TEMPLATE, locale, resolver, data);
	}

	@Override
	public void addMQProperties(OOPMessage msg, ItemValue<VEvent> entity, VAlarm valarm) {

		msg.putStringProperty("title", "Reminder");
		msg.putStringProperty("body", entity.value.summary);
		// FIXME ? ItemValue.uid ?
		msg.putStringProperty("id", entity.uid);
		msg.putLongProperty("eventStart", new BmDateTimeWrapper(entity.value.dtstart).toUTCTimestamp());

		// -valarm.trigger because it is relative to event start date
		msg.putIntProperty("alert", valarm.trigger != null ? -valarm.trigger : 0);
	}

	@Override
	public String getName() {
		return "CalendarReminderJob";
	}

	@Override
	public void initContainerItemsCache(BmDateTime dtalarm, BaseContainerDescriptor containerDescriptor) {
		prepareForDate(dtalarm);

		String containerUid = containerDescriptor.uid;
		List<ItemValue<VEventSeries>> events = eventsOfTheDay.get(containerUid);
		if (events == null) {
			logger.info("populate CalendarReminder cache for {}", containerUid);
			events = loadEvents(containerUid);
			eventsOfTheDay.put(containerUid, events);
		}

	}

}
