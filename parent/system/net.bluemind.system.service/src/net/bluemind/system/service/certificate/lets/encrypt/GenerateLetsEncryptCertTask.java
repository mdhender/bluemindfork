/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.service.certificate.lets.encrypt;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.server.api.Server;
import net.bluemind.system.hook.ISystemHook;
import net.bluemind.utils.FileUtils;

public class GenerateLetsEncryptCertTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(GenerateLetsEncryptCertTask.class);

	private final LetsEncryptCertificate letsEncryptCertificate;
	private final List<ItemValue<Server>> servers;
	private final List<ISystemHook> hooks;

	public GenerateLetsEncryptCertTask(LetsEncryptCertificate letsEncryptCertificate, List<ItemValue<Server>> servers,
			List<ISystemHook> hooks) {
		this.letsEncryptCertificate = letsEncryptCertificate;
		this.servers = servers;
		this.hooks = hooks;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		try {
			letsEncryptCertificate.letsEncrypt(monitor);
			letsEncryptCertificate.getCertifEngine().certificateMgmt(servers, hooks);
			monitor.end(true, "Let's Encrypt Certificate correctly imported", "");
		} catch (LetsEncryptException e) {
			monitor.end(false, e.getMessage(), "");
			logger.error("Let's Encrypt Certificate Generation fails: {}", e.getMessage());
		} finally {
			logger.info("Clear challenge files in " + LetsEncryptCertificate.CHALLENGE_LOCATION);
			FileUtils.cleanDir(new File(LetsEncryptCertificate.CHALLENGE_LOCATION));
		}
	}
}
