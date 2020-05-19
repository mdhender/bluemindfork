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
package net.bluemind.system.auth;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class CasAuthConfigChangeHandler implements ISystemConfigurationObserver {

	private enum Status {
		Install, Remove, None
	}

	private static final Logger logger = LoggerFactory.getLogger(CasAuthConfigChangeHandler.class);

	private BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
	private final HpsHelper hpsHelper = HpsHelper.get();

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		switch (casAuthHasChanged(previous, conf)) {
		case Install:
			logger.info("Authentication has changed, installing CAS conf files");
			installCas(conf);
			break;
		case Remove:
			logger.info("Authentication has changed, Removing CAS conf files");
			removeCas();
		default:
			break;
		}

	}

	private void removeCas() throws ServerFault {
		List<ItemValue<Server>> servers = hpsHelper.hpsNodes(context);
		for (ItemValue<Server> server : servers) {
			removeCasParameters(server.value);
		}
	}

	private void installCas(SystemConf conf) throws ServerFault {

		List<ItemValue<Server>> servers = hpsHelper.hpsNodes(context);
		for (ItemValue<Server> server : servers) {
			String casUrl = conf.values.get(SysConfKeys.cas_url.name());
			String casDomain = conf.values.get(SysConfKeys.cas_domain.name());

			updateCasParameters(server.value, casUrl, casDomain);
		}
	}

	private Status casAuthHasChanged(SystemConf previous, SystemConf conf) {
		Map<String, String> previousValues = previous.values;
		Map<String, String> currentValues = conf.values;

		if (currentValues.get(SysConfKeys.auth_type.name()) != null
				&& currentValues.get(SysConfKeys.auth_type.name()).equals("CAS")) {
			if (!"CAS".equals(previousValues.get(SysConfKeys.auth_type.name()))) {
				return Status.Install;
			}

			if (isDifferent(previousValues, currentValues, SysConfKeys.cas_domain)) {
				return Status.Install;
			}
			if (isDifferent(previousValues, currentValues, SysConfKeys.cas_url)) {
				return Status.Install;
			}
		}

		if ("CAS".equals(previousValues.get(SysConfKeys.auth_type.name()))
				&& !"CAS".equals(currentValues.get(SysConfKeys.auth_type.name()))) {
			return Status.Remove;
		}

		return Status.None;
	}

	private boolean isDifferent(Map<String, String> previousValues, Map<String, String> currentValues,
			SysConfKeys authType) {
		String prev = previousValues.get(authType.name());
		String current = currentValues.get(authType.name());
		return !prev.equals(current);
	}

	private void updateCasParameters(Server server, String url, String domain) throws ServerFault {
		// Read bm.ini file
		String bmIni = hpsHelper.nodeRead(server, "/etc/bm/bm.ini");

		// Update CAS parameters inside it
		String out = "";
		boolean modified = false;

		for (String line : bmIni.split("\n")) {
			if (line.startsWith("casUrl")) {
				String oldCasUrl = line.replaceAll("^.*?=", "").trim();
				out += line.replace(oldCasUrl, url) + "\n";
				modified = true;
			} else if (line.startsWith("casDomain")) {
				String oldCasDomain = line.replaceAll("^.*?=", "").trim();
				out += line.replace(oldCasDomain, domain) + "\n";
			} else {
				out += line + "\n";
			}
		}

		if (!modified) {
			out += "casUrl = " + url + "\n";
			out += "casDomain = " + domain + "\n";
		}

		// Write back bm.ini file
		hpsHelper.nodeWrite(server, "/etc/bm/bm.ini", out);
		// restart hps
		hpsHelper.restartHps(server);
	}

	private void removeCasParameters(Server server) throws ServerFault {
		// Read bm.ini
		String bmIni = hpsHelper.nodeRead(server, "/etc/bm/bm.ini");

		// Remove CAS configuration
		String out = "";
		for (String line : bmIni.split("\n")) {
			if (!line.startsWith("casUrl") && !line.startsWith("casDomain")) {
				out += line + "\n";
			}
		}

		// Write back bm.ini
		hpsHelper.nodeWrite(server, "/etc/bm/bm.ini", out);
		hpsHelper.restartHps(server);
	}

}
