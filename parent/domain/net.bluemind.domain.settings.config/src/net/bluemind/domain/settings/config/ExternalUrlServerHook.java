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
package net.bluemind.domain.settings.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class ExternalUrlServerHook extends DefaultServerHook {
	@Override
	public void onServerCreated(BmContext context, ItemValue<Server> server) throws ServerFault {
		File file = new File(DomainSettingsConfigFileUpdate.BM_EXTERNAL_URL_FILEPATH);
		if (!file.exists()) {
			return;
		}

		INodeClient remote = NodeActivator.get(server.value.address());
		if (NCUtils.connectedToMyself(remote)) {
			return;
		}

		copyToRemote(server, remote, file.toPath());
	}

	private void copyToRemote(ItemValue<Server> server, INodeClient remote, Path file) {
		try {
			remote.writeFile(file.toFile().getAbsolutePath(), new ByteArrayInputStream(Files.readAllBytes(file)));
		} catch (IOException e) {
			throw new ServerFault(String.format("Fail to copy %s to server %s", file.toFile().getAbsolutePath(),
					server.value.address()), e);
		}
	}
}
