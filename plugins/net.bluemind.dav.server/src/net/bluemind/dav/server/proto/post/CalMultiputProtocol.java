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
package net.bluemind.dav.server.proto.post;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemAdd;
import net.bluemind.calendar.api.VEventChanges.ItemDelete;
import net.bluemind.calendar.api.VEventChanges.ItemModify;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.post.VEventStuffPostProtocol.VEventStuffContext;
import net.bluemind.dav.server.proto.report.webdav.Create;
import net.bluemind.dav.server.proto.report.webdav.Remove;
import net.bluemind.dav.server.proto.report.webdav.Update;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;

public class CalMultiputProtocol {

	private static final Logger logger = LoggerFactory.getLogger(CalMultiputProtocol.class);

	public static CalMultiputResponse execute(LoggedCore lc, CalMultiputQuery query, VEventStuffContext ctx) {
		logger.info("{} events to process.", query.getEvents().size());
		CalMultiputResponse resp = new CalMultiputResponse(query.getPath());
		try {
			ICalendar calApi = lc.getCore().instance(ICalendar.class, getCalendarUid(ctx.ressource));

			VEventChanges changes = new VEventChanges();
			List<ItemAdd> create = new LinkedList<>();
			List<ItemModify> update = new LinkedList<>();
			List<ItemDelete> delete = new LinkedList<>();

			for (VEventPut eventPut : query.getEvents()) {
				ItemValue<VEventSeries> series = eventPut.getEvent();
				if (series == null && eventPut.getUpdateHref() != null) {
					// a delete
					String uid = getEventUid(eventPut);
					delete.add(ItemDelete.create(uid, true));
				} else if (series != null && eventPut.getUpdateHref() == null) {
					// create
					create.add(ItemAdd.create(series.uid, series.value, true));
				} else {
					// update
					update.add(ItemModify.create(getEventUid(eventPut), series.value, true));
				}
			}
			changes.add = create;
			changes.modify = update;
			changes.delete = delete;
			ContainerUpdatesResult res = calApi.updates(changes);

			List<Remove> removals = new ArrayList<>(res.removed.size());
			List<Create> creates = new ArrayList<>(res.added.size());
			List<Update> updates = new ArrayList<>(res.updated.size());

			for (String ia : res.added) {
				creates.add(new Create(ia, res.version));
			}
			for (String ia : res.updated) {
				updates.add(new Update(ia, res.version));
			}
			for (String ia : res.removed) {
				removals.add(new Remove(ia, res.version));
			}

			resp.setCreated(creates);
			resp.setRemoved(removals);
			resp.setUpdated(updates);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return resp;
	}

	private static String getEventUid(VEventPut vcp) {
		String ur = vcp.getUpdateHref();
		Matcher m = ResType.VSTUFF.matcher(ur);
		m.find();
		return m.group(3);
	}

	private static String getCalendarUid(DavResource dr) {
		ResType rt = dr.getResType();
		Matcher m = rt.matcher(dr.getPath());
		m.find();
		try {
			return URLDecoder.decode(m.group(2), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return m.group(2);
		}
	}

	public static MultiStatusBuilder getResponse(CalMultiputResponse response) {
		MultiStatusBuilder msb = new MultiStatusBuilder();
		StringBuilder sb = new StringBuilder(256);
		String path = response.getPath();
		int len = path.length();
		sb.append(path);
		for (Create c : response.getCreated()) {
			sb.setLength(len);
			sb.append(c.getUrlId()).append(".ics");
			String eventPath = sb.toString();
			Element prop = msb.newResponse(eventPath, 200);
			String et = SyncTokens.getEtag(eventPath, c.getLastMod());
			DOMUtils.createElementAndText(prop, "d:getetag", et);
			DOMUtils.createElementAndText(prop, "cso:uid", c.getUuid());
		}
		for (Update c : response.getUpdated()) {
			sb.setLength(len);
			sb.append(c.getUrlId()).append(".vcf");
			String eventPath = sb.toString();
			Element prop = msb.newResponse(eventPath, 200);
			String et = SyncTokens.getEtag(eventPath, c.getLastMod());
			DOMUtils.createElementAndText(prop, "d:getetag", et);
			DOMUtils.createElementAndText(prop, "cso:uid", c.getUuid());
		}
		for (Remove c : response.getRemoved()) {
			sb.setLength(len);
			sb.append(c.getUuid()).append(".vcf");
			msb.newResponse(sb.toString(), 404);
		}
		return msb;
	}
}
