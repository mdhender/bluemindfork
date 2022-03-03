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
import net.bluemind.server.api.IServer;

/**
 * /etc/cyrus-annotations handler
 * 
 * 
 */
public class Annotations extends AbstractConfFile {

	private INodeClient node;

	public Annotations(INodeClient nc) throws ServerFault {
		super(null, null);
		this.node = nc;
	}

	public Annotations(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	@Override
	public void write() throws ServerFault {
		Template cyrusConf = openTemplate("backend.annotations.conf");
		Map<String, Object> data = new HashMap<>();
		byte[] rendered = render(cyrusConf, data);
		if (node != null) {
			node.writeFile("/etc/cyrus-annotations", new ByteArrayInputStream(rendered));
		} else {
			service.writeFile(serverUid, "/etc/cyrus-annotations", rendered);
		}
	}

}
