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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.system.service.certificate.engine;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.hook.ISystemHook;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;
import net.bluemind.system.service.helper.SecurityCertificateHelper;

public class DisabledCertifEngine extends CertifEngine {

	public DisabledCertifEngine(String domainUid) {
		super(domainUid);
		certData = createDomainCertData(CertificateDomainEngine.DISABLED);
	}

	public DisabledCertifEngine(CertData certData, BmContext context) {
		super(certData, context);
	}

	@Override
	public void externalUrlUpdated(boolean removed) {
		if (removed) {
			systemHelper.getSuProvider().instance(ISecurityMgmt.class)
					.updateCertificate(CertData.createForDisable(domainUid));
		}
	}

	@Override
	public boolean authorizeUpdate() {
		if (SecurityCertificateHelper.isGlobalVirtDomain(certData.domainUid)) {
			throw new ServerFault("Cannot disable 'global.virt' domain Certificate");
		}
		return true;
	}

	@Override
	public void authorizeLetsEncrypt() {
		throw new ServerFault("SSL certif engine must be 'LETS_ENCRYPT'");
	}

	@Override
	public void doBeforeUpdate() {
		new LetsEncryptCertificate(systemHelper).cleanLetsEncryptProperties(domainUid);
	}

	@Override
	public ItemValue<Domain> getDomain() {
		return domain;
	}

	@Override
	public CertData getCertData() {
		return certData;
	}

	@Override
	public void certificateMgmt(List<ItemValue<Server>> servers, List<ISystemHook> hooks) {
		logger.info("disable certificate by {}", systemHelper.getContext().getSecurityContext().getSubject());
		boolean fireUpdate = false;
		for (ItemValue<Server> serverItem : servers) {
			INodeClient nc = new AHCNodeClientFactory().create(serverItem.value.address());
			for (String certFilePath : getCertificateFilePaths()) {
				if (nc.listFiles(certFilePath).size() > 0) {
					fireUpdate = true;
					nc.deleteFile(certFilePath);
				}
			}
		}
		updateDomainCertifEngine();
		if (fireUpdate) {
			fireCertificateUpdated(hooks);
		}
	}
}
