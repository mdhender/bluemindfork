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
package net.bluemind.dataprotect.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.worker.DefaultWorker;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;

public class ConfigWorker extends DefaultWorker {

	private static final String dir = "/var/backups/bluemind/work/conf";

	@Override
	public boolean supportsTag(String tag) {
		return "bm/conf".equals(tag);
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(dir);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		String script = dataString("/data/protectConfigurationFiles.sh");
		script = script.replace("${dstPath}", dir);

		INodeClient nc = NodeActivator.get(toBackup.value.address());
		NCUtils.exec(nc, "mkdir -p " + dir);
		nc.writeFile(dir + "/protectConfigurationFiles.sh", new ByteArrayInputStream(script.getBytes()));
		NCUtils.exec(nc, "chmod +x " + dir + "/protectConfigurationFiles.sh");
		ctx.info("en", "Protect configuration files starting... ");
		ctx.info("fr", "Démarrage de la sauvegarde des fichiers de configuration... ");

		List<String> protectLogs = NCUtils.exec(nc, dir + "/protectConfigurationFiles.sh");
		for (String s : protectLogs) {
			ctx.info("en", "configurationFilesProtect: " + s);
			ctx.info("fr", "configurationFilesProtect: " + s);
		}
	}

	private String dataString(String path) throws ServerFault {
		try (InputStream backupScriptStream = this.getClass().getResourceAsStream(path)) {
			return new String(ByteStreams.toByteArray(backupScriptStream));
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void dataDirsSaved(IDPContext ctx, String tag, ItemValue<Server> backedUp) throws ServerFault {
	}

	@Override
	public void restore(IDPContext ctx, PartGeneration part, Map<String, Object> params) throws ServerFault {
	}

	@Override
	public String getDataType() {
		return "configuration";
	}

}
