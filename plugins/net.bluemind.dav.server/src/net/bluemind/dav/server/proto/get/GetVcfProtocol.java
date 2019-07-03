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
package net.bluemind.dav.server.proto.get;

import java.util.LinkedList;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.addressbook.adapter.VCardAdapter;
import net.bluemind.addressbook.adapter.VCardVersion;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dav.server.proto.DavHeaders;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.BookUtils;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.property.Uid;

public class GetVcfProtocol implements IDavProtocol<GetQuery, GetResponse<String>> {

	private static final Logger logger = LoggerFactory.getLogger(GetVcfProtocol.class);

	@Override
	public final void parse(final HttpServerRequest r, DavResource davRes, final Handler<GetQuery> handler) {
		GetQuery pq = new GetQuery(davRes);
		DavHeaders.parse(pq, r.headers());
		handler.handle(pq);
	}

	@Override
	public final void execute(LoggedCore lc, GetQuery query, Handler<GetResponse<String>> handler) {
		handler.handle(getCard(lc, query.getResource()));
	}

	private GetResponse<String> getCard(LoggedCore lc, DavResource dr) {
		GetResponse<String> gr = new GetResponse<>();

		try {
			ContainerDescriptor container = BookUtils.addressbook(lc, dr);
			IAddressBook bookApi = lc.getCore().instance(IAddressBook.class, container.uid);
			Matcher m = dr.getResType().matcher(dr.getPath());
			m.find();
			String cardUid = m.group(3);
			ItemValue<VCard> card = bookApi.getComplete(cardUid);
			if (card == null) {
				logger.warn("vcard '{}' not found", cardUid);
				gr.setStatus(404);
			} else {

				net.fortuna.ical4j.vcard.VCard ret = VCardAdapter.adaptCard(container.uid, card.value, VCardVersion.v3);
				ret.getProperties().add(new Uid(new LinkedList<Parameter>(), card.uid));
				gr.setValue(ret.toString());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			gr.setStatus(500);
		}

		return gr;
	}

	@Override
	public final void write(GetResponse<String> response, HttpServerResponse sr) {
		Buffer b = new Buffer();
		if (response.getValue() != null) {
			String vcfString = response.getValue();
			b.appendString(vcfString);
			sr.headers().set("Content-Type", "text/vcard; charset=\"utf-8\"");
			sr.headers().set("Content-Length", "" + b.length());
			logger.info("[{} Bytes]:\n{}", b.length(), vcfString);
		}
		sr.setStatusCode(response.getStatus()).end(b);
	}

}
