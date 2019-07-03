/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.auth;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class DefaultDomainConfigurationObserver extends HpsHelper implements ISystemConfigurationObserver {

	public enum Action {
		UPDATE, REMOVE, NOTHING
	}

	private static Logger logger = LoggerFactory.getLogger(DefaultDomainConfigurationObserver.class);
	private final String key = "default-domain";

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {

		Action action = process(previous, conf);
		if (action == Action.NOTHING) {
			logger.debug("Nothing to do");
			return;
		}
		List<ItemValue<Server>> servers = hpsNodes(context);

		switch (action) {
		case REMOVE:
			for (ItemValue<Server> server : servers) {
				remove(server.value);
			}
			break;
		case UPDATE:
			for (ItemValue<Server> server : servers) {
				update(server.value, conf.stringValue(key));
			}
			break;
		default:
		}
	}

	public Action process(SystemConf previous, SystemConf conf) {
		if ((previous.stringValue(key) != null && !previous.stringValue(key).equals(conf.stringValue(key)))
				|| (previous.stringValue(key) == null && conf.stringValue(key) != null)) {
			String val = conf.stringValue(key);

			if (val == null || val.isEmpty()) {
				return Action.REMOVE;
			}

			return Action.UPDATE;
		}

		return Action.NOTHING;
	}

	private void update(Server server, String newValue) {
		String bmIni = nodeRead(server, "/etc/bm/bm.ini");

		String out = "";
		boolean updated = false;
		for (String s : bmIni.split("\n")) {
			if (s.startsWith(key)) {
				String oldValue = s.replaceAll("^.*?=", "").trim();
				out += s.replace(oldValue, newValue) + "\n";
				updated = true;
			} else {
				out += s + "\n";
			}
		}

		if (!updated) {
			out += key + " = " + newValue + "\n";
		}

		nodeWrite(server, "/etc/bm/bm.ini", out);
		reloadHps("default-domain");

	}

	private void remove(Server server) throws ServerFault {
		String bmIni = nodeRead(server, "/etc/bm/bm.ini");

		String out = "";
		for (String s : bmIni.split("\n")) {
			if (!s.startsWith(key)) {
				out += s + "\n";
			}
		}

		nodeWrite(server, "/etc/bm/bm.ini", out);
		reloadHps("default-domain");
	}

}
