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
package net.bluemind.system.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.nginx.NginxService;

public class UpdateBmIniHook implements ISystemConfigurationObserver {
	private static final String BMINI = "/etc/bm/bm.ini";

	private static Logger logger = LoggerFactory.getLogger(UpdateBmIniHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		if (isNotUpdated(SysConfKeys.external_url, previous, conf)
				&& isNotUpdated(SysConfKeys.other_urls, previous, conf)) {
			return;
		}

		updateBmIni(conf);
		new NginxService().restart();
		logger.info("System configuration has been updated, {} from '{}' to '{}', {} from '{}' to '{}'",
				SysConfKeys.external_url, previous.stringValue(SysConfKeys.external_url.name()),
				conf.stringValue(SysConfKeys.external_url.name()), SysConfKeys.other_urls,
				previous.stringValue(SysConfKeys.other_urls.name()), conf.stringValue(SysConfKeys.other_urls.name()));
	}

	private boolean isNotUpdated(SysConfKeys value, SystemConf previousSettings, SystemConf newSettings) {
		return (Strings.isNullOrEmpty(newSettings.stringValue(value.name()))
				&& Strings.isNullOrEmpty(previousSettings.stringValue(value.name())))
				|| Strings.nullToEmpty(newSettings.stringValue(value.name()))
						.equals(Strings.nullToEmpty(previousSettings.stringValue(value.name())));
	}

	private void updateBmIni(SystemConf conf) {
		String externalUrl = conf.stringValue(SysConfKeys.external_url.name());
		String otherUrls = conf.stringValue(SysConfKeys.other_urls.name());

		Ini iniFile;
		try (InputStream in = Files.newInputStream(new File(BMINI).toPath())) {
			iniFile = new Ini(in);
		} catch (IOException e) {
			logger.error("Unable to read {}", BMINI, e);
			throw new ServerFault(e);
		}

		iniFile.get("global").put("external-url", externalUrl);
		if (!Strings.isNullOrEmpty(otherUrls)) {
			iniFile.get("global").put("other-urls", otherUrls);
		} else {
			iniFile.get("global").remove("other-urls");
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (OutputStream out = Files.newOutputStream(new File(BMINI).toPath())) {
			iniFile.store(baos);
			iniFile.store(out);
		} catch (IOException e) {
			logger.error("Unable to write {}", BMINI, e);
			throw new ServerFault(e);
		}

		byte[] iniFileContent = baos.toByteArray();
		Topology.getIfAvailable()
				.ifPresent(topo -> topo.nodes().forEach(server -> updateBmIni(server, iniFileContent)));
	}

	private void updateBmIni(ItemValue<Server> server, byte[] iniFile) {
		NodeActivator.get(server.value.address()).writeFile(BMINI, new ByteArrayInputStream(iniFile));
	}
}
