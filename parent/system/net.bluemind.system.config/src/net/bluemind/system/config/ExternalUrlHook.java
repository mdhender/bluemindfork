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
package net.bluemind.system.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.Regex;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;
import net.bluemind.system.nginx.NginxService;

public class ExternalUrlHook
		implements ISystemConfigurationObserver, ISystemConfigurationSanitizor, ISystemConfigurationValidator {
	private static final String BMINI = "/etc/bm/bm.ini";

	private static Logger logger = LoggerFactory.getLogger(ExternalUrlHook.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String externalUrl = conf.stringValue(SysConfKeys.external_url.name());
		if ((Strings.isNullOrEmpty(externalUrl)
				&& Strings.isNullOrEmpty(previous.values.get(SysConfKeys.external_url.name())))
				|| Strings.nullToEmpty(externalUrl)
						.equals(Strings.nullToEmpty(previous.values.get(SysConfKeys.external_url.name())))) {
			return;
		}

		logger.info("System configuration {} has been updated, changed to {}", SysConfKeys.external_url.name(),
				externalUrl);
		new NginxService().updateExternalUrl(externalUrl);

		updateBmIni(externalUrl);
	}

	private void updateBmIni(String externalUrl) {
		Ini iniFile;
		try (InputStream in = Files.newInputStream(new File(BMINI).toPath())) {
			iniFile = new Ini(in);
		} catch (IOException e) {
			logger.error("Unable to read {}", BMINI, e);
			throw new ServerFault(e);
		}

		iniFile.get("global").put("external-url", externalUrl);

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

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.external_url.name())
				&& previous.values.containsKey("external-url")) {
			modifications.put(SysConfKeys.external_url.name(), previous.stringValue("external-url"));
		}
		// external-url key is forbidden in database
		modifications.put("external-url", null);

		if (!modifications.containsKey(SysConfKeys.external_url.name())
				|| modifications.get(SysConfKeys.external_url.name()) == null) {
			return;
		}

		modifications.put(SysConfKeys.external_url.name(), modifications.get(SysConfKeys.external_url.name()).trim());
	}

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		if (!modifications.containsKey(SysConfKeys.external_url.name())) {
			return;
		}

		if (Strings.isNullOrEmpty(modifications.get(SysConfKeys.external_url.name()))) {
			throw new ServerFault("External URL must not be null or empty!", ErrorCode.INVALID_PARAMETER);
		}

		if (!Regex.DOMAIN.validate(modifications.get(SysConfKeys.external_url.name()))) {
			throw new ServerFault(
					String.format("Invalid external URL '%s'", modifications.get(SysConfKeys.external_url.name())),
					ErrorCode.INVALID_PARAMETER);
		}
	}
}
