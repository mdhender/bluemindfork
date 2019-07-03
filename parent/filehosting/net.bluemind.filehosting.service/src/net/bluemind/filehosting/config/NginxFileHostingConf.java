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
package net.bluemind.filehosting.config;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.system.config.AbstractConfFile;

public class NginxFileHostingConf extends AbstractConfFile {

	private static final int MEMORY_SIZE = 64;
	private long messageSizeLimit;
	private static String path = "/etc/bm-webmail/bm-filehosting.conf";

	public NginxFileHostingConf(INodeClient nc) throws ServerFault {
		super(nc);
	}

	@Override
	public void write() throws ServerFault {
		Template mcf = openTemplate(getClass(), "webmailFilehosting.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("messageSizeLimit", "" + messageSizeLimit);
		data.put("memorySize", "" + (MEMORY_SIZE + 8 * messageSizeLimit));
		nc.writeFile(path, render(mcf, data));
	}

	public void setMessageSizeLimit(long messageSizeLimit) {
		this.messageSizeLimit = messageSizeLimit;
	}

	@Override
	public void clear() {
	}
}
