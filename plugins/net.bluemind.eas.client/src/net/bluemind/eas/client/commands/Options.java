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
package net.bluemind.eas.client.commands;

import java.util.List;
import java.util.Map.Entry;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.IEasCommand;
import net.bluemind.eas.client.OPClient;

public class Options implements IEasCommand<Void> {

	private static final Logger logger = LoggerFactory.getLogger(Options.class);

	@Override
	public Void run(AccountInfos ai, OPClient opc, AsyncHttpClient hc) throws Exception {
		BoundRequestBuilder om = hc.prepareOptions(ai.getUrl() + "?User=" + ai.getLogin() + "&DeviceId=" + ai.getDevId()
				+ "&DeviceType=" + ai.getDevType());

		om.setHeader("User-Agent", ai.getUserAgent());
		om.setHeader("Authorization", ai.authValue());
		ListenableFuture<Response> future = om.execute();
		Response r = future.get();
		if (r.getStatusCode() != 200) {
			logger.error("method failed:\n" + r.getStatusText() + "\n" + r.getResponseBody());
		} else {
			logger.info("Options OK");
		}
		List<Entry<String, String>> heads = r.getHeaders().entries();
		for (Entry<String, String> entry : heads) {
			System.out.println("S: Header " + entry.getKey() + " => " + entry.getValue());
		}
		return null;
	}

}
