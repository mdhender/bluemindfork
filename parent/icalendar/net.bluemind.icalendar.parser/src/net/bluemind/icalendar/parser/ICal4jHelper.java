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
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.utils.DateTimeComparator;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.icalendar.api.ICalendarElement.CUType;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.lib.ical4j.util.IcalConverter;
import net.bluemind.linkify.Linkify;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;
import net.bluemind.utils.HtmlToPlainText;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.NumberList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.Recur.Builder;
import net.fortuna.ical4j.model.Recur.Frequency;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
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
import net.fortuna.ical4j.model.property.Attach;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.Categories;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DateListProperty;
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
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Trigger;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Url;
import net.fortuna.ical4j.model.property.XProperty;

public class ICal4jHelper<T extends ICalendarElement> {

	protected static Logger logger = LoggerFactory.getLogger(ICal4jHelper.class);

	private static final TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
	public static final String CONFERENCE = "X-CONFERENCE";
	public static final String CONFERENCE_ID = "X-CONFERENCE-ID";
	public static final String MAIL_TO = "mailto:";
	public static final String CID = "X-CID";
	public static final String FILE_NAME = "X-FILE-NAME";
	public static final String EMAIL = "EMAIL";
	public static final String ALT_DESC = "X-ALT-DESC";

	static ZoneId utcTz = ZoneId.of("UTC");

	public ItemValue<T> parseIcs(T iCalendarElement, CalendarComponent cc, Optional<String> globalTZ,
			Optional<CalendarOwner> owner, List<TagRef> allTags) {
		return this.parseIcs(iCalendarElement, cc, globalTZ, Collections.emptyMap(), owner, allTags);
	}

	// ICS -> BM
	public ItemValue<T> parseIcs(T iCalendarElement, CalendarComponent cc, Optional<String> globalTZ,
			Map<String, String> tzMapping, Optional<CalendarOwner> owner, List<TagRef> allTags) {

		// UID
		String uid = parseIcsUid(cc.getProperty(Property.UID));

		// DTSTART
		iCalendarElement.dtstart = parseIcsDate((DateProperty) cc.getProperty(Property.DTSTART), globalTZ, tzMapping);

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

		boolean htmlProvided = false;
		// look for X-ALT-DESC too
		if (cc.getProperty(ALT_DESC) != null) {
			Property prop = cc.getProperty(ALT_DESC);
			Parameter fmtType = prop.getParameter(Parameter.FMTTYPE);
			if (fmtType != null && fmtType.getValue().equals("text/html")) {
				iCalendarElement.description = net.fortuna.ical4j.util.Strings.unescape(prop.getValue());
				htmlProvided = true;
			}
		}

		if (!htmlProvided && iCalendarElement.description != null) {
			iCalendarElement.description = Linkify.toHtml(iCalendarElement.description);
		}

		// URL
		if (cc.getProperty(Property.URL) != null) {
			iCalendarElement.url = cc.getProperty(Property.URL).getValue();
		}

		// PRIORITY
		if (cc.getProperty(Property.PRIORITY) != null) {
			iCalendarElement.priority = Integer.valueOf(cc.getProperty(Property.PRIORITY).getValue());
		}

		// VALARM
		ComponentList alarms = parseIcsVAlarm(cc);
		if (alarms != null && !alarms.isEmpty()) {
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
				} else {
					if (EMAIL.equalsIgnoreCase(alarm.getAction().getValue())) {
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
					TemporalAmount dur = alarmTrig.getDuration();
					trigger = temporalAmountToSeconds(dur);
				} else if (alarmTrig.getDateTime() != null && cc.getProperty(Property.DTSTART) != null) {
					// DateTime trigger
					// related to dtstart, do the math
					DateProperty dstart = (DateProperty) cc.getProperty(Property.DTSTART);
					trigger = (int) ((alarmTrig.getDateTime().getTime() - dstart.getDate().getTime()) / 1000);
				}

				Integer duration = null;
				if (alarm.getDuration() != null) {
					duration = temporalAmountToSeconds(alarm.getDuration().getDuration());
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
		List<TagRef> categories = parseIcsCategories(cc.getProperties(Property.CATEGORIES), owner, allTags);
		if (categories != null && !categories.isEmpty()) {
			iCalendarElement.categories = categories;
		}

		// EXDATE
		Set<BmDateTime> exdate = parseIcsDate(cc.getProperties(Property.EXDATE), globalTZ, tzMapping);
		if (exdate != null) {
			iCalendarElement.exdate = exdate;
		}

		// RDATE
		Set<BmDateTime> rdate = parseIcsDate(cc.getProperties(Property.RDATE), globalTZ, tzMapping);
		if (rdate != null) {
			iCalendarElement.rdate = rdate;
		}

		// RRULE
		ICalendarElement.RRule rrule = parseIcsRRule(iCalendarElement, cc.getProperties(Property.RRULE));
		if (rrule != null) {
			iCalendarElement.rrule = rrule;
		}

		// ATTACH
		iCalendarElement.attachments = parseAttachments(cc.getProperties(Property.ATTACH), owner);

		// SEQUENCE
		iCalendarElement.sequence = parseIcsSequence(cc.getProperty(Property.SEQUENCE));

		// CONFERENCE
		Property videoConfUrl = cc.getProperty(CONFERENCE);
		if (videoConfUrl != null) {
			iCalendarElement.conference = videoConfUrl.getValue();
		}
		Property conferenceId = cc.getProperty(CONFERENCE_ID);
		if (conferenceId != null) {
			iCalendarElement.conferenceId = conferenceId.getValue();
		}

		ICal4jTeamsHelper.parseTeamsToBm(iCalendarElement, cc);

		return ItemValue.create(uid, iCalendarElement);
	}

	private Integer temporalAmountToSeconds(TemporalAmount dur) {
		for (TemporalUnit unit : dur.getUnits()) {
			long unitDurationInSeconds = unit.getDuration().getSeconds();
			if (unitDurationInSeconds > 0) {
				long valueAsUnit = dur.get(unit);
				if (valueAsUnit != 0) {
					return (int) (valueAsUnit * unitDurationInSeconds);
				}
			}
		}
		return 0;
	}

	private List<AttachedFile> parseAttachments(PropertyList attachments, Optional<CalendarOwner> owner) {
		List<AttachedFile> atts = new ArrayList<>();

		for (int i = 0; i < attachments.size(); i++) {
			Attach prop = (Attach) attachments.get(i);
			byte[] binary = prop.getBinary();
			if (binary == null && prop.getUri() != null) {
				addUriAttachment(prop).ifPresent(atts::add);
			} else if (binary != null && owner.isPresent()) {
				AttachedFile addBinaryAttachment = addBinaryAttachment(prop, binary, i, owner);
				if (addBinaryAttachment != null) {
					atts.add(addBinaryAttachment);
				}
			}
		}

		return atts;

	}

	private AttachedFile addBinaryAttachment(Attach prop, byte[] binary, int index, Optional<CalendarOwner> owner) {
		String extension = "data";
		Parameter fmtType = prop.getParameter(Parameter.FMTTYPE);
		if (fmtType != null) {
			extension = Mime.getExtension(fmtType.getValue());
		}
		String filename = "attachment_" + index + "." + extension;
		if (owner.isPresent()) {
			CalendarOwner calOwner = owner.get();
			try (Sudo asUser = new Sudo(calOwner.userUid, calOwner.domainUid)) {
				try {
					IAttachment service = ServerSideServiceProvider.getProvider(asUser.context)
							.instance(IAttachment.class, calOwner.domainUid);
					return service.share(filename, GenericStream.simpleValue(binary, bin -> bin));
				} catch (ServerFault e) {
					logger.info("Cannot attach binary file as attachment: {}", e.getMessage());
				}
			}
		}
		return null;
	}

	private Optional<AttachedFile> addUriAttachment(Attach prop) {
		String url = prop.getUri().toString();
		String filename = null;
		if (prop.getParameter(FILE_NAME) != null) {
			filename = prop.getParameter(FILE_NAME).getValue();
		} else {
			filename = prop.getUri().getPath();
		}
		AttachedFile att = new AttachedFile();
		att.expirationDate = 0l;
		att.name = filename;
		att.publicUrl = url;
		if (prop.getParameter(CID) != null) {
			att.cid = prop.getParameter(CID).getValue();
		} else {
			if (url != null && url.toUpperCase().startsWith("CID:")) {
				att.cid = url;
				att.name = url;
			}
		}
		if (Strings.isNullOrEmpty(att.name) || Strings.isNullOrEmpty(att.publicUrl)) {
			logger.info("Skipping broken attachment property {}:{}", att.name, att.publicUrl);
			return Optional.empty();
		}
		return Optional.of(att);
	}

	/**
	 * @param categoriesPropList
	 * @param owner
	 * @return
	 */
	private static List<TagRef> parseIcsCategories(PropertyList categoriesPropList, Optional<CalendarOwner> owner,
			List<TagRef> allTags) {
		if (categoriesPropList == null || categoriesPropList.isEmpty()) {
			return null;
		}
		if (!owner.isPresent()) {
			return null;
		}

		CalendarOwner calOwner = owner.get();

		Optional<String> containerUid = calOwner.kind != Kind.CALENDAR && calOwner.kind != Kind.RESOURCE
				? Optional.of(ITagUids.defaultUserTags(calOwner.userUid))
				: Optional.empty();
		Optional<ITags> service = containerUid.map(uid -> {
			try (Sudo asUser = new Sudo(calOwner.userUid, calOwner.domainUid)) {
				return ServerSideServiceProvider.getProvider(asUser.context).instance(ITags.class, uid);
			}
		});

		List<TagRef> categories = new ArrayList<>(categoriesPropList.size());

		for (@SuppressWarnings("unchecked")
		Iterator<Property> it = categoriesPropList.iterator(); it.hasNext();) {
			Property category = it.next();
			String labelValue = category.getValue();
			if (Strings.isNullOrEmpty(labelValue)) {
				continue;
			}
			String[] values = labelValue.split(",");
			for (String label : values) {
				Optional<TagRef> exsistingTag = allTags.stream().filter(tag -> label.equals(tag.label)).findFirst();
				if (exsistingTag.isPresent()) {
					categories.add(exsistingTag.get());
				} else {
					// 3d98ff blue
					service.ifPresent(s -> {
						String uid = UUID.randomUUID().toString();
						s.create(uid, Tag.create(label, "3d98ff"));

						TagRef tr = TagRef.create(containerUid.get(), s.getComplete(uid));
						allTags.add(tr);
						categories.add(tr);
					});
				}
			}
		}
		return categories;
	}

	private static ComponentList<VAlarm> parseIcsVAlarm(CalendarComponent cc) {
		ComponentList<VAlarm> alarms = null;
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
		if (attendeePropList != null && !attendeePropList.isEmpty()) {
			List<ICalendarElement.Attendee> attendees = new ArrayList<>(attendeePropList.size());
			for (@SuppressWarnings("unchecked")
			Iterator<Property> it = attendeePropList.iterator(); it.hasNext();) {
				Attendee prop = (Attendee) it.next();

				Parameter cuTypeParam = prop.getParameter(Parameter.CUTYPE);
				ICalendarElement.CUType cuType = null;
				if (isParamNotNull(cuTypeParam)) {
					cuType = CUType.byName(cuTypeParam.getValue());
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
						logger.error("Unsupported Role {}", value);
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

				Parameter cNParam = prop.getParameter(Parameter.CN);
				String commonName = null;
				if (isParamNotNull(cNParam)) {
					commonName = cNParam.getValue();
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
					mailto = prop.getCalAddress().toURL().getPath().toLowerCase().replace(MAIL_TO, "");
				} catch (Exception e) {
					logger.error("Fail to parse Attendee URI {}: {}", prop.getCalAddress(), e.getMessage());

					// iCal specific
					Parameter email = prop.getParameter(EMAIL);
					if (email != null) {
						mailto = email.getValue();
					}
				}

				ICalendarElement.Attendee attendee = ICalendarElement.Attendee.create(cuType, member, role, partStatus,
						rsvp, delTo, delFrom, sentBy, commonName, dir, lang, null, mailto);

				Parameter responseComment = prop.getParameter("X-RESPONSE-COMMENT");
				if (isParamNotNull(responseComment)) {
					attendee.responseComment = responseComment.getValue();
				}

				// iOS style
				Parameter counter = prop.getParameter("TO-ALL-PROPOSED-NEW-TIME");
				if (isParamNotNull(counter)) {
					// TO-ALL-PROPOSED-NEW-TIME=DTSTART:20201223T100005Z;STATUS:;

					Splitter.on(";").split(counter.getValue()).forEach(val -> {
						if (val.startsWith("DTSTART:")) {
							Iterator<String> dtstartIt = Splitter.on(":").split(val).iterator();
							dtstartIt.next();
							String dtstart = dtstartIt.next();

							DtStart counterDtStart = new DtStart();
							counterDtStart.getParameters().add(Value.DATE_TIME);
							try {
								counterDtStart.setValue(dtstart);
								attendee.counter = IcalConverter.convertToDateTime(counterDtStart.getDate(), null);
							} catch (ParseException e) {
								logger.warn("Failed to parse DTSTART {}", dtstart);
							}

						}
					});

				}

				attendees.add(attendee);
			}
			return attendees;
		}

		return null;
	}

	/**
	 * @param exDatePropList
	 * @param tzMapping
	 * @return
	 */
	private static Set<BmDateTime> parseIcsDate(PropertyList<DateListProperty> datePropList, Optional<String> globalTZ,
			Map<String, String> tzMapping) {

		if (datePropList != null && !datePropList.isEmpty()) {
			Set<BmDateTime> ret = new HashSet<>();
			for (DateListProperty date : datePropList) {
				DateList dateList = date.getDates();
				boolean adaptToParamTz = false;
				String oTimeZone = null != date.getTimeZone() ? date.getTimeZone().getID() : null;
				if (oTimeZone == null) {
					Parameter tzParam = date.getParameter(Parameter.TZID);
					if (tzParam != null) {
						oTimeZone = tzParam.getValue();
						adaptToParamTz = true;
					}
				}
				oTimeZone = oTimeZone != null ? oTimeZone : globalTZ.orElse(null);
				if (oTimeZone != null && tzMapping.containsKey(oTimeZone)) {
					oTimeZone = tzMapping.get(oTimeZone);
				}
				for (Date singleDate : dateList) {
					if (adaptToParamTz) {
						long adapted = singleDate.getTime();
						ZoneId zone = ZoneId.of(oTimeZone);
						ZoneOffset zoneOffSet = zone.getRules().getOffset(Instant.ofEpochMilli(adapted));
						adapted -= zoneOffSet.getTotalSeconds() * 1000;
						if (singleDate instanceof DateTime) {
							singleDate = new DateTime(adapted);
						} else {
							singleDate = new Date(adapted);
						}
					}
					ret.add(IcalConverter.convertToDateTime(singleDate, oTimeZone));
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
		if (rrulePropList != null && !rrulePropList.isEmpty()) {
			ICalendarElement.RRule reccurringRule = new ICalendarElement.RRule();

			RRule rrule = (RRule) rrulePropList.get(0);
			Recur recur = rrule.getRecur();

			reccurringRule.frequency = ICalendarElement.RRule.Frequency.valueOf(recur.getFrequency().name());

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
				for (Iterator<Integer> it = recur.getSecondList().iterator(); it.hasNext();) {
					reccurringRule.bySecond.add(it.next());
				}
			}

			if (recur.getMinuteList() != null) {
				reccurringRule.byMinute = new ArrayList<>(recur.getMinuteList().size());
				for (Iterator<Integer> it = recur.getMinuteList().iterator(); it.hasNext();) {
					reccurringRule.byMinute.add(it.next());
				}
			}

			if (recur.getHourList() != null) {
				reccurringRule.byHour = new ArrayList<>(recur.getHourList().size());
				for (Iterator<Integer> it = recur.getHourList().iterator(); it.hasNext();) {
					reccurringRule.byHour.add(it.next());
				}
			}

			if (recur.getDayList() != null) {
				reccurringRule.byDay = new ArrayList<ICalendarElement.RRule.WeekDay>();
				for (Iterator<WeekDay> it = recur.getDayList().iterator(); it.hasNext();) {
					String value = it.next().toString().toLowerCase();
					reccurringRule.byDay.add(new ICalendarElement.RRule.WeekDay(value));
				}
			}

			if (recur.getMonthDayList() != null) {
				reccurringRule.byMonthDay = new ArrayList<>(recur.getMonthDayList().size());
				for (Iterator<Integer> it = recur.getMonthDayList().iterator(); it.hasNext();) {
					reccurringRule.byMonthDay.add(it.next());
				}
			}

			if (recur.getYearDayList() != null) {
				reccurringRule.byYearDay = new ArrayList<>(recur.getYearDayList().size());
				for (Iterator<Integer> it = recur.getYearDayList().iterator(); it.hasNext();) {
					reccurringRule.byYearDay.add(it.next());
				}
			}

			if (recur.getWeekNoList() != null) {
				reccurringRule.byWeekNo = new ArrayList<>(recur.getWeekNoList().size());
				for (Iterator<Integer> it = recur.getWeekNoList().iterator(); it.hasNext();) {
					reccurringRule.byWeekNo.add(it.next());
				}
			}
			if (recur.getMonthList() != null) {
				reccurringRule.byMonth = new ArrayList<>(recur.getMonthList().size());
				for (Iterator<Integer> it = recur.getMonthList().iterator(); it.hasNext();) {
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
				logger.error("Unsupported Clazz {}", classification);
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
	 * @param summary
	 * @return
	 */
	private static Integer parseIcsSequence(Property sequence) {
		Integer result = 0;
		if (sequence != null) {
			String value = sequence.getValue();
			try {
				result = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				logger.warn("Sequence is not valid", e);
			}
		}
		return result;
	}

	/**
	 * @param organizer
	 * @return
	 */
	private static ICalendarElement.Organizer parseIcsOrganizer(Organizer organizer) {
		if (organizer != null) {
			ICalendarElement.Organizer ret = new ICalendarElement.Organizer();
			try {
				ret.mailto = organizer.getCalAddress().toURL().getPath().toLowerCase().replace(MAIL_TO, "");
			} catch (Exception e) {
				logger.error("Fail to parse Organizer URI {}: {}", organizer.getCalAddress(), e.getMessage());

				// iCal specific
				Parameter email = organizer.getParameter(EMAIL);
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
	 * @param tzMapping
	 * @param startDate
	 * @return
	 */
	protected static BmDateTime parseIcsDate(DateProperty date, Optional<String> globalTZ,
			Map<String, String> tzMapping) {
		return IcalConverter.convertToDateTime(date, globalTZ, tzMapping);
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
		if (iCalendarElement.alarm != null && !iCalendarElement.alarm.isEmpty()) {
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

				VAlarm valarm = new VAlarm(java.time.Duration.ofSeconds(alarm.trigger));
				Trigger t = valarm.getTrigger();
				t.getParameters().add(Value.DURATION);

				if (alarm.action == ICalendarElement.VAlarm.Action.Audio) {
					valarm.getProperties().add(Action.AUDIO);
				} else if (alarm.action == ICalendarElement.VAlarm.Action.Display) {
					valarm.getProperties().add(Action.DISPLAY);
				} else {
					valarm.getProperties().add(Action.EMAIL);
					if (iCalendarElement.attendees != null && !iCalendarElement.attendees.isEmpty()) {
						for (ICalendarElement.Attendee attendee : iCalendarElement.attendees) {
							valarm.getProperties().add(parseICalendarElementAttendee(attendee));
						}
					}
				}

				if (alarm.description != null && !alarm.description.isEmpty()) {
					valarm.getProperties().add(new Description(alarm.description));
				} else {
					valarm.getProperties().add(new Description(""));
				}

				if (alarm.summary != null && !alarm.summary.isEmpty()) {
					valarm.getProperties().add(new Summary(alarm.summary));
				} else {
					valarm.getProperties().add(new Summary(""));
				}

				if (alarm.duration != null) {
					Dur duration = new Dur(0, 0, 0, alarm.duration);
					valarm.getProperties().add(new Duration(duration));
				}

				if (alarm.repeat != null) {
					valarm.getProperties().add(new Repeat(alarm.repeat));
				}

				if (calendarComponent instanceof VEvent) {
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

		// URL
		parseICalendarElementUrl(properties, iCalendarElement);

		// ATTACH
		parseICalendarElementAttachments(properties, iCalendarElement);

		// SEQUENCE
		parseICalendarElementSequence(properties, iCalendarElement);

		// CONFERENCE
		if (StringUtils.isNotBlank(iCalendarElement.conference)) {
			properties.add(new XProperty(CONFERENCE, iCalendarElement.conference));
		}

		// CONFERENCE ID
		if (StringUtils.isNotBlank(iCalendarElement.conferenceId)) {
			properties.add(new XProperty(CONFERENCE_ID, iCalendarElement.conferenceId));
		}

		// TEAMS
		ICal4jTeamsHelper.parseTeamsToICS(properties, iCalendarElement);

		return properties;
	}

	private static void parseICalendarElementAttachments(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.attachments != null && !iCalendarElement.attachments.isEmpty()) {
			for (AttachedFile attachment : iCalendarElement.attachments) {
				ParameterList params = new ParameterList();
				params.add(new XParameter(FILE_NAME, attachment.name));
				if (attachment.cid != null) {
					params.add(new XParameter(CID, attachment.cid));
				}
				try {
					Attach attach = new Attach(params, attachment.publicUrl);
					properties.add(attach);
				} catch (Exception e) {
					logger.warn("Attachment is not valid", e);
				}
			}
		}
	}

	private static void parseICalendarElementReccurId(PropertyList properties, ICalendarElement iCalendarElement) {
		if ((iCalendarElement instanceof VEventOccurrence) && ((VEventOccurrence) iCalendarElement).recurid != null) {
			RecurrenceId recurId = new RecurrenceId(convertToIcsDate(((VEventOccurrence) iCalendarElement).recurid));
			properties.add(recurId);
		}
	}

	private static void parseICalendarElementSequence(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.sequence != null && iCalendarElement.sequence > 0) {
			properties.add(new Sequence(iCalendarElement.sequence));
		}
	}

	private static void parseICalendarElementRRule(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.rrule != null) {

			Builder recur = new Recur.Builder();

			if (iCalendarElement.rrule.count != null) {
				recur.count(iCalendarElement.rrule.count);
			}

			if (iCalendarElement.rrule.until != null) {
				recur.until(convertToIcsDate(iCalendarElement.rrule.until));
			}

			if (iCalendarElement.rrule.frequency != null) {
				recur.frequency(Frequency.valueOf(iCalendarElement.rrule.frequency.name()));
			}

			if (iCalendarElement.rrule.interval != null) {
				recur.interval(iCalendarElement.rrule.interval);
			}

			if (iCalendarElement.rrule.bySecond != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.bySecond) {
					nl.add(value);

				}
				recur.secondList(nl);
			}

			if (iCalendarElement.rrule.byMinute != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.byMinute) {
					nl.add(value);
				}
				recur.minuteList(nl);
			}

			if (iCalendarElement.rrule.byHour != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.byHour) {
					nl.add(value);
				}
				recur.hourList(nl);
			}

			if (iCalendarElement.rrule.byDay != null) {
				WeekDayList wd = new WeekDayList();
				for (ICalendarElement.RRule.WeekDay weekDay : iCalendarElement.rrule.byDay) {
					wd.add(new net.fortuna.ical4j.model.WeekDay(new net.fortuna.ical4j.model.WeekDay(weekDay.day),
							weekDay.offset));

				}
				recur.dayList(wd);
			}

			if (iCalendarElement.rrule.byMonthDay != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.byMonthDay) {
					nl.add(value);
				}
				recur.monthDayList(nl);
			}

			if (iCalendarElement.rrule.byYearDay != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.byYearDay) {
					nl.add(value);
				}
				recur.yearDayList(nl);
			}

			if (iCalendarElement.rrule.byWeekNo != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.byWeekNo) {
					nl.add(value);
				}
				recur.weekNoList(nl);
			}

			if (iCalendarElement.rrule.byMonth != null) {
				NumberList nl = new NumberList();
				for (Integer value : iCalendarElement.rrule.byMonth) {
					nl.add(value);
				}
				recur.monthDayList(nl);
			}

			RRule rrule = new RRule(recur.build());
			properties.add(rrule);
		}
	}

	private static void parseICalendarElementExDate(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
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

	private static void parseICalendarElementRDate(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
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
			if (d.precision == Precision.Date) {
				rdate.getParameters().add(Value.DATE);
			} else {
				rdate.getParameters().add(Value.DATE_TIME);
			}
			properties.add(rdate);
		}
	}

	private static void parseICalendarElementCategories(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
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

	private static void parseICalendarElementOrganizer(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.organizer != null) {
			properties.add(parseICalendarElementOrganizer(iCalendarElement.organizer));
		}
	}

	private static void parseICalendarElementAttendees(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.attendees != null && !iCalendarElement.attendees.isEmpty()) {
			for (ICalendarElement.Attendee attendee : iCalendarElement.attendees) {
				properties.add(parseICalendarElementAttendee(attendee));
			}
		}
	}

	private static void parseICalendarElementStatus(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.status != null) {
			properties.add(new Status(iCalendarElement.status.name().toUpperCase()));
		}
	}

	private static void parseICalendarElementPriority(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.priority != null) {
			properties.add(new Priority(iCalendarElement.priority.intValue()));
		}
	}

	private static void parseICalendarElementDescription(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.description)) {
			HtmlToPlainText formater = new HtmlToPlainText();
			properties.add(new Description(formater.convert(iCalendarElement.description).trim()));

			String desc = iCalendarElement.description;
			if (!desc.startsWith("<html>")) {
				desc = "<html><body>" + desc + "</body></html>";
			}

			XProperty xAltDesc = new XProperty(ALT_DESC, desc);
			xAltDesc.getParameters().add(new FmtType("text/html"));
			properties.add(xAltDesc);
		}
	}

	private static void parseICalendarElementUrl(PropertyList<Property> properties, ICalendarElement iCalendarElement) {
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

	private static void parseICalendarElementLocation(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.location)) {
			properties.add(new Location(iCalendarElement.location));
		}
	}

	private static void parseICalendarElementClassification(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.classification != null) {
			properties.add(new Clazz(iCalendarElement.classification.name().toUpperCase()));
		}
	}

	private static void parseICalendarElementSummary(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (isStringNotNull(iCalendarElement.summary)) {
			properties.add(new Summary(iCalendarElement.summary));
		}
	}

	private static void parseICalendarElementDate(PropertyList<Property> properties,
			ICalendarElement iCalendarElement) {
		if (iCalendarElement.dtstart != null) {
			DtStart dtstart = new DtStart(convertToIcsDate(iCalendarElement.dtstart));
			properties.add(dtstart);
		}
	}

	private static void parseICalendarElementUid(PropertyList<Property> properties, String uid) {
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
				orga.setValue(MAIL_TO + organizer.mailto);
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

	@SuppressWarnings("unchecked")
	public static <T extends Date> T convertToIcsDate(BmDateTime date) {
		BmDateTimeWrapper bmDate = new BmDateTimeWrapper(date);
		if (date.precision == Precision.DateTime) {
			DateTime dt = new DateTime(bmDate.toUTCTimestamp());
			if (bmDate.containsTimeZone()) {
				TimeZone tz = tzRegistry.getTimeZone(date.timezone);
				dt.setTimeZone(tz);
			} else {
				dt.setTimeZone(tzRegistry.getTimeZone(utcTz.getId()));
			}
			return (T) dt;
		} else {
			return (T) new Date(bmDate.toTimestamp(utcTz.getId()));
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
