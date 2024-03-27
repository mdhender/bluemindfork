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
package net.bluemind.eas.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.EasHeaders;
import net.bluemind.vertx.common.request.Requests;

public final class OptionsHandler implements Handler<AuthorizedDeviceQuery> {

	private static final Logger logger = LoggerFactory.getLogger(OptionsHandler.class);

	@Override
	public void handle(AuthorizedDeviceQuery event) {
		Requests.tagUserLogin(event.request(), event.loginAtDomain());
		HttpServerRequest req = event.request();
		HttpServerResponse resp = req.response();
		MultiMap headers = resp.headers();
		headers.add(HttpHeaders.SERVER, "Microsoft-IIS/7.5");
		headers.add(EasHeaders.Server.MS_SERVER, "14.3");

		// BM-4843

		String uaHeader = event.request().headers().get("User-Agent");
		String ua = uaHeader != null ? uaHeader.toLowerCase() : "";

		logger.info("Handling OPTIONS: ua: {}", ua);

		if (!ua.contains("apple")) {
			headers.add(EasHeaders.Server.PROTOCOL_VERSIONS, "2.0,2.1,2.5,12.0,12.1,14.0,14.1");
		} else {
			headers.add(EasHeaders.Server.PROTOCOL_VERSIONS, "2.0,2.1,2.5,12.0,12.1,14.0,14.1,16.0,16.1");
		}

		headers.add(EasHeaders.Server.SUPPORTED_COMMANDS,
				"Sync,SendMail,SmartForward,SmartReply,GetAttachment,GetHierarchy,CreateCollection,DeleteCollection,MoveCollection,Find,FolderSync,FolderCreate,FolderDelete,FolderUpdate,MoveItems,GetItemEstimate,MeetingResponse,Search,Settings,Ping,ItemOperations,Provision,ResolveRecipients,ValidateCert");
		headers.add("Public", "OPTIONS,POST");
		headers.add(HttpHeaders.ALLOW, "OPTIONS,POST");
		headers.add(HttpHeaders.CACHE_CONTROL, "private");
		resp.setStatusCode(200).end();

	}

}
