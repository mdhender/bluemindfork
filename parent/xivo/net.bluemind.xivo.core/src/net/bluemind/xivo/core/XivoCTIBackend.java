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
package net.bluemind.xivo.core;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.cti.api.Status;
import net.bluemind.cti.api.Status.PhoneState;
import net.bluemind.cti.backend.ICTIBackend;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.User;
import net.bluemind.xivo.client.XivoClient;
import net.bluemind.xivo.client.XivoFault;
import net.bluemind.xivo.common.Hosts;
import net.bluemind.xivo.common.PhoneStatus;

public class XivoCTIBackend implements ICTIBackend {

	private static final Logger logger = LoggerFactory.getLogger(XivoCTIBackend.class);

	@Override
	public void forward(String domain, ItemValue<User> caller, String number) throws ServerFault {

		try {
			XivoClient client = getCTIClient();
			client.forward(caller.value.login, domain, number);
		} catch (XivoFault e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	@Override
	public void dnd(String domain, ItemValue<User> caller, boolean dndEnabled) throws ServerFault {

		try {
			XivoClient client = getCTIClient();
			client.setDND(caller.value.login, domain, dndEnabled);
		} catch (XivoFault e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	@Override
	public void dial(String domain, ItemValue<User> caller, String number) throws ServerFault {

		try {
			XivoClient client = getCTIClient();
			client.dial(caller.value.login, domain, number);
		} catch (XivoFault e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}

	}

	@Override
	public List<String> users(String domain, ItemValue<User> caller) throws ServerFault {
		return Collections.emptyList();
	}

	/**
	 * @param login
	 * @return
	 * @throws ServerFault
	 */
	private XivoClient getCTIClient() throws ServerFault {
		XivoClient client = new XivoClient(Hosts.xivo());
		return client;
	}

	@Override
	public Status.PhoneState getPhoneState(String domain, ItemValue<User> caller) throws ServerFault {

		try {
			// OPTIMIZE
			Optional<ItemValue<Server>> optHost = Topology.get().anyIfPresent("cti/frontend");
			if (optHost.isPresent()) {
				String host = optHost.get().value.address();
				String url = "http://" + host + ":9091/xivo/1.0/status/" + domain + "/" + caller.value.login + "/";
				try (InputStream in = new URL(url).openStream()) {
					JsonObject status = new JsonObject(new String(ByteStreams.toByteArray(in), "utf-8"));
					Integer ret = status.getInteger("status");
					if (ret != null) {
						PhoneStatus xivoStatus = PhoneStatus.fromCode(ret);
						return adapt(xivoStatus);
					} else {
						return Status.PhoneState.Unknown;
					}
				}
			} else {
				return Status.PhoneState.Unknown;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return Status.PhoneState.Unknown;
		}
	}

	private PhoneState adapt(PhoneStatus xivoStatus) {
		switch (xivoStatus) {
		case AVAILABLE:
			return PhoneState.Available;
		case BUSY:
		case CALLING:
			return PhoneState.Busy;
		case RINGING:
		case BUSY_AND_RINGING:
			return PhoneState.Ringing;
		case ONHOLD:
			return PhoneState.OnHold;
		case DEACTIVATED:
		case ERROR:
		case UNEXISTING:
		default:
			return PhoneState.Unknown;
		}
	}
}