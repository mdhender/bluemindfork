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
package net.bluemind.eas.backend.bm.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;

import com.google.common.collect.ImmutableList;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.backend.MergedFreeBusy;
import net.bluemind.eas.backend.MergedFreeBusy.SlotAvailability;
import net.bluemind.eas.backend.bm.compat.OldFormats;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.data.calendarenum.AttendeeStatus;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyType;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.base.DisposableByteSource;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse.Response.Status;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient.Availability;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options.ConflicResolution;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;

public class CalendarBackend extends CoreConnect {

	private EventConverter converter;
	private final ISyncStorage storage;

	public CalendarBackend(ISyncStorage storage) {
		converter = new EventConverter();
		this.storage = storage;
	}

	/**
	 * @param bs
	 * @param version
	 * @param collectionId
	 * @return
	 * @throws ActiveSyncException
	 */
	public Changes getContentChanges(BackendSession bs, long version, Integer collectionId) throws ActiveSyncException {

		Changes changes = new Changes();

		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
			ICalendar service = getService(bs, folder.containerUid);

			ContainerChangeset<String> changeset = service.changeset(version);
			logger.debug(
					"[{}][{}] get calendar changes. created: {}, updated: {}, deleted: {}, folder: {}, version: {}",
					bs.getLoginAtDomain(), bs.getDevId(), changeset.created.size(), changeset.updated.size(),
					changeset.deleted.size(), folder.containerUid, version);

			changes.version = changeset.version;

			for (String uid : changeset.created) {
				changes.items.add(getItemChange(collectionId, uid, ItemDataType.CALENDAR, ChangeType.ADD));
			}

			for (String uid : changeset.updated) {
				changes.items.add(getItemChange(collectionId, uid, ItemDataType.CALENDAR, ChangeType.CHANGE));
			}

			for (String del : changeset.deleted) {
				ItemChangeReference ic = getItemChange(collectionId, del, ItemDataType.CALENDAR, ChangeType.DELETE);
				changes.items.add(ic);
			}

			logger.debug("getContentChanges(" + bs.getLoginAtDomain() + ", " + folder.containerUid + ", version: "
					+ version + ") => " + changes.items.size() + " entries.");

		} catch (ServerFault e) {
			if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
				logger.warn(e.getMessage());
			} else {
				logger.error(e.getMessage(), e);
			}
			changes.version = version;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// BM-7227
			// Something went wrong
			// Send current version number to prevent full sync
			changes.version = version;
		}

		return changes;
	}

	public CollectionItem store(BackendSession bs, Integer collectionId, Optional<String> sid, IApplicationData data,
			ConflicResolution conflictPolicy, SyncState syncState) throws ActiveSyncException {
		CollectionItem ret = null;
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		ICalendar service = getService(bs, folder.containerUid);
		try {
			if (sid.isPresent()) {
				String serverId = sid.get();
				String uid = getItemUid(serverId);

				if (uid != null && !uid.isEmpty()) {
					ItemValue<VEventSeries> item = service.getComplete(uid);

					if (item == null) {
						logger.debug("Fail to find VEvent {}", uid);
						return CollectionItem.of(collectionId, uid);
					}

					if (conflictPolicy == ConflicResolution.SERVER_WINS && item.version > syncState.version) {
						throw new ActiveSyncException(
								"Both server and client changes. Conflict resolution is SERVER_WINS");
					}

					VEventSeries oldEvent = item.value;
					ConvertedVEvent de = converter.convert(bs, oldEvent, data);

					VEventSeries event = de.vevent;

					// BM-5755
					event.main.rdate = oldEvent.main.rdate;

					if (event.main.description == null) {
						// GLAG-72 desc can be ghosted
						event.main.description = oldEvent.main.description;
					}

					// BM-8000
					MSEvent d = (MSEvent) data;
					if (d.getBusyStatus() == null) {
						event.main.transparency = oldEvent.main.transparency;
					}

					if (d.getStartTime().getTime() == 0 && d.getEndTime().getTime() == 0) {
						event.main.dtstart = oldEvent.main.dtstart;
						event.main.dtend = oldEvent.main.dtend;
					}

					if (d.getLocation() == null) {
						event.main.location = oldEvent.main.location;
					}

					if (d.getReminder() == null) {
						event.main.alarm = oldEvent.main.alarm;
					}

					if (d.getSensitivity() == null) {
						event.main.classification = oldEvent.main.classification;
					}

					if (d.getSubject() == null) {
						event.main.summary = oldEvent.main.summary;
					}

					if (d.getAttendees() == null) {
						event.main.attendees = oldEvent.main.attendees;
					} else {
						for (Attendee a : event.main.attendees) {
							if (a.partStatus == null) {
								for (Attendee o : oldEvent.main.attendees) {
									if (a.mailto.equals(o.mailto)) {
										a.partStatus = o.partStatus;
									}
								}
							}
						}
					}

					if (!event.main.attendees.isEmpty() && event.main.organizer.mailto == null) {
						if (oldEvent.main.organizer != null) {
							event.main.organizer = oldEvent.main.organizer;
						} else {
							event.main.organizer = new Organizer(bs.getUser().getDefaultEmail());
						}
					}
					event.main.categories = oldEvent.main.categories;

					try {
						service.update(uid, event, true);
						ret = CollectionItem.of(collectionId, uid);
						logger.info("Update event bs:" + bs.getLoginAtDomain() + ", collection: " + folder.containerUid
								+ ", serverId: " + serverId + ", event title:" + event.main.summary);
					} catch (Exception e) {
						// trying to send a revert to the client (instead of
						// sending error ?)
						logger.error("Fail to update event bs:" + bs.getLoginAtDomain() + ", collection: "
								+ folder.containerUid + ", serverId: " + serverId + ", event title:"
								+ event.main.summary, e);
						service.touch(uid);
					}
				}

			} else {
				ConvertedVEvent de = converter.convert(bs, data);
				VEventSeries event = de.vevent;

				if (event.main.organizer.mailto == null) {
					event.main.organizer = new Organizer(bs.getUser().getDefaultEmail());
				}

				MSEvent d = (MSEvent) data;

				// BM-8000
				if (d.getStartTime().getTime() == 0 && d.getEndTime().getTime() == 0) {
					Date now = new Date();
					Date startTime = DateUtils.round(now, Calendar.HOUR);

					Calendar c = Calendar.getInstance();
					c.setTime(now);
					if (c.get(Calendar.MINUTE) < 30) {
						startTime = new Date(startTime.getTime() - 1800000);
					}
					Date endTime = new Date(startTime.getTime() + 1800000);
					event.main.dtstart = BmDateTimeWrapper.fromTimestamp(startTime.getTime(), d.getTimeZone().getID(),
							Precision.DateTime);
					event.main.dtend = BmDateTimeWrapper.fromTimestamp(endTime.getTime(), d.getTimeZone().getID(),
							Precision.DateTime);
				}

				String uid = UUID.randomUUID().toString();
				service.create(uid, event, true);
				ret = CollectionItem.of(collectionId, uid);
				logger.info("Create event bs:" + bs.getLoginAtDomain() + ", collection: " + folder.containerUid
						+ ", serverId: " + ret + ", event title:" + event.main.summary);

			}

		} catch (ServerFault e) {
			logger.error("error during storing event", e);
			throw new ActiveSyncException(e);
		} catch (Exception e) {
			logger.error("error during storing event", e);
			throw new ActiveSyncException(e);
		}

		return ret;
	}

	/**
	 * @param bs
	 * @param serverIds
	 * @throws ActiveSyncException
	 */
	public void delete(BackendSession bs, Collection<CollectionItem> serverIds) throws ActiveSyncException {
		if (serverIds != null) {
			try {
				for (CollectionItem serverId : serverIds) {
					HierarchyNode folder = storage.getHierarchyNode(bs, serverId.collectionId);
					ICalendar service = getService(bs, folder.containerUid);

					service.delete(serverId.itemId, true);
				}
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					throw new ActiveSyncException(e);
				}
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param bs
	 * @param data
	 * @param status
	 * @return
	 */
	public String updateUserStatus(BackendSession bs, String eventUid, AttendeeStatus status, Date instanceId) {
		try {
			ICalendar cs = getService(bs, ICalendar.class, ICalendarUids.defaultUserCalendar(bs.getUser().getUid()));

			HierarchyNode f = storage.getHierarchyNode(bs.getUser().getDomain(), bs.getUser().getUid(),
					ContainerHierarchyNode.uidFor(ICalendarUids.defaultUserCalendar(bs.getUser().getUid()), "calendar",
							bs.getUser().getDomain()));

			ItemValue<VEventSeries> vevent = cs.getComplete(eventUid);

			ParticipationStatus partStatus = fromStatus(status);
			boolean rsvp = (partStatus == ParticipationStatus.NeedsAction);
			if (instanceId == null) {
				for (VEvent.Attendee a : vevent.value.main.attendees) {
					if (bs.getUser().getEmails().contains(a.mailto)) {
						a.partStatus = partStatus;
						a.rsvp = rsvp;
					}
				}
				cs.update(vevent.uid, vevent.value, true);
			} else {
				BmDateTime exceptionStart = BmDateTimeWrapper.fromTimestamp(instanceId.getTime(),
						vevent.value.main.dtstart.timezone);
				VEventOccurrence rec = vevent.value.occurrence(exceptionStart);
				if (rec == null) {
					// FIXME sure ?
					// Create exception
					VEventOccurrence exception = VEventOccurrence.fromEvent(vevent.value.main, exceptionStart);
					Iterator<Attendee> it = exception.attendees.iterator();
					while (it.hasNext()) {
						Attendee a = it.next();
						if (bs.getUser().getEmails().contains(a.mailto)) {
							a.partStatus = partStatus;
							a.rsvp = rsvp;
						}
					}
					long duration = BmDateTimeWrapper.toTimestamp(vevent.value.main.dtend.iso8601,
							vevent.value.main.dtend.timezone)
							- BmDateTimeWrapper.toTimestamp(vevent.value.main.dtstart.iso8601,
									vevent.value.main.dtstart.timezone);

					exception.dtstart = exceptionStart;
					exception.dtend = BmDateTimeWrapper.fromTimestamp(instanceId.getTime() + duration,
							exception.dtstart.timezone);

					vevent.value.occurrences = ImmutableList.<VEventOccurrence>builder()
							.addAll(vevent.value.occurrences).add(exception).build();
					cs.update(vevent.uid, vevent.value, true);
				} else {
					Iterator<Attendee> it = rec.attendees.iterator();
					while (it.hasNext()) {
						Attendee a = it.next();
						if (bs.getUser().getEmails().contains(a.mailto)) {
							a.partStatus = partStatus;
							a.rsvp = rsvp;
						}
					}
					cs.update(vevent.uid, vevent.value, true);
				}

			}

			return getServerId(f.collectionId, vevent.uid);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public AppData fetch(BackendSession bs, ItemChangeReference ic) throws ActiveSyncException {
		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, ic.getServerId().collectionId);
			ICalendar service = getService(bs, folder.containerUid);

			ItemValue<VEventSeries> event = service.getComplete(ic.getServerId().itemId);
			AppData data = toAppData(bs, ic.getServerId().collectionId, event);

			return data;
		} catch (Exception e) {
			throw new ActiveSyncException(e.getMessage(), e);
		}
	}

	public Map<String, AppData> fetchMultiple(BackendSession bs, int collectionId, List<String> uids)
			throws ActiveSyncException {
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		ICalendar service = getService(bs, folder.containerUid);

		List<ItemValue<VEventSeries>> events = service.multipleGet(uids);
		Map<String, AppData> res = new HashMap<String, AppData>(uids.size());
		events.stream().forEach(event -> {
			try {
				AppData data = toAppData(bs, collectionId, event);
				res.put(event.uid, data);
			} catch (Exception e) {
				logger.error("Fail to convert event {}", event.uid, e);
			}
		});

		return res;
	}

	private AppData toAppData(BackendSession bs, int collectionId, ItemValue<VEventSeries> event) {
		MSEvent msEvent = new EventConverter().convert(bs.getUser(), event);
		CalendarResponse cr = OldFormats.update(bs, msEvent, bs.getUser(), collectionId);
		AppData data = AppData.of(cr);

		if (!msEvent.getDescription().trim().isEmpty()) {
			final AirSyncBaseResponse airSyncBase = new AirSyncBaseResponse();
			airSyncBase.body = new AirSyncBaseResponse.Body();
			airSyncBase.body.type = BodyType.PlainText;
			airSyncBase.body.data = DisposableByteSource.wrap(msEvent.getDescription().trim());
			airSyncBase.body.estimatedDataSize = (int) airSyncBase.body.data.size();
			data.body = LazyLoaded.loaded(airSyncBase);
		}
		return data;
	}

	public Availability fetchAvailability(BackendSession bs, String entryUid, Date start, Date end) {

		StringBuilder sb = new StringBuilder();
		Calendar cal = new GregorianCalendar();
		cal.setTime(start);
		if (entryUid == null) {
			while (cal.getTime().before(end)) {
				sb.append(SlotAvailability.NoData.toString());
				cal.add(Calendar.MINUTE, 30);
			}
		} else {
			IVFreebusy fbApi = getService(bs, IVFreebusy.class, IFreebusyUids.getFreebusyContainerUid(entryUid));
			BmDateTime dtstart = BmDateTimeWrapper.fromTimestamp(start.getTime());
			BmDateTime dtend = BmDateTimeWrapper.fromTimestamp(end.getTime());
			VFreebusyQuery query = VFreebusyQuery.create(dtstart, dtend);
			try {
				VFreebusy fb = fbApi.get(query);
				while (cal.getTime().before(end)) {
					sb.append(availability(fb.slots, cal.getTime()).toString());
					cal.add(Calendar.MINUTE, 30);
				}
			} catch (ServerFault sf) {
				if (sf.getCode() == ErrorCode.PERMISSION_DENIED) {
					while (cal.getTime().before(end)) {
						sb.append(SlotAvailability.NoData.toString());
						cal.add(Calendar.MINUTE, 30);
					}
				}
			}

		}
		Availability a = new Availability();
		a.status = Availability.Status.Success;
		a.mergedFreeBusy = sb.toString();

		return a;
	}

	private MergedFreeBusy.SlotAvailability availability(Collection<VFreebusy.Slot> freeBusyIntervals, Date time) {
		SlotAvailability ret = SlotAvailability.Free;
		for (Slot fbi : freeBusyIntervals) {
			Date start = new BmDateTimeWrapper(fbi.dtstart).toDate();
			Date end = new BmDateTimeWrapper(fbi.dtend).toDate();
			if (time.equals(start) || (time.after(start) && time.before(end))) {
				switch (fbi.type) {
				case BUSY:
					ret = SlotAvailability.Busy;
					break;
				case BUSYUNAVAILABLE:
					ret = SlotAvailability.OutOfOffice;
					break;
				case BUSYTENTATIVE:
					ret = SlotAvailability.Tentative;
					break;
				default:
					break;
				}
			}
			logger.debug("{} compared to [{} - {}]{}", time, start, end, fbi.type);
		}

		return ret;
	}

	private VEvent.ParticipationStatus fromStatus(AttendeeStatus status) {
		switch (status) {
		case ACCEPT:
			return VEvent.ParticipationStatus.Accepted;
		case DECLINE:
			return VEvent.ParticipationStatus.Declined;
		case RESPONSE_UNKNOWN:
		case NOT_RESPONDED:
			return VEvent.ParticipationStatus.NeedsAction;
		default:
		case TENTATIVE:
			return VEvent.ParticipationStatus.Tentative;
		}
	}

	private ICalendar getService(BackendSession bs, String containerUid) throws ServerFault {
		return getCalendarService(bs, containerUid);
	}

	public List<MoveItemsResponse.Response> move(BackendSession bs, HierarchyNode srcFolder, HierarchyNode dstFolder,
			List<CollectionItem> items) {

		List<MoveItemsResponse.Response> ret = new ArrayList<MoveItemsResponse.Response>(items.size());
		items.forEach(item -> {
			MoveItemsResponse.Response resp = new MoveItemsResponse.Response();

			try {
				ICalendar service = getService(bs, srcFolder.containerUid);

				ItemValue<VEventSeries> evt = service.getComplete(item.itemId);

				service.delete(item.itemId, false);

				service = getService(bs, dstFolder.containerUid);
				String uid = UUID.randomUUID().toString();
				service.create(uid, evt.value, false);

				resp.status = Status.Success;
				resp.srcMsgId = item.toString();
				resp.dstMsgId = dstFolder.collectionId + ":" + uid;
				ret.add(resp);

			} catch (ServerFault sf) {
				logger.error(sf.getMessage());
				resp.status = Status.ServerError;
				ret.add(resp);

			}
		});

		return ret;
	}

}
