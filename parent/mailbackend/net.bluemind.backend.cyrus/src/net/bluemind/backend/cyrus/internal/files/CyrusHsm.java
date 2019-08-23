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
package net.bluemind.backend.cyrus.internal.files;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.server.api.IServer;

/**
 * /etc/cyrus-hsm handler
 * 
 * 
 */
public class CyrusHsm extends AbstractConfFile {

	private INodeClient node;

	public CyrusHsm(INodeClient nc) throws ServerFault {
		super(null, null);
		this.node = nc;
	}

	public CyrusHsm(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	@Override
	public void write() throws ServerFault {
		Template cyrusConf = openTemplate("backend.hsm.conf");
		Map<String, Object> data = new HashMap<String, Object>();

		HsmConfig config = getHsmConfig();

		data.put("archiveEnabled", config.enabled);
		data.put("archiveDays", config.archiveDays);
		data.put("archiveMaxSize", config.maxSize);

		byte[] rendered = render(cyrusConf, data);
		if (node != null) {
			NCUtils.execNoOut(node, "mkdir /dummy-sds");
			NCUtils.execNoOut(node, "chown -R cyrus:mail /dummy-sds");
			node.writeFile("/etc/cyrus-hsm", new ByteArrayInputStream(rendered));
		} else {
			service.submitAndWait(serverUid, "mkdir /dummy-sds");
			service.submitAndWait(serverUid, "chown -R cyrus:mail /dummy-sds");
			service.writeFile(serverUid, "/etc/cyrus-hsm", rendered);
		}
	}

}
