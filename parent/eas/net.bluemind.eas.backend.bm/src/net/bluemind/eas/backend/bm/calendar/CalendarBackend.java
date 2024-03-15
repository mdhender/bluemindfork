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

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.Response;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarUids.UserCalendarType;
import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusy.Slot;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSAttachementData;
import net.bluemind.eas.backend.MSEvent;
import net.bluemind.eas.backend.MergedFreeBusy;
import net.bluemind.eas.backend.MergedFreeBusy.SlotAvailability;
import net.bluemind.eas.backend.bm.compat.OldFormats;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.backend.bm.mail.AttachmentHelper;
import net.bluemind.eas.backend.bm.user.UserBackend;
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
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest.Options.ConflicResolution;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Organizer;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.proxy.support.AHCWithProxy;

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
	public Changes getContentChanges(BackendSession bs, long version, CollectionId collectionId)
			throws ActiveSyncException {
		Changes changes = new Changes();

		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
			ICalendar service = getService(bs, folder.containerUid);

			ContainerChangeset<Long> changeset = service.changesetById(version);
			logger.debug(
					"[{}][{}] get calendar changes. created: {}, updated: {}, deleted: {}, folder: {}, version: {}",
					bs.getLoginAtDomain(), bs.getDevId(), changeset.created.size(), changeset.updated.size(),
					changeset.deleted.size(), folder.containerUid, version);

			changes.version = changeset.version;

			for (long id : changeset.created) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.CALENDAR, ChangeType.ADD));
			}

			for (long id : changeset.updated) {
				changes.items.add(getItemChange(collectionId, id, ItemDataType.CALENDAR, ChangeType.CHANGE));
			}

			for (long id : changeset.deleted) {
				ItemChangeReference ic = getItemChange(collectionId, id, ItemDataType.CALENDAR, ChangeType.DELETE);
				changes.items.add(ic);
			}

			logger.debug("getContentChanges({}, {}, version: {}) => {} entries.", bs.getLoginAtDomain(),
					folder.containerUid, version, changes.items.size());

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

	public CollectionItem store(BackendSession bs, CollectionId collectionId, Optional<String> sid,
			Optional<Date> recurId, IApplicationData data, ConflicResolution conflictPolicy, SyncState syncState)
			throws ActiveSyncException {
		CollectionItem ret = null;
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		ICalendar service = getService(bs, folder.containerUid);
		MSEvent msEvent = (MSEvent) data;

		Optional<List<AttachedFile>> attachments = storeAttachment(bs, msEvent);

		try {
			if (sid.isPresent()) {
				String serverId = sid.get();
				Long id = getItemId(serverId);

				if (id != null) {
					ItemValue<VEventSeries> item = service.getCompleteById(id);

					if (item == null) {
						logger.debug("Fail to find VEvent {}", id);
						return CollectionItem.of(collectionId, id);
					}

					if (conflictPolicy == ConflicResolution.SERVER_WINS && item.version > syncState.version) {
						throw new ActiveSyncException(
								"Both server and client changes. Conflict resolution is SERVER_WINS");
					}

					ConvertedVEvent de = converter.convert(bs, item.value, data, recurId, attachments);
					try {
						service.update(item.uid, de.vevent, true);
						ret = CollectionItem.of(collectionId, id);
						logger.info("Update event bs: {}, collection: {}, serverId: {}, event title: {}",
								bs.getLoginAtDomain(), folder.containerUid, serverId, de.vevent.main.summary);
					} catch (Exception e) {
						// trying to send a revert to the client (instead of
						// sending error ?)
						logger.error("Fail to update event bs:" + bs.getLoginAtDomain() + ", collection: "
								+ folder.containerUid + ", serverId: " + serverId + ", event title:"
								+ de.vevent.main.summary, e);
						service.touch(item.uid);
					}
				}

			} else {
				ConvertedVEvent de = converter.convert(bs, data, Optional.empty(), attachments);
				VEventSeries event = de.vevent;
				if (attachments.isPresent()) {
					event.main.attachments = attachments.get();
				}
				if (event.main.organizer.mailto == null) {
					event.main.organizer = new Organizer(bs.getUser().getDefaultEmail());
				}

				// BM-8000
				if (msEvent.getStartTime().getTime() == 0 && msEvent.getEndTime().getTime() == 0) {
					Date now = new Date();
					Date startTime = DateUtils.round(now, Calendar.HOUR);

					Calendar c = Calendar.getInstance();
					c.setTime(now);
					if (c.get(Calendar.MINUTE) < 30) {
						startTime = new Date(startTime.getTime() - 1800000);
					}
					Date endTime = new Date(startTime.getTime() + 1800000);
					event.main.dtstart = BmDateTimeWrapper.fromTimestamp(startTime.getTime(),
							msEvent.getTimeZone().getID(), Precision.DateTime);
					event.main.dtend = BmDateTimeWrapper.fromTimestamp(endTime.getTime(), msEvent.getTimeZone().getID(),
							Precision.DateTime);
				}

				String uid = UUID.randomUUID().toString();
				event.main.sequence = 0;
				service.create(uid, event, true);

				ItemValue<VEventSeries> created = service.getComplete(uid);
				ret = CollectionItem.of(collectionId, created.internalId);

				logger.info("Create event bs: {}, collection: {}, serverId: {}, event title: {}", bs.getLoginAtDomain(),
						folder.containerUid, ret, event.main.summary);
			}

		} catch (Exception e) {
			throw new ActiveSyncException(e);
		}

		return ret;
	}

	private Optional<List<AttachedFile>> storeAttachment(BackendSession bs, MSEvent msEvent) {
		if (msEvent.getAttachments() == null || msEvent.getAttachments().isEmpty()) {
			return Optional.empty();
		}

		IAttachment attachmentService = getAttachmentService(bs, bs.getUser().getDomain());

		List<AttachedFile> attachments = new ArrayList<>();
		msEvent.getAttachments().forEach(attachment -> {
			try {
				Stream document = streamFromByteSource(attachment.content);
				AttachedFile file = attachmentService.share(attachment.displayName, document);
				attachments.add(file);
				logger.info("Attach file {}", attachment.displayName);
			} catch (Exception e) {
				logger.warn("[{}] Failed to attach '{}'", bs.getLoginAtDomain(), attachment.displayName, e);
			}

		});
		return Optional.of(attachments);
	}

	private static Stream streamFromByteSource(ByteSource content) throws IOException {
		ByteBufOutputStream os = new ByteBufOutputStream(Unpooled.buffer());
		content.copyTo(os);
		return VertxStream.stream(Buffer.buffer(os.buffer()));
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
					ItemValue<VEventSeries> evt = service.getCompleteById(serverId.itemId);
					if (evt != null) {
						if (serverId.data.containsKey("instanceId") && evt.value.main != null) {
							long recurIdTime = ((Date) serverId.data.get("instanceId")).getTime();
							evt.value.occurrences = evt.value.occurrences == null ? new ArrayList<>()
									: new ArrayList<>(evt.value.occurrences);
							evt.value.occurrences
									.removeIf(occurrence -> BmDateTimeWrapper.toTimestamp(occurrence.recurid.iso8601,
											occurrence.recurid.timezone) == recurIdTime);
							evt.value.main.exdate = evt.value.main.exdate == null ? new HashSet<>()
									: new HashSet<>(evt.value.main.exdate);
							if (evt.value.main.dtstart.precision == Precision.DateTime) {
								evt.value.main.exdate.add(
										BmDateTimeWrapper.fromTimestamp(recurIdTime, evt.value.main.dtstart.timezone));
							} else {
								String iso = DateTimeFormatter.ISO_DATE.format(Instant.ofEpochMilli(recurIdTime));
								evt.value.main.exdate.add(new BmDateTime(iso, null, Precision.Date));
							}
							service.update(evt.uid, evt.value, true);
						} else {
							service.delete(evt.uid, true);
						}
					}

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
	 * @param itemId
	 * @param status
	 * @param instanceId
	 * @return
	 */
	public String updateUserStatus(BackendSession bs, long itemId, AttendeeStatus status, Date instanceId,
			String calendarUid) {
		try {
			ICalendar cs = getService(bs, ICalendar.class, calendarUid);

			String ownerUid = calendarUid.replace(ICalendarUids.TYPE + ":" + UserCalendarType.Default + ":", "").trim();
			UserBackend userBackend = new UserBackend();
			MSUser mSUSer = userBackend.getUser(bs, ownerUid);
			Set<String> userMails = mSUSer.getEmails();

			HierarchyNode f = storage.getHierarchyNode(bs.getUniqueIdentifier(), bs.getUser().getDomain(),
					bs.getUser().getUid(),
					ContainerHierarchyNode.uidFor(ICalendarUids.defaultUserCalendar(bs.getUser().getUid()), "calendar",
							bs.getUser().getDomain()));

			ItemValue<VEventSeries> vevent = cs.getCompleteById(itemId);
			ParticipationStatus partStatus = fromStatus(status);
			boolean rsvp = (partStatus == ParticipationStatus.NeedsAction);
			if (instanceId == null) {
				for (Attendee a : vevent.value.main.attendees) {
					if (userMails.contains(a.mailto)) {
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
					// Create exception
					VEventOccurrence exception = VEventOccurrence.fromEvent(vevent.value.main, exceptionStart);
					Iterator<Attendee> it = exception.attendees.iterator();
					while (it.hasNext()) {
						Attendee a = it.next();
						if (userMails.contains(a.mailto)) {
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
						if (userMails.contains(a.mailto)) {
							a.partStatus = partStatus;
							a.rsvp = rsvp;
						}
					}

					cs.update(vevent.uid, vevent.value, true);
				}

			}

			return CollectionItem.of(f.collectionId, itemId).toString();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public AppData fetch(BackendSession bs, ItemChangeReference ic) throws ActiveSyncException {
		try {
			HierarchyNode folder = storage.getHierarchyNode(bs, ic.getServerId().collectionId);
			ICalendar service = getService(bs, folder.containerUid);
			ItemValue<VEventSeries> event = service.getCompleteById(ic.getServerId().itemId);
			return toAppData(bs, ic.getServerId().collectionId, folder.containerUid, event);
		} catch (Exception e) {
			throw new ActiveSyncException(e.getMessage(), e);
		}
	}

	public Map<Long, AppData> fetchMultiple(BackendSession bs, CollectionId collectionId, List<Long> ids)
			throws ActiveSyncException {
		HierarchyNode folder = storage.getHierarchyNode(bs, collectionId);
		ICalendar service = getService(bs, folder.containerUid);

		List<ItemValue<VEventSeries>> events = service.multipleGetById(ids);
		Map<Long, AppData> res = new HashMap<>(ids.size());
		events.stream().forEach(event -> {
			try {
				AppData data = toAppData(bs, collectionId, folder.containerUid, event);
				res.put(event.internalId, data);
			} catch (Exception e) {
				logger.error("Fail to convert event {}", event.uid, e);
			}
		});

		return res;
	}

	private AppData toAppData(BackendSession bs, CollectionId collectionId, String calendarUid,
			ItemValue<VEventSeries> event) {
		MSEvent msEvent = new EventConverter().convert(bs, event);
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
		data.metadata.event.calendarUid = calendarUid;
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
		a.status = Availability.Status.SUCCESS;
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

	private ParticipationStatus fromStatus(AttendeeStatus status) {
		switch (status) {
		case ACCEPT:
			return ParticipationStatus.Accepted;
		case DECLINE:
			return ParticipationStatus.Declined;
		case RESPONSE_UNKNOWN:
		case NOT_RESPONDED:
			return ParticipationStatus.NeedsAction;
		default:
		case TENTATIVE:
			return ParticipationStatus.Tentative;
		}
	}

	private ICalendar getService(BackendSession bs, String containerUid) throws ServerFault {
		return getCalendarService(bs, containerUid);
	}

	public List<MoveItemsResponse.Response> move(BackendSession bs, HierarchyNode srcFolder, HierarchyNode dstFolder,
			List<CollectionItem> items) {
		List<MoveItemsResponse.Response> ret = new ArrayList<>(items.size());
		items.forEach(item -> {
			MoveItemsResponse.Response resp = new MoveItemsResponse.Response();

			try {
				ICalendar service = getService(bs, srcFolder.containerUid);
				ItemValue<VEventSeries> evt = service.getCompleteById(item.itemId);

				if (evt == null) {
					logger.error("Failed to find event {} in {}", item.itemId, srcFolder.containerUid);
					resp.srcMsgId = item.toString();
					resp.status = Status.SERVER_ERROR;
					ret.add(resp);
					return;
				}

				service = getService(bs, dstFolder.containerUid);
				String uid = UUID.randomUUID().toString();
				evt.value.main.sequence = 0;
				service.create(uid, evt.value, false);

				resp.status = Status.SUCCESS;
				resp.srcMsgId = item.toString();
				resp.dstMsgId = dstFolder.collectionId.getValue() + ":" + uid;
				ret.add(resp);

				service.delete(evt.uid, false);

			} catch (ServerFault sf) {
				logger.error(sf.getMessage());
				resp.srcMsgId = item.toString();
				resp.status = Status.SERVER_ERROR;
				ret.add(resp);
			}
		});

		return ret;
	}

	public MSAttachementData getAttachment(BackendSession bs, Map<String, String> parsedAttId) {
		String url = parsedAttId.get(AttachmentHelper.URL);
		try (FileBackedOutputStream fbos = new FileBackedOutputStream(32000, "bm-eas-calendar-getattachment");
				AsyncHttpClient ahc = AHCWithProxy.build(storage.getSystemConf())) {
			return ahc.prepareGet(url).execute(new AsyncCompletionHandler<MSAttachementData>() {

				@Override
				public State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
					fbos.write(bodyPart.getBodyPartBytes());
					return State.CONTINUE;
				}

				@Override
				public MSAttachementData onCompleted(Response response) throws Exception {
					return new MSAttachementData("application/octet-stream", DisposableByteSource.wrap(fbos));
				}
			}).get(20, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

}
