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
package net.bluemind.dav.server.proto.report.webdav;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerResponse;
import org.w3c.dom.Element;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.report.IReportExecutor;
import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.BookUtils;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.DavStore;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.todolist.api.ITodoList;

public class SyncCollectionExecutor implements IReportExecutor {

	private static final QName root = WDReports.SYNC_COLLECTION;
	private static final Logger logger = LoggerFactory.getLogger(SyncCollectionExecutor.class);

	@Override
	public ReportResponse execute(LoggedCore lc, ReportQuery rq) {
		SyncCollectionQuery scq = (SyncCollectionQuery) rq;
		String st = scq.getSyncToken();
		DavStore ds = new DavStore(lc);
		DavResource dr = ds.from(rq.getPath());
		ResType rt = dr.getResType();
		Long lastSync = SyncTokens.getDate(st);
		ContainerChangeset<String> changelog = null;
		switch (rt) {
		case VSTUFF_CONTAINER:
		case VCARDS_CONTAINER:
			break;
		default:
			logger.warn("Don't know how to sync " + scq.getPath());
			changelog = new ContainerChangeset<>();
			return response(dr, changelog, lastSync, scq.getProps());
		}
		ContainerDescriptor cd = lc.vStuffContainer(dr);
		logger.info("************* lastSync from sync-token is {}", lastSync);
		try {
			if ("calendar".equals(cd.type)) {
				ICalendar api = lc.getCore().instance(ICalendar.class, cd.uid);
				changelog = api.changeset(lastSync);
			} else if ("todolist".equals(cd.type)) {
				ITodoList api = lc.getCore().instance(ITodoList.class, cd.uid);
				changelog = api.changeset(lastSync);
			} else if (rt == ResType.VCARDS_CONTAINER) {
				ContainerDescriptor book = BookUtils.addressbook(lc, dr);
				IAddressBook api = lc.getCore().instance(IAddressBook.class, book.uid);
				changelog = api.changeset(lastSync);
			} else {
				logger.warn("Don't know how to sync " + scq.getPath());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return response(dr, changelog, lastSync, scq.getProps());
	}

	private SyncCollectionResponse response(DavResource dr, ContainerChangeset<String> cc, Long since,
			List<QName> props) {
		LinkedList<Create> creates = new LinkedList<Create>();
		LinkedList<Update> updates = new LinkedList<Update>();
		LinkedList<Remove> removals = new LinkedList<Remove>();

		Long newVersion = since;
		if (cc != null) {
			newVersion = cc.version;
			for (String uid : cc.created) {
				creates.add(new Create(uid, cc.version));
			}
			for (String uid : cc.updated) {
				updates.add(new Update(uid, cc.version));
			}
			for (String uid : cc.deleted) {
				removals.add(new Remove(uid, cc.version));
			}
		}
		logger.info("[{}] new version is {}", dr.getPath(), newVersion);
		String tokenString = SyncTokens.get(dr, newVersion);
		return new SyncCollectionResponse(dr.getPath(), root, dr.getResType(), tokenString, creates, updates, removals,
				props);
	}

	@Override
	public void write(ReportResponse rr, HttpServerResponse sr) {
		SyncCollectionResponse scr = (SyncCollectionResponse) rr;
		MultiStatusBuilder msb = new MultiStatusBuilder();

		for (Create c : scr.getCreates()) {
			addResponse(scr, msb, c);
		}
		for (Update c : scr.getUpdates()) {
			addResponse(scr, msb, c);
		}
		for (Remove r : scr.getRemovals()) {
			msb.newResponse(changePath(scr, r), 404);
		}
		Element ste = DOMUtils.createElement(msb.root(), "d:sync-token");
		ste.setTextContent(scr.getNewToken());

		msb.sendAs(sr, true);
	}

	private String changePath(SyncCollectionResponse scr, IChange c) {
		StringBuilder sb = new StringBuilder(256);
		sb.append(scr.getHref());
		try {
			sb.append(URLEncoder.encode(c.getUrlId(), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		switch (scr.getType()) {
		case VCARDS_CONTAINER:
			sb.append(".vcf");
			break;
		case VSTUFF_CONTAINER:
			sb.append(".ics");
			break;
		default:
			logger.error("Can't find change path for type {}", scr.getType());
		}
		return sb.toString();
	}

	private void addResponse(SyncCollectionResponse scr, MultiStatusBuilder msb, IChange c) {
		String path = changePath(scr, c);
		Element prop = msb.newResponse(path, 200);
		for (QName qn : scr.getProps()) {
			Element pv = DOMUtils.createElement(prop, qn.getPrefix() + ":" + qn.getLocalPart());
			switch (qn.getLocalPart()) {
			case "getcontenttype":
				pv.setTextContent("text/calendar;charset=utf-8");
				break;
			case "getetag":
				logger.info("**** [{}] c.lastMod: {}", c.getUuid(), c.getLastMod());
				pv.setTextContent(SyncTokens.getEtag(path, c.getLastMod()));
				break;
			default:
				logger.error("Don't know how to handle " + qn.getLocalPart());
			}
		}
	}

	@Override
	public QName getKind() {
		return root;
	}

}
