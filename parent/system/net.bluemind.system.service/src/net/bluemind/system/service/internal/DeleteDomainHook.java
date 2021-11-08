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
package net.bluemind.system.service.internal;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class DeleteDomainHook extends DomainHookAdapter {

	private Logger logger = LoggerFactory.getLogger(DeleteDomainHook.class);

	@Override
	public void onDomainItemsDeleted(BmContext context, ItemValue<Domain> domain) throws ServerFault {
		deleteCertFiles(context, domain.uid);
	}

	private void deleteCertFiles(BmContext context, String domainUid) {
		logger.info("Deleting all certificates of domain {}", domainUid);
		try {
			IServer serverService = context.provider().instance(IServer.class, InstallationId.getIdentifier());
			String bmCertFileName = "bm_cert-" + domainUid + ".pem";
			List<String> files = Arrays.asList("/etc/bm/certs/" + bmCertFileName, "/etc/ssl/certs/" + bmCertFileName);
			for (ItemValue<Server> serverItem : serverService.allComplete()) {
				INodeClient nc = new AHCNodeClientFactory().create(serverItem.value.address());
				for (String certFilePath : files) {
					nc.deleteFile(certFilePath);
				}
			}
		} catch (Exception e) {
			logger.warn("Error occurs trying to deleteCertFiles", e.getMessage());
		}
	}

}
