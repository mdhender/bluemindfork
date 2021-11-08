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
import net.bluemind.server.api.Server;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.hook.ISystemHook;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;

public class LetsEncryptCertifEngine extends CertifEngine {

	public LetsEncryptCertifEngine(String domainUid) {
		super(domainUid);
		certData = createDomainCertData(CertificateDomainEngine.LETS_ENCRYPT);
	}

	public LetsEncryptCertifEngine(CertData certData, BmContext context) {
		super(certData, context);
	}

	@Override
	public void externalUrlUpdated(boolean removed) {
		if (isApproved()) {
			if (removed) {
				systemHelper.getSuProvider().instance(ISecurityMgmt.class)
						.updateCertificate(CertData.createForDisable(domainUid));
			} else {
				systemHelper.getSuProvider().instance(ISecurityMgmt.class)
						.generateLetsEncrypt(CertData.createForLetsEncrypt(domainUid, null));
			}
		}
	}

	private boolean isApproved() {
		return LetsEncryptCertificate.isTosApproved(domain.value);
	}

	@Override
	public boolean authorizeUpdate() {
		return false;
	}

	@Override
	public void authorizeLetsEncrypt() {
		if (!isApproved()) {
			throw new ServerFault("Let's Encrypt terms of service must been approved to continue");
		}
	}

	@Override
	public void doBeforeUpdate() {
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
		checkCertificateAndWriteFile(servers, hooks);
	}

}
