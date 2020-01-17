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
package net.bluemind.eas.command.meetingresponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IContentsImporter;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.data.calendarenum.AttendeeStatus;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.calendar.CalendarResponse;
import net.bluemind.eas.dto.calendar.CalendarResponse.InstanceType;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseRequest;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseRequest.Request;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseResponse;
import net.bluemind.eas.dto.meetingresponse.MeetingResponseResponse.Result.Status;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.VertxLazyLoader;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.meetingresponse.MeetingResponseRequestParser;
import net.bluemind.eas.serdes.meetingresponse.MeetingResponseResponseFormatter;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class MeetingResponseProtocol implements IEasProtocol<MeetingResponseRequest, MeetingResponseResponse> {

	private static final Logger logger = LoggerFactory.getLogger(MeetingResponseProtocol.class);

	private final IBackend backend;
	private final ISyncStorage store;

	public MeetingResponseProtocol() {
		backend = Backends.dataAccess();
		store = Backends.internalStorage();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<MeetingResponseRequest> parserResultHandler) {
		MeetingResponseRequestParser parser = new MeetingResponseRequestParser();
		MeetingResponseRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(final BackendSession bs, MeetingResponseRequest query,
			final Handler<MeetingResponseResponse> responseHandler) {
		final MeetingResponseResponse response = new MeetingResponseResponse();
		response.results = new ArrayList<MeetingResponseResponse.Result>(query.requests.size());

		final AtomicInteger toProcess = new AtomicInteger(query.requests.size());
		for (final Request request : query.requests) {
			try {
				ItemDataType dataClass = ItemDataType.EMAIL;
				Integer collectionId = Integer.parseInt(request.collectionId);
				HierarchyNode node = store.getHierarchyNode(bs, collectionId);
				dataClass = ItemDataType.getValue(node.containerType);

				ItemChangeReference ic = new ItemChangeReference(dataClass);
				ic.setServerId(CollectionItem.of(request.requestId));

				final MeetingResponseResponse.Result r = new MeetingResponseResponse.Result();
				final ItemChangeReference itemRef = ic;
				// The RequestId element is present in MeetingResponse
				// command responses only if it was present in the
				// corresponding MeetingResponse command request. The
				// RequestId element MUST NOT be present in the
				// MeetingResponse command request if the search:LongId
				// element is present.
				if (request.requestId != null && request.LongId == null) {
					r.requestId = request.requestId;
				}

				invitation(bs, itemRef, new Handler<CalendarResponse>() {

					@Override
					public void handle(CalendarResponse invitation) {
						if (invitation == null) {
							logger.error("Invalid meeting request for {}", r.requestId);
							r.status = Status.InvalidMeetingRequest;
						} else {
							if (invitation.instanceType == InstanceType.singleInstance
									&& invitation.recurrenceId != null) {
								request.instanceId = invitation.recurrenceId;
							}
							r.status = Status.Success;

							AttendeeStatus attendeeStatus = null;
							switch (request.userResponse) {
							case Accepted:
								attendeeStatus = AttendeeStatus.ACCEPT;
								break;
							case Declined:
								attendeeStatus = AttendeeStatus.DECLINE;
								break;
							case TentativelyAccepted:
								attendeeStatus = AttendeeStatus.TENTATIVE;
								break;
							default:
								break;
							}

							String itemUid = itemRef.getServerId().itemId;
							if (itemRef.getType() == ItemDataType.EMAIL) {
								itemUid = invitation.itemUid;
							}

							IContentsImporter importer = backend.getContentsImporter(bs);
							String calendarId = importer.importCalendarUserStatus(bs, itemUid, attendeeStatus,
									request.instanceId);

							// 2.2.3.18 CalendarId
							// If the meeting is declined, the response does not
							// contain a CalendarId element.
							if (attendeeStatus != AttendeeStatus.DECLINE) {
								r.calendarId = calendarId;
							}

							// delete the email
							deleteMeetingRequest(bs, itemRef);
						}

						response.results.add(r);
						subRequestProcessed(toProcess, responseHandler, response);
					}
				});

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				MeetingResponseResponse.Result r = new MeetingResponseResponse.Result();
				r.status = Status.ServerError;
				response.results.add(r);
				subRequestProcessed(toProcess, responseHandler, response);
			}

		}
	}

	private void subRequestProcessed(AtomicInteger toProcess, Handler<MeetingResponseResponse> responseHandler,
			MeetingResponseResponse response) {
		int now = toProcess.decrementAndGet();
		if (now == 0) {
			responseHandler.handle(response);
		}
	}

	private void invitation(BackendSession bs, ItemChangeReference ic, Handler<CalendarResponse> foundInvite) {
		Optional<AppData> optData = ic.getData();
		AppData loaded = null;
		if (!optData.isPresent()) {
			try {
				loaded = backend.getContentsExporter(bs).loadStructure(bs, null, ic);
				loaded.body = VertxLazyLoader.wrap(loaded.body);
			} catch (ActiveSyncException e) {
				logger.error(e.getMessage(), e);
				foundInvite.handle(null);
				return;
			}
		} else {
			loaded = optData.get();
		}
		CalendarResponse cr = null;
		if (ic.getType() == ItemDataType.EMAIL) {
			if (loaded.metadata.email != null) {
				cr = loaded.metadata.email.meetingRequest;
				logger.debug("Loaded invitation from email {}", cr);
			}
		} else if (ic.getType() == ItemDataType.CALENDAR) {
			cr = loaded.metadata.event;
			logger.debug("Loaded invitation from calendar {}", cr);
		}
		foundInvite.handle(cr);
	}

	private void deleteMeetingRequest(BackendSession bs, ItemChangeReference ic) {
		if (ic.getType() != ItemDataType.EMAIL) {
			logger.info("Can't delete meeting request for type {}", ic.getType());
			return;
		}
		try {
			IContentsImporter mailImporter = backend.getContentsImporter(bs);
			mailImporter.importMessageDeletion(bs, ItemDataType.EMAIL, Arrays.asList(ic.getServerId()), false);
		} catch (Exception t) {
			logger.error(t.getMessage(), t);
		}
	}

	@Override
	public void write(BackendSession bs, Responder responder, MeetingResponseResponse response,
			final Handler<Void> completion) {
		MeetingResponseResponseFormatter formatter = new MeetingResponseResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		formatter.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

			@Override
			public void onResult(Void data) {
				completion.handle(null);
			}
		});

	}

	@Override
	public String address() {
		return "eas.protocol.meetingresponse";
	}

}
