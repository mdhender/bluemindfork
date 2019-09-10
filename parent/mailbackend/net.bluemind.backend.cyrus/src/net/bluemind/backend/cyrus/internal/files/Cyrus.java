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

import java.util.HashMap;
import java.util.Map;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.server.api.IServer;

/**
 * /etc/cyrus.conf handler
 * 
 * 
 */
public class Cyrus extends AbstractConfFile {

	public static final int DEFAULT_MAX_CHILD = 200;
	public static final int DEFAULT_RETENTION = 7;
	private final int imapMaxChild;
	private final int retention;

	public Cyrus(IServer service, String serverUid, int imapMaxChild, int retention) throws ServerFault {
		super(service, serverUid);
		this.imapMaxChild = imapMaxChild;
		this.retention = retention;
	}

	@Override
	public void write() throws ServerFault {
		Template cyrusConf = openTemplate("backend.cyrus.conf");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("imapMaxChild", Integer.toString(imapMaxChild));
		data.put("retention", retention);

		HsmConfig config = getHsmConfig();

		boolean archiveEnabled = config.cyrusConf.get("archive_enabled").equals("1");
		if (archiveEnabled) {
			data.put("archiveEnabled", archiveEnabled);
			data.put("archiveDays", config.cyrusConf.get("archive_days"));
		}

		service.writeFile(serverUid, "/etc/cyrus.conf", render(cyrusConf, data));

	}

}
