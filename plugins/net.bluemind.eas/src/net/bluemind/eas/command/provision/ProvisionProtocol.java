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

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.provision.ProvisionRequest;
import net.bluemind.eas.dto.provision.ProvisionResponse;
import net.bluemind.eas.dto.provision.ProvisionResponse.Policies.Policy.EASProvisionDoc;
import net.bluemind.eas.dto.provision.ProvisionResponse.Policies.Policy.Status;
import net.bluemind.eas.dto.settings.SettingsResponse;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.Responder.ConnectionHeader;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.provision.ProvisionRequestParser;
import net.bluemind.eas.serdes.provision.ProvisionResponseFormatter;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class ProvisionProtocol implements IEasProtocol<ProvisionRequest, ProvisionResponse> {

	private static final Logger logger = LoggerFactory.getLogger(ProvisionProtocol.class);

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<ProvisionRequest> parserResultHandler) {
		ProvisionRequestParser parser = new ProvisionRequestParser();
		ProvisionRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, ProvisionRequest req, Handler<ProvisionResponse> responseHandler) {
		ProvisionResponse response = new ProvisionResponse();

		response.status = ProvisionResponse.Status.Success;

		if (req.deviceInformation != null && req.deviceInformation.set != null) {
			response.deviceInformation = new SettingsResponse.DeviceInformation();
		}

		if (req.policies != null) {
			response.policies = new ProvisionResponse.Policies();
			response.policies.policy.policyType = req.policies.policy.policyType;
			response.policies.policy.status = Status.Success;

			String policyKey = req.policies.policy.policyKey;
			if ("0".equals(policyKey) || isUnknown(policyKey)) {
				logger.info("Client downloads policy from server, send temporary policy key: {}",
						Policies.TEMPORARY_POLICY_KEY);
				response.policies.policy.policyKey = Policies.TEMPORARY_POLICY_KEY;
				response.policies.policy.data = new EASProvisionDoc();
			} else {
				if (Policies.TEMPORARY_POLICY_KEY.equals(policyKey)) {
					// 4.1.3 Phase 3
					logger.info(
							"Client acknowledges receipt and application of policy settings, send the final policy key: {}",
							Policies.FINAL_POLICY_KEY);
					response.policies.policy.policyKey = Policies.FINAL_POLICY_KEY;
				} else if (Policies.FINAL_POLICY_KEY.equals(policyKey)) {
					response.policies.policy.policyKey = Policies.TEMPORARY_POLICY_KEY;
					response.policies.policy.data = new EASProvisionDoc();
				}
			}
		}

		if (req.remoteWipe != null) {
			Backends.dataAccess().acknowledgeRemoteWipe(bs);
			response.remoteWipe = new ProvisionResponse.RemoteWipe();
			responseHandler.handle(null);
			return;
		}

		responseHandler.handle(response);

	}

	private boolean isUnknown(String policyKey) {
		return !(Policies.TEMPORARY_POLICY_KEY.equals(policyKey) || Policies.FINAL_POLICY_KEY.equals(policyKey));
	}

	@Override
	public void write(BackendSession bs, Responder responder, ProvisionResponse response,
			final Handler<Void> completion) {
		if (response == null) {
			responder.sendStatus(200);
			completion.handle(null);
			return;
		}
		ProvisionResponseFormatter formatter = new ProvisionResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(),
				responder.asOutput(ConnectionHeader.close));
		formatter.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

			@Override
			public void onResult(Void data) {
				completion.handle(null);
			}
		});
	}

	@Override
	public String address() {
		return "eas.protocol.provision";
	}

}
