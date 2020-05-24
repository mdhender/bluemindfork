/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.dataprotect.service.tool;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;

public class RemoveForgottenParts extends AbstractConfFile {
	private static final Logger logger = LoggerFactory.getLogger(RemoveForgottenParts.class);

	private static final String RFP_PATH = "/tmp/removeForgottenParts.sh";
	private Collection<Integer> validPartsId;

	public RemoveForgottenParts(INodeClient nc, Collection<Integer> validPartsId) throws ServerFault {
		super(nc);
		this.validPartsId = validPartsId;
	}

	@Override
	public void write() throws ServerFault {
		Template mcf = openTemplate(getClass(), "removeForgottenParts.sh");
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("validPartsIds", validPartsId);
		data.put("backupRoot", "/var/backups/bluemind/dp_spool/rsync");

		InputStream rfpScript = render(mcf, data);
		nc.writeFile(RFP_PATH, rfpScript);

		TaskRef tr = nc.executeCommandNoOut("chmod +x " + RFP_PATH);
		NCUtils.waitFor(nc, tr);

		tr = nc.executeCommand(RFP_PATH);
		ExitList results = NCUtils.waitFor(nc, tr);
		for (String result : results) {
			logger.info(result);
		}

		tr = nc.executeCommandNoOut("rm -f " + RFP_PATH);
		NCUtils.waitFor(nc, tr);
	}

	@Override
	public void clear() {
	}

	public void execute() {
		write();
	}
}
