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
package net.bluemind.backend.postfix.internal.cf;

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.backend.postfix.internal.PostfixPaths;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.IServer;

public class MainCf extends AbstractConfFile {

	private String hostname;
	private String networks;
	private String messageSizeLimit;

	public MainCf(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	@Override
	public void write() throws ServerFault {
		ParametersValidator.notNullAndNotEmpty(hostname);

		Template cyrusConf = openTemplate("main.cf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("myHostname", hostname);
		data.put("myNetworks", networks);
		data.put("messageSizeLimit", messageSizeLimit);
		service.writeFile(serverUid, PostfixPaths.MAIN_CF, render(cyrusConf, data));
	}

	public void setMyNetworks(String networks) {
		this.networks = networks;
	}

	public void setMessageSizeLimit(String msl) {
		this.messageSizeLimit = msl;
	}
}
