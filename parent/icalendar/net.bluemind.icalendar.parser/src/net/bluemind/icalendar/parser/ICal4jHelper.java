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
package net.bluemind.icalendar.parser;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.DateTimeComparator;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.lib.ical4j.util.IcalConverter;
import net.bluemind.neko.common.NekoHelper;
import net.bluemind.tag.api.TagRef;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.DelegatedFrom;
import net.fortuna.ical4j.model.parameter.DelegatedTo;
import net.fortuna.ical4j.model.parameter.Dir;
import net.fortuna.ical4j.model.parameter.FmtType;
import net.fortuna.ical4j.model.parameter.Member;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.parameter.SentBy;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.RDate;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Repeat;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.Uris;

public class ICal4jHelper<T extends ICalendarElement> {

	protected static Logger logger = LoggerFactory.getLogger(ICal4jHelper.class);

	private static final TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();

	static ZoneId utcTz = ZoneId.of("UTC");

	// ICS -> BM
	public ItemValue<T> parseIcs(T iCalendarElement, CalendarComponent cc, String globalTZ) {

		// UID
		String uid = parseIcsUid(cc.getProperty(Property.UID));

		// DTSTART
		iCalendarElement.dtstart = parseIcsDate((DateProperty) cc.getProperty(Property.DTSTART), globalTZ);

		// SUMMARY
		iCalendarElement.summary = parseIcsSummary(cc.getProperty(Property.SUMMARY));

		// CLASSIFICATION
		Classification classification = parseIcsClassification(cc.getProperty(Property.CLASS));
		if (classification != null) {
			iCalendarElement.classification = classification;
		}

		// LOCATION
		if (cc.getProperty(Property.LOCATION) != null) {
			iCalendarElement.location = cc.getProperty(Property.LOCATION).getValue();
		}

		// DESCRIPTION
		if (cc.getProperty(Property.DESCRIPTION) != null) {
			iCalendarElement.description = cc.getProperty(Property.DESCRIPTION).getValue();
		}

		// look for X-ALT-DESC too
		if (cc.getProperty("X-ALT-DESC") != null) {
			Property prop = cc.getProperty("X-ALT-DESC");
			Parameter fmtType = prop.getParameter(Parameter.FMTTYPE);
			if (fmtType != null && fmtType.getValue().equals("text/html")) {
				iCalendarElement.description = prop.getValue();
			}
		}

		// URL
		if (cc.getProperty(Property.URL) != null) {
			iCalendarElement.url = cc.getProperty(Property.URL).getValue();
		}

		// PRIORITY
		if (cc.getProperty(Property.PRIORITY) != null) {
			iCalendarElement.priority = new Integer(cc.getProperty(Property.PRIORITY).getValue());
		}

		// VALARM
		ComponentList alarms = parseIcsVAlarm(cc);
		if (alarms != null && alarms.size() > 0) {
			iCalendarElement.alarm = new ArrayList<ICalendarElement.VAlarm>(alarms.size());

			for (int i = 0; i < alarms.size(); i++) {
				VAlarm alarm = (VAlarm) alarms.get(i);
				ICalendarElement.VAlarm.Action action = null;

				if (alarm.getAction() == null) {
					// Action is required
					logger.warn("No action for alarm {}", alarm);
					continue;
				}

				if ("AUDIO".equalsIgnoreCase(alarm.getAction().getValue())) {
					action = ICalendarElement.VAlarm.Action.Audio;
				} else if ("DISPLAY".equalsIgnoreCase(alarm.getAction().getValue())) {
					action = ICalendarElement.VAlarm.Action.Display;
				} else if ("EMAIL".equalsIgnoreCase(alarm.getAction().getValue())) {
					action = ICalendarElement.VAlarm.Action.Email;
				} else if ("NONE".equalsIgnoreCase(alarm.getAction().getValue())) {
					// NONE action, skip
					// https://tools.ietf.org/html/draft-daboo-valarm-extensions-04#section-11.3
					// The "NONE" action is used solely to indicate a
					// default
					// alarm that does not alert the calendar user.
					continue;
				} else {
					action = ICalendarElement.VAlarm.Action.Email;
				}

				Integer trigger = null;
				Trigger alarmTrig = alarm.getTrigger();

				if (alarmTrig == null) {
					// Trigger is required
					logger.warn("No trigger for alarm {}", alarm);
					continue;
				}

				if (alarmTrig.getDuration() != null) {
					// Duration trigger
					Dur dur = alarmTrig.getDuration();
					trigger = dur.getSeconds() + 60 * dur.getMinutes() + 3600 * dur.getHours() + 86400 * dur.getDays();
					if (alarmTrig.getDuration().isNegative()) {
						trigger = -trigger;
					}
				} else if (alarmTrig.getDateTime() != null && cc.getProperty(Property.DTSTART) != null) {
					// DateTime trigger
					// related to dtstart, do the math
					DateProperty dstart = (DateProperty) cc.getProperty(Property.DTSTART);
					trigger = (int) ((alarmTrig.getDateTime().getTime() - dstart.getDate().getTime()) / 1000);
				}

				Integer duration = null;
				if (alarm.getDuration() != null) {
					duration = alarm.getDuration().getDuration().getSeconds();
					if (alarm.getDuration().getDuration().isNegative()) {
						duration = -duration;
					}
				}

				Integer repeat = null;
				if (alarm.getRepeat() != null) {
					repeat = alarm.getRepeat().getCount();
				}

				String description = null;
				if (alarm.getDescription() != null) {
					description = alarm.getDescription().getValue();
				}

				String summary = null;
				if (alarm.getSummary() != null) {
					summary = alarm.getSummary().getValue();
				}

				ICalendarElement.VAlarm valarm = ICalendarElement.VAlarm.create(action, trigger, description, duration,
						repeat, summary);

				iCalendarElement.alarm.add(valarm);
			}
		}

		// STATUS
		ICalendarElement.Status status = parseIcsStatus(cc.getProperty(Property.STATUS));
		if (status != null) {
			iCalendarElement.status = status;
		}

		// ATTENDEE
		List<ICalendarElement.Attendee> attendees = parseIcsAttendee(cc.getProperties(Property.ATTENDEE));
		if (attendees != null) {
			iCalendarElement.attendees = attendees;
		}

		// ORGANIZER
		ICalendarElement.Organizer organizer = parseIcsOrganizer((Organizer) cc.getProperty(Property.ORGANIZER));
		if (organizer != null) {
			iCalendarElement.organizer = organizer;
		}

		// CATEGORIES
		List<TagRef> categories = parseIcsCategories(cc.getProperties(Property.CATEGORIES));
		if (categories != null) {
			// FIXME store tags first
			// iCalendarElement.categories = categories;
		}

		// EXDATE
		Set<BmDateTime> exdate = parseIcsExDate(cc.getProperties(Property.EXDATE), globalTZ);
		if (exdate != null) {
			iCalendarElement.exdate = exdate;
		}

		// RDATE
		Set<BmDateTime> rdate = parseIcsRDate(cc.getProperties(Property.RDATE), globalTZ);
		if (rdate != null) {
			iCalendarElement.rdate = rdate;
		}

		// RRULE
		ICalendarElement.RRule rrule = parseIcsRRule(iCalendarElement, cc.getProperties(Property.RRULE));
		if (rrule != null) {
			iCalendarElement.rrule = rrule;
		}

		return ItemValue.create(uid, iCalendarElement);
	}

	/**
	 * @param categoriesPropList
	 * @return
	 */
	private static List<TagRef> parseIcsCategories(PropertyList categoriesPropList) {
		if (categoriesPropList != null && categoriesPropList.size() > 0) {
			List<TagRef> categories = new ArrayList<TagRef>(categoriesPropList.size());

			for (@SuppressWarnings("unchecked")
			Iterator<Property> it = categoriesPropList.iterator(); it.hasNext();) {
				Property category = it.next();
				TagRef tr = new TagRef();
				tr.label = category.getValue().toString();
				categories.add(tr);
			}
			return categories;
		}
		return null;
	}

	private static ComponentList parseIcsVAlarm(CalendarComponent cc) {

		ComponentList alarms = null;
		if (cc instanceof VEvent) {
			alarms = ((VEvent) cc).getAlarms();
		} else if (cc instanceof VToDo) {
			alarms = ((VToDo) cc).getAlarms();
		}

		return alarms;
	}

	/**
	 * @param it
	 * @return
	 */
	private static List<ICalendarElement.Attendee> parseIcsAttendee(PropertyList attendeePropList) {

		if (attendeePropList != null && attendeePropList.size() > 0) {
			List<ICalendarElement.Attendee> attendees = new ArrayList<>(attendeePropList.size());
			for (@SuppressWarnings("unchecked")
			Iterator<Property> it = attendeePropList.iterator(); it.hasNext();) {
				Attendee prop = (Attendee) it.next();
				Parameter cuTypeParam = prop.getParameter(Parameter.CUTYPE);
				ICalendarElement.CUType cuType = null;
				if (isParamNotNull(cuTypeParam)) {
					String value = cuTypeParam.getValue().toLowerCase();
					if ("individual".equals(value)) {
						cuType = ICalendarElement.CUType.Individual;
					} else if ("group".equals(value)) {
						cuType = ICalendarElement.CUType.Group;
					} else if ("Resource".equals(value)) {
						cuType = ICalendarElement.CUType.Resource;
					} else if ("Room".equals(value)) {
						cuType = ICalendarElement.CUType.Room;
					} else {
						cuType = ICalendarElement.CUType.Unknown;
					}

				}

				Parameter memberParam = prop.getParameter(Parameter.MEMBER);
				String member = null;
				if (isParamNotNull(memberParam)) {
					member = memberParam.getValue();
				}

				Parameter roleParam = prop.getParameter(Parameter.ROLE);
				ICalendarElement.Role role = null;
				if (isParamNotNull(roleParam)) {
					String value = roleParam.getValue().toLowerCase();
					if ("chair".equals(value)) {
						role = ICalendarElement.Role.Chair;
					} else if ("req-participant".equals(value)) {
						role = ICalendarElement.Role.RequiredParticipant;
					} else if ("opt-participant".equals(value)) {
						role = ICalendarElement.Role.OptionalParticipant;
					} else if ("non-participant".equals(value)) {
						role = ICalendarElement.Role.NonParticipant;
					} else {
						logger.error("Unsupported Role " + value);
					}
				}

				Parameter partStatParam = prop.getParameter(Parameter.PARTSTAT);
				ICalendarElement.ParticipationStatus partStatus = null;
				if (isParamNotNull(partStatParam)) {
					String value = partStatParam.getValue().toLowerCase();
					if ("needs-action".equals(value)) {
						partStatus = ICalendarElement.ParticipationStatus.NeedsAction;
					} else if ("accepted".equals(value)) {
						partStatus = ICalendarElement.ParticipationStatus.Accepted;
					} else if ("declined".equals(value)) {
						partStatus = ICalendarElement.ParticipationStatus.Declined;
					} else if ("tentative".equals(value)) {
						partStatus = ICalendarElement.ParticipationStatus.Tentative;
					} else if ("delegated".equals(value)) {
						partStatus = ICalendarElement.ParticipationStatus.Delegated;
					} else {
						partStatus = ICalendarElement.ParticipationStatus.NeedsAction;
					}
				}

				Parameter rsvpParam = prop.getParameter(Parameter.RSVP);
				Boolean rsvp = null;
				if (isParamNotNull(rsvpParam)) {
					rsvp = Boolean.valueOf(rsvpParam.getValue());
				}

				Parameter delToParam = prop.getParameter(Parameter.DELEGATED_TO);
				String delTo = null;
				if (isParamNotNull(delToParam)) {
					delTo = delToParam.getValue();
				}

				Parameter delFromParam = prop.getParameter(Parameter.DELEGATED_FROM);
				String delFrom = null;
				if (isParamNotNull(delFromParam)) {
					delFrom = delFromParam.getValue();
				}

				Parameter sentByparam = prop.getParameter(Parameter.SENT_BY);
				String sentBy = null;
				if (isParamNotNull(sentByparam)) {
					sentBy = sentByparam.getValue();
				}

				Parameter CNParam = prop.getParameter(Parameter.CN);
				String commonName = null;
				if (isParamNotNull(CNParam)) {
					commonName = CNParam.getValue();
				}

				Parameter dirParam = prop.getParameter(Parameter.DIR);
				String dir = null;
				if (isParamNotNull(dirParam)) {
					dir = dirParam.getValue();
				}

				Parameter langParam = prop.getParameter(Parameter.LANGUAGE);
				String lang = null;
				if (isParamNotNull(langParam)) {
					lang = langParam.getValue();
				}
				String mailto = null;
				try {
					mailto = prop.getCalAddress().toURL().getPath().toLowerCase().replace("mailto:", "");
				} catch (Exception e) {
					logger.error("Fail to parse Attendee URI {}: {}", prop.getCalAddress(), e.getMessage());

					// iCal specific
					Parameter email = prop.getParameter("EMAIL");
					if (email != null) {
						mailto = email.getValue();
					}
				}

				ICalendarElement.Attendee attendee = ICalendarElement.Attendee.create(cuType, member, role, partStatus,
						rsvp, delTo, delFrom, sentBy, commonName, dir, lang, null, mailto);

				Parameter responseComment = prop.getParameter("X-RESPONSE-COMMENT");
				if (responseComment != null) {
					attendee.responseComment = responseComment.getValue();
				}
				attendees.add(attendee);
			}
			return attendees;
		}

		return null;
	}

	/**
	 * @param exDatePropList
	 * @return
	 */
	private static Set<BmDateTime> parseIcsExDate(PropertyList exDatePropList, String globalTZ) {

		if (exDatePropList != null && exDatePropList.size() > 0) {
			Set<BmDateTime> ret = new HashSet<>();
			for (@SuppressWarnings("unchecked")
			Iterator<Property> it = exDatePropList.iterator(); it.hasNext();) {
				ExDate exDate = (ExDate) it.next();
				DateList dateList = exDate.getDates();
				for (Object o : dateList) {
					String oTimeZone = null != exDate.getTimeZone() ? exDate.getTimeZone().getID() : null;
					oTimeZone = null != oTimeZone ? oTimeZone : globalTZ;
					ret.add(IcalConverter.convertToDateTime((Date) o, oTimeZone));
				}

			}
			return ret;
		}
		return null;

	}

	/**
	 * @param rDatePropList
	 * @return
	 */
	private static Set<BmDateTime> parseIcsRDate(PropertyList rDatePropList, String globalTZ) {

		if (rDatePropList != null && rDatePropList.size() > 0) {
			Set<BmDateTime> ret = new HashSet<>();
			for (@SuppressWarnings("unchecked")
			Iterator<Property> it = rDatePropList.iterator(); it.hasNext();) {
				RDate rDate = (RDate) it.next();
				DateList dateList = rDate.getDates();
				for (Object o : dateList) {
					String oTimeZone = null != rDate.getTimeZone() ? rDate.getTimeZone().getID() : null;
					oTimeZone = null != oTimeZone ? oTimeZone : globalTZ;
					ret.add(IcalConverter.convertToDateTime((Date) o, oTimeZone));
				}

			}
			return ret;
		}
		return null;
	}

	/**
	 * @param elem
	 * @param rrulePropList
	 * @return
	 */
	private static ICalendarElement.RRule parseIcsRRule(ICalendarElement elem, PropertyList rrulePropList) {
		if (rrulePropList != null && rrulePropList.size() > 0) {
			ICalendarElement.RRule reccurringRule = new ICalendarElement.RRule();

			RRule rrule = (RRule) rrulePropList.get(0);
			Recur recur = rrule.getRecur();

			reccurringRule.frequency = ICalendarElement.RRule.Frequency.valueOf(recur.getFrequency());

			if (recur.getCount() > 0) {
				reccurringRule.count = recur.getCount();
			}

			if (recur.getInterval() > 0) {
				reccurringRule.interval = recur.getInterval();
			}

			// RFC 5545
			// If the "DTSTART" property is specified as a date with UTC
			// time or a date with local time and time zone reference, then the
			// UNTIL rule part MUST be specified as a date with UTC time
			String tz = (null == elem.dtstart.timezone) ? null : "UTC";

			reccurringRule.until = IcalConverter.convertToDateTime(recur.getUntil(), tz);
			if (null != reccurringRule.until && null != elem.dtstart) {
				reccurringRule.until = BmDateTimeWrapper.create(reccurringRule.until.iso8601, elem.dtstart.precision);
			}

			if (recur.getSecondList() != null) {
				reccurringRule.bySecond = new ArrayList<>(recur.getSecondList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getSecondList().iterator(); it.hasNext();) {
					reccurringRule.bySecond.add(it.next());
				}
			}

			if (recur.getMinuteList() != null) {
				reccurringRule.byMinute = new ArrayList<>(recur.getMinuteList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getMinuteList().iterator(); it.hasNext();) {
					reccurringRule.byMinute.add(it.next());
				}
			}

			if (recur.getHourList() != null) {
				reccurringRule.byHour = new ArrayList<>(recur.getHourList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getHourList().iterator(); it.hasNext();) {
					reccurringRule.byHour.add(it.next());
				}
			}

			if (recur.getDayList() != null) {
				reccurringRule.byDay = new ArrayList<ICalendarElement.RRule.WeekDay>();
				for (@SuppressWarnings("unchecked")
				Iterator<WeekDay> it = recur.getDayList().iterator(); it.hasNext();) {
					String value = it.next().toString().toLowerCase();
					reccurringRule.byDay.add(new ICalendarElement.RRule.WeekDay(value));
				}
			}

			if (recur.getMonthDayList() != null) {
				reccurringRule.byMonthDay = new ArrayList<>(recur.getMonthDayList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getMonthDayList().iterator(); it.hasNext();) {
					reccurringRule.byMonthDay.add(it.next());
				}
			}

			if (recur.getYearDayList() != null) {
				reccurringRule.byYearDay = new ArrayList<>(recur.getYearDayList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getYearDayList().iterator(); it.hasNext();) {
					reccurringRule.byYearDay.add(it.next());
				}
			}

			if (recur.getWeekNoList() != null) {
				reccurringRule.byWeekNo = new ArrayList<>(recur.getWeekNoList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getWeekNoList().iterator(); it.hasNext();) {
					reccurringRule.byWeekNo.add(it.next());
				}
			}
			if (recur.getMonthList() != null) {
				reccurringRule.byMonth = new ArrayList<>(recur.getMonthList().size());
				for (@SuppressWarnings("unchecked")
				Iterator<Integer> it = recur.getMonthList().iterator(); it.hasNext();) {
					reccurringRule.byMonth.add(it.next());
				}
			}
			return reccurringRule;
		}
		return null;
	}

	/**
	 * @param status
	 * @return
	 */
	private static ICalendarElement.Status parseIcsStatus(Property status) {
		if (status != null) {
			String value = status.getValue().toLowerCase();
			if ("tentative".equals(value)) {
				return ICalendarElement.Status.Tentative;
			} else if ("confirmed".equals(value)) {
				return ICalendarElement.Status.Confirmed;
			} else if ("cancelled".equals(value)) {
				return ICalendarElement.Status.Cancelled;
			} else if ("needs-action".equals(value)) {
				return ICalendarElement.Status.NeedsAction;
			} else if ("completed".equals(value)) {
				return ICalendarElement.Status.Completed;
			} else if ("in-progress".equals(value)) {
				return ICalendarElement.Status.InProcess;
			} else {
				logger.error("Unsupported Status '{}'", value);
			}
		}
		return null;
	}

	/**
	 * @param classification
	 * @return
	 */
	private static ICalendarElement.Classification parseIcsClassification(Property classification) {
		if (classification != null) {
			String value = classification.getValue().toLowerCase();
			if ("public".equals(value)) {
				return ICalendarElement.Classification.Public;
			} else if ("private".equals(value)) {
				return ICalendarElement.Classification.Private;
			} else if ("confidential".equals(value)) {
				return ICalendarElement.Classification.Confidential;
			} else {
				logger.error("Unsupported Clazz " + classification);
			}

		}
		return null;
	}

	/**
	 * @param clazz
	 * @return
	 */
	private static String parseIcsUid(Property uid) {
		if (uid != null) {
			return uid.getValue();
		} else {
			return UUID.randomUUID().toString();
		}
	}

	/**
	 * @param summary
	 * @return
	 */
	private static String parseIcsSummary(Property summary) {
		if (summary != null) {
			String value = summary.getValue();
			// FIXME: empty summary?
			if (value == null || value.isEmpty()) {
				return "-";
			}
			return summary.getValue();
		}
		// FIXME: empty summary?
		return "-";
	}

	/**
	 * @param organizer
	 * @return
	 */
	private static ICalendarElement.Organizer parseIcsOrganizer(Organizer organizer) {
		if (organizer != null) {
			ICalendarElement.Organizer ret = new ICalendarElement.Organizer();
			try {
				ret.mailto = organizer.getCalAddress().toURL().getPath().toLowerCase().replace("mailto:", "");
			} catch (Exception e) {
				logger.error("Fail to parse Organizer URI {}: {}", organizer.getCalAddress(), e.getMessage());

				// iCal specific
				Parameter email = organizer.getParameter("EMAIL");
				if (email != null) {
					ret.mailto = email.getValue();
				}
			}
			Parameter commonName = organizer.getParameter(Parameter.CN);
			if (commonName != null) {
				ret.commonName = commonName.getValue();
			}
			return ret;
		}
		return null;
	}

	/**
	 * @param startDate
	 * @return
	 */
	protected static BmDateTime parseIcsDate(DateProperty date, String globalTZ) {
		return IcalConverter.convertToDateTime(date, globalTZ);
	}

	// BM -> ICS
	protected static PropertyList parseICalendarElement(String uid, CalendarComponent calendarComponent,
			ICalendarElement iCalendarElement) {

		PropertyList properties = calendarComponent.getProperties();

		// UID
		parseICalendarElementUid(properties, uid);

		// DTSTART
		parseICalendarElementDate(properties, iCalendarElement);

		// SUMMARY
		parseICalendarElementSummary(properties, iCalendarElement);

		// CLASSIFICATION
		parseICalendarElementClassification(properties, iCalendarElement);

		// LOCATION
		parseICalendarElementLocation(properties, iCalendarElement);

		// DESCRIPTION
		parseICalendarElementDescription(properties, iCalendarElement);

		// DESCRIPTION
		parseICalendarElementUrl(properties, iCalendarElement);

		// PRIORITY
		parseICalendarElementPriority(properties, iCalendarElement);

		// VALARM
		if (iCalendarElement.alarm != null && iCalendarElement.alarm.size() > 0) {
			for (ICalendarElement.VAlarm alarm : iCalendarElement.alarm) {
				if (alarm == null) {
					continue;
				}

				if (alarm.trigger == null) {
					// trigger is required
					continue;
				}

				if (alarm.action == null) {
					// action is required
					continue;
				}

				Dur trigger = new Dur(0, 0, 0, alarm.trigger);
				VAlarm valarm = new VAlarm(trigger);

				Trigger t = valarm.getTrigger();
				t.getParameters().add(Value.DURATION);

				if (alarm.action == ICalendarElement.VAlarm.Action.Audio) {
					valarm.getProperties().add(Action.AUDIO);
				} else if (alarm.action == ICalendarElement.VAlarm.Action.Display) {
					valarm.getProperties().add(Action.DISPLAY);
				} else {
					valarm.getProperties().add(Action.EMAIL);
				}

				if (alarm.description != null && !alarm.description.isEmpty()) {
					valarm.getProperties().add(new Description(alarm.description));
				}

				if (alarm.summary != null && !alarm.summary.isEmpty()) {
					valarm.getProperties().add(new Summary(alarm.summary));
				}

				if (alarm.duration != null) {
					Dur duration = new Dur(0, 0, 0, alarm.duration);
					valarm.getProperties().add(new Duration(duration));
				}

				if (alarm.repeat != null) {
					valarm.getProperties().add(new Repeat(alarm.repeat));
				}

				if (calendarComponent instanceof VEvent) {
					calendarComponent = (VEvent) calendarComponent;
					((VEvent) calendarComponent).getAlarms().add(valarm);
				} else { // VToDo
					((VToDo) calendarComponent).getAlarms().add(valarm);
				}
			}
		}

		// STATUS
		parseICalendarElementStatus(properties, iCalendarElement);

		// ATTENDEES
		parseICalendarElementAttendees(properties, iCalendarElement);

		// ORGANIZER
		parseICalendarElementOrganizer(properties, iCalendarElement);

		// CATEGORIES
		parseICalendarElementCategories(properties, iCalendarElement);

		// EXDATE
		parseICalendarElementExDate(properties, iCalendarElement);

		// RDATE
		parseICalendarElementRDate(properties, iCalendarElement);

		// RRULE
		parseICalendarElementRRule(properties, iCalendarElement);

		// RECCURID
		parseICalendarElementReccurId(properties, iCalendarElement);

		if (StringUtils.isNotBlank(iCalendarElement.url)) {
			try {
				properties.add(new Url(Uris.create(iCalendarElement.url)));
			} catch (URISyntaxException e) {
				logger.warn("url is not valid", e);
			}
		}
		return properties;
	}

	private static void parseICalendarElementReccurId(PropertyList properties, ICalendarElement iCalendarElement) {
		if ((iCalendarElement instanceof VEventOccurrence) && ((VEventOccurrence) iCalendarElement).recurid != null) {
			RecurrenceId recurId = new RecurrenceId(convertToIcsDate(((VEventOccurrence) iCalendarElement).recurid));
			properties.add(recurId);
		}
	}

	private static void parseICalendarElementRRule(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.rrule != null) {
			Recur recur = new Recur();

			recur.setFrequency(iCalendarElement.rrule.frequency.name());

			if (iCalendarElement.rrule.count != null) {
				recur.setCount(iCalendarElement.rrule.count);
			}

			if (iCalendarElement.rrule.interval != null) {
				recur.setInterval(iCalendarElement.rrule.interval);
			}

			if (iCalendarElement.rrule.until != null) {
				recur.setUntil(convertToIcsDate(iCalendarElement.rrule.until));
			}

			if (iCalendarElement.rrule.bySecond != null) {
				for (Integer value : iCalendarElement.rrule.bySecond) {
					recur.getSecondList().add(value);
				}
			}

			if (iCalendarElement.rrule.byMinute != null) {
				for (Integer value : iCalendarElement.rrule.byMinute) {
					recur.getMinuteList().add(value);
				}
			}

			if (iCalendarElement.rrule.byHour != null) {
				for (Integer value : iCalendarElement.rrule.byHour) {
					recur.getHourList().add(value);
				}
			}

			if (iCalendarElement.rrule.byDay != null) {
				for (ICalendarElement.RRule.WeekDay weekDay : iCalendarElement.rrule.byDay) {
					net.fortuna.ical4j.model.WeekDay wd = new net.fortuna.ical4j.model.WeekDay(
							new net.fortuna.ical4j.model.WeekDay(weekDay.day), weekDay.offset);
					recur.getDayList().add(wd);
				}
			}

			if (iCalendarElement.rrule.byMonthDay != null) {
				for (Integer value : iCalendarElement.rrule.byMonthDay) {
					recur.getMonthDayList().add(value);
				}
			}

			if (iCalendarElement.rrule.byYearDay != null) {
				for (Integer value : iCalendarElement.rrule.byYearDay) {
					recur.getYearDayList().add(value);
				}
			}

			if (iCalendarElement.rrule.byWeekNo != null) {
				for (Integer value : iCalendarElement.rrule.byWeekNo) {
					recur.getWeekNoList().add(value);
				}
			}

			if (iCalendarElement.rrule.byMonth != null) {
				for (Integer value : iCalendarElement.rrule.byMonth) {
					recur.getMonthList().add(value);
				}
			}

			RRule rrule = new RRule(recur);
			properties.add(rrule);
		}
	}

	private static void parseICalendarElementExDate(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.exdate != null && !iCalendarElement.exdate.isEmpty()) {
			DateList dateList = new DateList();
			ArrayList<BmDateTime> sorted = new ArrayList<>(iCalendarElement.exdate);
			Collections.sort(sorted, new DateTimeComparator(utcTz.getId()));
			BmDateTime d = sorted.get(0);
			if (d.precision == Precision.DateTime || d.timezone != null) {
				String tz = d.timezone != null ? d.timezone : utcTz.getId();
				dateList.setTimeZone(tzRegistry.getTimeZone(tz));
			}
			for (BmDateTime date : sorted) {
				dateList.add(convertToIcsDate(date));
			}
			ExDate exDate = new ExDate(dateList);
			exDate.setTimeZone(dateList.getTimeZone());
			properties.add(exDate);
		}
	}

	private static void parseICalendarElementRDate(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.rdate != null && !iCalendarElement.rdate.isEmpty()) {
			DateList dateList = new DateList();
			ArrayList<BmDateTime> sorted = new ArrayList<>(iCalendarElement.rdate);
			Collections.sort(sorted, new DateTimeComparator(utcTz.getId()));
			BmDateTime d = sorted.get(0);
			if (d.precision == Precision.DateTime || d.timezone != null) {
				String tz = d.timezone != null ? d.timezone : utcTz.getId();
				dateList.setTimeZone(tzRegistry.getTimeZone(tz));
			}
			for (BmDateTime date : sorted) {
				dateList.add(convertToIcsDate(date));
			}
			RDate rdate = new RDate(dateList);
			rdate.setTimeZone(dateList.getTimeZone());
			properties.add(rdate);
		}
	}

	private static void parseICalendarElementCategories(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.categories != null && !iCalendarElement.categories.isEmpty()) {
			StringBuilder categories = new StringBuilder();
			String sep = "";
			for (TagRef tag : iCalendarElement.categories) {
				categories.append(sep);
				categories.append(tag.label);
				sep = ",";
			}
			properties.add(new Categories(categories.toString()));
		}
	}

	private static void parseICalendarElementOrganizer(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.organizer != null) {
			properties.add(parseICalendarElementOrganizer(iCalendarElement.organizer));
		}
	}

	private static void parseICalendarElementAttendees(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.attendees != null && !iCalendarElement.attendees.isEmpty()) {
			for (ICalendarElement.Attendee attendee : iCalendarElement.attendees) {
				properties.add(parseICalendarElementAttendee(attendee));
			}
		}
	}

	private static void parseICalendarElementStatus(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.status != null) {
			properties.add(new Status(iCalendarElement.status.name().toUpperCase()));
		}
	}

	private static void parseICalendarElementPriority(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.priority != null) {
			properties.add(new Priority(iCalendarElement.priority.intValue()));
		}
	}

	private static void parseICalendarElementDescription(PropertyList properties, ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.description)) {
			properties.add(new Description(NekoHelper.rawText(iCalendarElement.description).trim()));
			XProperty xAltDesc = new XProperty("X-ALT-DESC",
					"<html>\n<body>" + iCalendarElement.description + "</body>\n</html>");
			xAltDesc.getParameters().add(new FmtType("text/html"));
			properties.add(xAltDesc);
		}
	}

	private static void parseICalendarElementUrl(PropertyList properties, ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.url)) {
			URI uri;
			try {
				uri = new URI(iCalendarElement.url);
				properties.add(new Url(uri));
			} catch (URISyntaxException e) {
				logger.warn(e.getMessage());
			}
		}
	}

	private static void parseICalendarElementLocation(PropertyList properties, ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.location)) {
			properties.add(new Location(iCalendarElement.location));
		}
	}

	private static void parseICalendarElementClassification(PropertyList properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.classification != null) {
			properties.add(new Clazz(iCalendarElement.classification.name().toUpperCase()));
		}
	}

	private static void parseICalendarElementSummary(PropertyList properties, ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.summary)) {
			properties.add(new Summary(iCalendarElement.summary));
		}
	}

	private static void parseICalendarElementDate(PropertyList properties, ICalendarElement iCalendarElement) {
		if (iCalendarElement.dtstart != null) {
			DtStart dtstart = new DtStart(convertToIcsDate(iCalendarElement.dtstart));
			properties.add(dtstart);
		}
	}

	private static void parseICalendarElementUid(PropertyList properties, String uid) {
		if (isStringNotNull(uid)) {
			properties.add(new Uid(uid));
		}
	}

	/**
	 * @param attendee
	 * @return
	 */
	protected static Attendee parseICalendarElementAttendee(ICalendarElement.Attendee attendee) {

		Attendee ret = new Attendee();

		ParameterList parameters = ret.getParameters();

		if (attendee.cutype != null) {
			parameters.add(new CuType(attendee.cutype.name().toUpperCase()));
		}

		if (isStringNotNull(attendee.member)) {
			try {
				parameters.add(new Member(attendee.member));
			} catch (URISyntaxException e) {
				logger.error("Fail to parse Member URI", e);
			}
		}

		if (attendee.role != null) {
			String role;
			if (attendee.role == ICalendarElement.Role.Chair) {
				role = "CHAIR";
			} else if (attendee.role == ICalendarElement.Role.RequiredParticipant) {
				role = "REQ-PARTICIPANT";
			} else if (attendee.role == ICalendarElement.Role.NonParticipant) {
				role = "NON-PARTICIPANT";
			} else {
				role = "OPT-PARTICIPANT";
			}
			parameters.add(new Role(role));
		}

		if (attendee.partStatus != null) {
			String partStat;
			if (attendee.partStatus == ICalendarElement.ParticipationStatus.NeedsAction) {
				partStat = "NEEDS-ACTION";
			} else {
				partStat = attendee.partStatus.name().toUpperCase();
			}
			parameters.add(new PartStat(partStat));
		}

		if (attendee.rsvp != null) {
			parameters.add(new Rsvp(attendee.rsvp));
		}

		if (isStringNotNull(attendee.delTo)) {
			try {
				parameters.add(new DelegatedTo(attendee.delTo));
			} catch (URISyntaxException e) {
				logger.error("Fail to parse DelegatedTo URI", e);
			}
		}

		if (isStringNotNull(attendee.delFrom)) {
			try {
				parameters.add(new DelegatedFrom(attendee.delFrom));
			} catch (URISyntaxException e) {
				logger.error("Fail to parse DelegatedFrom URI", e);
			}
		}

		if (isStringNotNull(attendee.sentBy)) {
			try {
				parameters.add(new SentBy(attendee.sentBy));
			} catch (URISyntaxException e) {
				logger.error("Fail to parse SentBy URI", e);
			}
		}

		if (isStringNotNull(attendee.commonName)) {
			parameters.add(new Cn(attendee.commonName));
		}

		if (isStringNotNull(attendee.dir)) {
			try {
				parameters.add(new Dir(attendee.dir));
			} catch (URISyntaxException e) {
				logger.error("Fail to parse Dir URI", e);
			}
		}

		if (isStringNotNull(attendee.lang)) {
			logger.info("Lang: not implemented");
		}

		if (isStringNotNull(attendee.responseComment)) {
			parameters.add(new XParameter("X-RESPONSE-COMMENT", attendee.responseComment));
		}
		try {
			ret.setValue("MAILTO:" + attendee.mailto);
		} catch (URISyntaxException e) {
			logger.error("Fail to parse MAILTO URI", e);
		}

		return ret;
	}

	/**
	 * @param organizer
	 * @return
	 */
	protected static Organizer parseICalendarElementOrganizer(ICalendarElement.Organizer organizer) {
		Organizer orga = new Organizer();
		if (organizer.commonName != null) {
			orga.getParameters().add(new Cn(organizer.commonName));
		}

		if (organizer.mailto != null) {
			try {
				orga.setValue("mailto:" + organizer.mailto);
			} catch (URISyntaxException e) {
				logger.error("Fail to parse Organizer URI", e);
			}
		}
		return orga;
	}

	protected static boolean isParamNotNull(Parameter param) {
		return (param != null && isStringNotNull(param.getValue()));
	}

	protected static boolean isStringNotNull(String s) {
		return (s != null && !s.isEmpty());
	}

	public static Date convertToIcsDate(BmDateTime date) {
		BmDateTimeWrapper bmDate = new BmDateTimeWrapper(date);
		if (date.precision == Precision.DateTime) {
			DateTime dt = new DateTime(bmDate.toUTCTimestamp());
			if (bmDate.containsTimeZone()) {
				TimeZone tz = tzRegistry.getTimeZone(date.timezone);
				dt.setTimeZone(tz);
			} else {
				dt.setTimeZone(tzRegistry.getTimeZone(utcTz.getId()));
			}
			return dt;
		} else {
			return new Date(bmDate.toTimestamp(utcTz.getId()));
		}
	}

	public static void addVTimezone(Calendar c, Set<String> timezones) {
		for (String timezone : timezones) {
			if (timezone != null) {
				net.fortuna.ical4j.model.TimeZone tz4j = tzRegistry.getTimeZone(timezone);
				if (tz4j != null) {
					VTimeZone vtz = tz4j.getVTimeZone();
					c.getComponents().add(vtz);
				} else {
					logger.warn("Invalid timezone {}", timezone);
				}
			}
		}
	}

	public static TimeZoneRegistry getTimeZoneRegistry() {
		return tzRegistry;
	}

}
