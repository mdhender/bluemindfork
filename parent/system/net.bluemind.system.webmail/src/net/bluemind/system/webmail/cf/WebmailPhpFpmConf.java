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
package net.bluemind.system.webmail.cf;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.api.INodeClient;
import net.bluemind.system.nginx.AbstractConfFile;
import net.bluemind.system.webmail.Path;

public class WebmailPhpFpmConf extends AbstractConfFile {

	private static final int MEMORY_SIZE = 64;
	private long messageSizeLimit;
	private static String path = Path.WEBMAIL_PATH + "/" + Path.WEBMAIL_PHP_FPM_CF;

	public WebmailPhpFpmConf(INodeClient nc) throws ServerFault {
		super(nc);
	}

	@Override
	public void write() throws ServerFault {
		Template mcf = openTemplate(getClass(), "webmailPhpFpm.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("messageSizeLimit", "" + messageSizeLimit);
		data.put("memorySize", "" + (MEMORY_SIZE + 8 * messageSizeLimit));
		nc.writeFile(path, render(mcf, data));
	}

	public void setMessageSizeLimit(long l) {
		this.messageSizeLimit = l;
	}

	@Override
	public void clear() {
	}
}
