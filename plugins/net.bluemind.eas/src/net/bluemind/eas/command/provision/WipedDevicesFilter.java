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
package net.bluemind.eas.command.provision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.http.AuthenticatedEASQuery;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestFilter;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;
import net.bluemind.eas.provisioning.MSEASProvisioningWBXML;
import net.bluemind.eas.provisioning.Policy;
import net.bluemind.eas.utils.DOMUtils;

/**
 * Ensures we continue to send wipe commands to wiped devices
 *
 */
public final class WipedDevicesFilter implements IEasRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(WipedDevicesFilter.class);

	@Override
	public int priority() {
		return 1;
	}

	@Override
	public void filter(AuthenticatedEASQuery query, FilterChain next) {
		if (WipedDevices.isWiped(query)) {
			logger.info("[{}] Wiped device {}", query.loginAtDomain(), query.deviceIdentifier());
			Responder responder = new VertxResponder(query.request(), query.request().response());
			if ("Provision".equals(query.command())) {
				Policy policy = new MSEASProvisioningWBXML(query.protocolVersion());
				sendRemoteWipeRequest(responder, policy);
			} else if (query.protocolVersion() < 14) {
				responder.sendStatus(449);
			} else {
				ProvisionHelper.forceWipeProto14(query.command(), responder);
			}
		} else {
			logger.debug("Not wiped.");
			next.filter(query);
		}

	}

	@Override
	public void filter(AuthorizedDeviceQuery query, FilterChain next) {
		next.filter(query);
	}

	/**
	 * @param responder
	 * @param policy
	 */
	public void sendRemoteWipeRequest(Responder responder, Policy policy) {
		try {
			Document ret = DOMUtils.createDoc("Provision", "Provision");
			Element root = ret.getDocumentElement();
			DOMUtils.createElementAndText(root, "Status", "1");

			// Policies stuff makes Android < 4.4 happy ...
			Element policies = DOMUtils.createElement(root, "Policies");
			Element pol = DOMUtils.createElement(policies, "Policy");
			DOMUtils.createElementAndText(pol, "PolicyType", "MS-EAS-Provisioning-WBXML");
			DOMUtils.createElementAndText(pol, "Status", "1");
			DOMUtils.createElementAndText(pol, "PolicyKey", "0");
			Element data = DOMUtils.createElement(pol, "Data");
			policy.serialize(data);

			DOMUtils.createElement(root, "RemoteWipe");

			responder.sendResponse(NamespaceMapping.Provision, ret);
		} catch (Exception e) {
			logger.error("Error creating provision RemoteWipe", e);
		}
	}

}
