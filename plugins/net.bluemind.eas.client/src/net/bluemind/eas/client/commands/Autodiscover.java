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

import java.io.InputStream;

import com.google.common.io.ByteStreams;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;

import net.bluemind.eas.client.AccountInfos;
import net.bluemind.eas.client.IEasCommand;
import net.bluemind.eas.client.OPClient;

public class Autodiscover implements IEasCommand<Void> {

	@Override
	public Void run(AccountInfos ai, OPClient opc, AsyncHttpClient hc)
			throws Exception {

		BoundRequestBuilder om = hc.preparePost(ai.getUrl() + "?User="
				+ ai.getLogin() + "&DeviceId=" + ai.getDevId() + "&DeviceType="
				+ ai.getDevType());
		om.setHeader("User-Agent", ai.getUserAgent());
		om.setHeader("Authorization", ai.authValue());
		om.setHeader("Content-Type", "text/xml");

		InputStream is = Autodiscover.class.getClassLoader()
				.getResourceAsStream("data/autodiscover.xml");
		String tpl = new String(ByteStreams.toByteArray(is));
		tpl = tpl.replace("${email}", ai.getLogin());
		System.out.println(tpl);
		is.close();
		om.setBody(tpl);

		ListenableFuture<Response> future = om.execute();
		Response r = future.get();

		String resp = r.getResponseBody();
		System.err.println("resp:\n" + resp);

		return null;
	}
}
