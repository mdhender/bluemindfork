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
package net.bluemind.eas.command.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.settings.SettingsRequest;
import net.bluemind.eas.dto.settings.SettingsResponse;
import net.bluemind.eas.dto.settings.SettingsResponse.DeviceInformation;
import net.bluemind.eas.dto.settings.SettingsResponse.Oof;
import net.bluemind.eas.dto.settings.SettingsResponse.UserInformation;
import net.bluemind.eas.dto.settings.SettingsStatus;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.settings.SettingsRequestParser;
import net.bluemind.eas.serdes.settings.SettingsResponseFormatter;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class SettingsProtocol implements IEasProtocol<SettingsRequest, SettingsResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SettingsProtocol.class);

	public SettingsProtocol() {
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<SettingsRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		SettingsRequestParser parser = new SettingsRequestParser();
		SettingsRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, SettingsRequest sr, Handler<SettingsResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		SettingsResponse response = new SettingsResponse();
		response.status = SettingsStatus.Ok;
		if (sr.userInformation != null && sr.userInformation.get != null) {
			UserInformation ui = new SettingsResponse.UserInformation();
			response.userInformation = ui;
			ui.smtpAddresses = bs.getUser().getEmails();
			ui.primaryAddress = bs.getUser().getDefaultEmail();
		}

		if (sr.deviceInformation != null && sr.deviceInformation.set != null) {
			DeviceInformation di = new SettingsResponse.DeviceInformation();
			response.deviceInformation = di;
		}

		if (sr.oof != null && sr.oof.get != null) {
			Oof oof = new SettingsResponse.Oof();
			response.oof = oof;
		}

		responseHandler.handle(response);
	}

	@Override
	public void write(BackendSession bs, Responder responder, SettingsResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}

		SettingsResponseFormatter formatter = new SettingsResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		formatter.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

			@Override
			public void onResult(Void data) {
				completion.handle(null);
			}
		});
	}

	@Override
	public String address() {
		return "eas.protocol.settings";
	}

}
