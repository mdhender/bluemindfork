/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
 package net.bluemind.core.nginx.config;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.system.nginx.AbstractConfFile;

public class CoreNginxConf extends AbstractConfFile {

	private long messageSizeLimit;
	private static String path = "/etc/bm-core/bm-core-nginx.conf";

	public CoreNginxConf(INodeClient nc) throws ServerFault {
		super(nc);
	}

	@Override
	public void write() throws ServerFault {
		Template mcf = openTemplate(getClass(), "coreNginx.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("messageSizeLimit", "" + messageSizeLimit);
		nc.writeFile(path, render(mcf, data));
	}

	public void setMessageSizeLimit(long l) {
		this.messageSizeLimit = l;
	}

	@Override
	public void clear() {
	}
}
