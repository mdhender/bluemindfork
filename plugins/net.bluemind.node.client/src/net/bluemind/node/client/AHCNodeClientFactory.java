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
package net.bluemind.node.client;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.INodeClientFactory;
import net.bluemind.node.client.impl.HostPortClient;
import net.bluemind.node.client.impl.WebsocketLink;
import net.bluemind.node.client.impl.ahc.AHCHelper;
import net.bluemind.node.client.impl.ahc.AHCHttpNodeClient;

public final class AHCNodeClientFactory implements INodeClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(AHCNodeClientFactory.class);

	private static final ConcurrentHashMap<String, HostPortClient> clients = new ConcurrentHashMap<>();

	public AHCNodeClientFactory() {
	}

	@Override
	public INodeClient create(String hostIpAddress) throws ServerFault {
		return new AHCHttpNodeClient(getClient(hostIpAddress));
	}

	@Override
	public void delete(String address) {
		clients.remove(address);
	}

	private synchronized HostPortClient getClient(final String hostIpAddress) {
		HostPortClient cli = clients.get(hostIpAddress);
		if (cli == null) {
			cli = createNew(hostIpAddress);
			clients.put(hostIpAddress, cli);
		} else if (!cli.isSSL() && AHCHelper.mayRebuild()) {
			cli = createNew(hostIpAddress);
			clients.put(hostIpAddress, cli);
		}
		return cli;
	}

	private HostPortClient createNew(final String hostIpAddress) {
		AsyncHttpClient ahc = AHCHelper.get();
		if (!AHCHelper.hasClientCertificate()) {
			ahc = AHCHelper.rebuild();
		}
		HostPortClient hpc = new HostPortClient();
		hpc.setHost(hostIpAddress);
		hpc.setPort(8021);
		hpc.setClient(ahc);
		if (AHCHelper.hasClientCertificate()) {
			try {
				Response resp = ahc.prepareOptions("https://" + hostIpAddress + ":8022/").execute().get();
				logger.info("SSL test: {}", resp.getStatusCode());
				if (resp.getStatusCode() == 200) {
					hpc.setPort(8022);
				}
			} catch (Exception ioe) {
				logger.info("Error testing SSL connection to " + hostIpAddress + ": " + ioe);
			}
		}
		WebsocketLink ws = new WebsocketLink(hpc);
		logger.info("[SSL: {}] Client configured for {}, ws: {}", hpc.isSSL(), hostIpAddress, ws);
		ws.waitAvailable(10, TimeUnit.SECONDS);
		return hpc;
	}

	@Override
	public int getPriority() {
		return 1;
	}

}
