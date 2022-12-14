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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.addressbook.adapter.AddressbookOwner;
import net.bluemind.addressbook.adapter.VCardAdapter;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCardChanges;
import net.bluemind.addressbook.api.VCardChanges.ItemAdd;
import net.bluemind.addressbook.api.VCardChanges.ItemDelete;
import net.bluemind.addressbook.api.VCardChanges.ItemModify;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.proto.report.webdav.Create;
import net.bluemind.dav.server.proto.report.webdav.Remove;
import net.bluemind.dav.server.proto.report.webdav.Update;
import net.bluemind.dav.server.store.BookUtils;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.dav.server.store.SyncTokens;
import net.bluemind.dav.server.xml.DOMUtils;
import net.bluemind.dav.server.xml.MultiStatusBuilder;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;
import net.bluemind.vertx.common.Body;
import net.fortuna.ical4j.vcard.Property.Id;
import net.fortuna.ical4j.vcard.VCard;

public class BookMultiputProtocol implements IDavProtocol<BookMultiputQuery, BookMultiputResponse> {

	private static final Logger logger = LoggerFactory.getLogger(BookMultiputProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<BookMultiputQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {

			@Override
			public void handle(Buffer body) {
				BookMultiputQuery bmq = new BookMultiputQueryParser().parse(davRes, r.headers(), body);
				handler.handle(bmq);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, BookMultiputQuery query, Handler<BookMultiputResponse> handler) {
		logger.info("{} vcards to process.", query.getVcards().size());
		try {
			ContainerDescriptor bookFolder = BookUtils.addressbook(lc, query.getResource());
			IAddressBook bookApi = lc.getCore().instance(IAddressBook.class, bookFolder.uid);
			VCardChanges changes = new VCardChanges();
			List<ItemAdd> create = new LinkedList<>();
			List<ItemModify> update = new LinkedList<>();
			List<ItemDelete> delete = new LinkedList<>();

			for (VCardPut vcp : query.getVcards()) {
				VCard vc = vcp.getVcard();
				if (vc == null && vcp.getUpdateHref() != null) {
					// a delete
					String uid = getCardId(vcp);
					delete.add(ItemDelete.create(uid));
				} else if (vc != null && vcp.getUpdateHref() == null) {
					// create
					net.bluemind.addressbook.api.VCard coreCard = coreCard(lc, vcp, bookFolder);
					String uid = vc.getProperty(Id.UID).getValue();
					create.add(ItemAdd.create(uid, coreCard));
				} else {
					// update
					net.bluemind.addressbook.api.VCard coreCard = coreCard(lc, vcp, bookFolder);
					update.add(ItemModify.create(getCardId(vcp), coreCard));
				}
			}
			changes.add = create;
			changes.modify = update;
			changes.delete = delete;
			// We don't want server changes
			ContainerUpdatesResult res = bookApi.updates(changes);

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

			BookMultiputResponse bmr = new BookMultiputResponse(query.getPath());
			bmr.setCreated(creates);
			bmr.setRemoved(removals);
			bmr.setUpdated(updates);
			handler.handle(bmr);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			BookMultiputResponse bmr = new BookMultiputResponse(query.getPath());
			handler.handle(bmr);
		}

	}

	private net.bluemind.addressbook.api.VCard coreCard(LoggedCore lc, VCardPut vcp, ContainerDescriptor bookFolder) {
		VCard vc = vcp.getVcard();
		net.bluemind.addressbook.api.VCard coreCard = VCardAdapter.adaptCard(vc, s -> s,
				Optional.of(new AddressbookOwner(lc.getDomain(), lc.getUser().uid, Kind.USER)), getAllTags(lc)).value;
		if (coreCard.kind == net.bluemind.addressbook.api.VCard.Kind.group) {
			coreCard.organizational.member = coreCard.organizational.member.stream().map(m -> {
				m.containerUid = bookFolder.uid;
				return m;
			}).collect(Collectors.toList());
		}
		return coreCard;
	}

	private List<TagRef> getAllTags(LoggedCore lc) {
		return lc.getCore().instance(ITags.class, ITagUids.defaultUserTags(lc.getUser().uid)).all().stream()
				.map(tag -> TagRef.create(ITagUids.defaultUserTags(lc.getUser().uid), tag))
				.collect(Collectors.toList());
	}

	private String getCardId(VCardPut vcp) {
		String ur = vcp.getUpdateHref();
		Matcher m = ResType.VCARD.matcher(ur);
		m.find();
		return m.group(3);
	}

	/**
	 * <code> <?xml version='1.0' encoding='UTF-8'?> <multistatus xmlns='DAV:'>
	 * <response>
	 * <href>/addressbooks/__uids__/EB397106-443A-47BE-9CA5-558037E137BF/addressbook/d8a1e5c3f788b232c30f09b2f727b3db.vcf</href>
	 * <propstat> <prop> <getetag>"0225f4f35a84402cbd0e57e7734b1a4f"</getetag>
	 * <uid xmlns=
	 * 'http://calendarserver.org/ns/'>bfc1edbf-db36-4469-ba4d-7f450ed5c759</uid>
	 * </prop> <status>HTTP/1.1 200 OK</status> </propstat> </response>
	 * </multistatus> </code>
	 **/
	@Override
	public void write(BookMultiputResponse response, HttpServerResponse sr) {
		MultiStatusBuilder msb = new MultiStatusBuilder();
		StringBuilder sb = new StringBuilder(256);
		String path = response.getPath();
		int len = path.length();
		sb.append(path);
		for (Create c : response.getCreated()) {
			sb.setLength(len);
			sb.append(c.getUrlId()).append(".vcf");
			String cardPath = sb.toString();
			Element prop = msb.newResponse(cardPath, 200);
			String et = SyncTokens.getEtag(cardPath, c.getLastMod());
			DOMUtils.createElementAndText(prop, "d:getetag", et);
			DOMUtils.createElementAndText(prop, "cso:uid", c.getUuid());
		}
		for (Update c : response.getUpdated()) {
			sb.setLength(len);
			sb.append(c.getUrlId()).append(".vcf");
			String cardPath = sb.toString();
			Element prop = msb.newResponse(cardPath, 200);
			String et = SyncTokens.getEtag(cardPath, c.getLastMod());
			DOMUtils.createElementAndText(prop, "d:getetag", et);
			DOMUtils.createElementAndText(prop, "cso:uid", c.getUuid());
		}
		for (Remove c : response.getRemoved()) {
			sb.setLength(len);
			sb.append(c.getUuid()).append(".vcf");
			msb.newResponse(sb.toString(), 404);
		}
		msb.sendAs(sr, true);
	}
}
