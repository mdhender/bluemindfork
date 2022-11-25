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
package net.bluemind.system.iptables.cf;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.system.iptables.IptablesPath;

public class BmIptablesRules extends AbstractConfFile {
	private static final Logger logger = LoggerFactory.getLogger(BmIptablesRules.class);

	private Set<String> bmHostsAddresses;
	private String iptablesScripts = null;

	public BmIptablesRules(Set<String> bmHostsAddresses) throws ServerFault {
		this.bmHostsAddresses = bmHostsAddresses;
	}

	@Override
	public void write(INodeClient nc) throws ServerFault {
		Template mcf = openTemplate("bm-iptables");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("bmHostsAddresses", bmHostsAddresses);

		if (iptablesScripts == null) {
			logger.info("Generate iptables script from template");
			iptablesScripts = render(mcf, data);
		}

		nc.mkdirs(IptablesPath.IPTABLES_PATH);

		nc.writeFile(IptablesPath.IPTABLES_SCRIPT_PATH, new ByteArrayInputStream(iptablesScripts.getBytes()));

		NCUtils.execNoOut(nc, "chmod +x " + IptablesPath.IPTABLES_SCRIPT_PATH);

		List<FileDescription> systemd = nc.listFiles("/run/systemd/system");
		if (!systemd.isEmpty()) {
			NCUtils.execNoOut(nc, "systemctl daemon-reload");
		}

		NCUtils.execNoOut(nc, "service " + IptablesPath.IPTABLES_SCRIPT_NAME + " restart");

		List<FileDescription> rh = nc.listFiles("/etc/redhat-release");
		if (!rh.isEmpty()) {
			NCUtils.execNoOut(nc, "/sbin/chkconfig --add " + IptablesPath.IPTABLES_SCRIPT_NAME);
			NCUtils.execNoOut(nc, "/sbin/chkconfig " + IptablesPath.IPTABLES_SCRIPT_NAME + " on");
		} else {
			NCUtils.execNoOut(nc, "/usr/sbin/update-rc.d " + IptablesPath.IPTABLES_SCRIPT_NAME + " defaults");
			NCUtils.execNoOut(nc, "/usr/sbin/update-rc.d " + IptablesPath.IPTABLES_SCRIPT_NAME + " enable");
		}
	}

	@Override
	public void clear() {
	}
}
