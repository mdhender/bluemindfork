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
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.IServer;

public class SmtpdConf extends AbstractConfFile {
	private String pwCheckMethod = "saslauthd";
	private String mechList = "PLAIN LOGIN";

	public SmtpdConf(IServer service, String serverUid) throws ServerFault {
		super(service, serverUid);
	}

	@Override
	public void write() throws ServerFault {
		Template mcf = openTemplate("sasl-smtpd.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("pwCheckMethod", pwCheckMethod);
		data.put("mechList", mechList);
		service.writeFile(serverUid, PostfixPaths.SASL_SMTPD_CONF, render(mcf, data));
	}

}
