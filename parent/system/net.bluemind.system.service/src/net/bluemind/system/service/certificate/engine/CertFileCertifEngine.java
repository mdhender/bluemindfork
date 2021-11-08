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

import com.google.common.base.Strings;

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

public class CertFileCertifEngine extends CertifEngine {

	public CertFileCertifEngine(String domainUid) {
		super(domainUid);
		certData = createDomainCertData(CertificateDomainEngine.FILE);
	}

	public CertFileCertifEngine(CertData certData, BmContext context) {
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
		if (Strings.isNullOrEmpty(certData.certificate) && Strings.isNullOrEmpty(certData.certificateAuthority)
				&& Strings.isNullOrEmpty(certData.privateKey)) {
			return false;
		}
		if (Strings.isNullOrEmpty(certData.certificate) || Strings.isNullOrEmpty(certData.certificateAuthority)
				|| Strings.isNullOrEmpty(certData.privateKey)) {
			throw new ServerFault("All files are mandatory");
		}
		return true;
	}

	@Override
	public void authorizeLetsEncrypt() {
		throw new ServerFault("SSL certif engine must be 'LETS_ENCRYPT'");
	}

	@Override
	public void doBeforeUpdate() {
		if (CertificateDomainEngine.LETS_ENCRYPT.name().equals(systemHelper.getSslCertifEngine(domainUid))) {
			new LetsEncryptCertificate(systemHelper).cleanLetsEncryptProperties(domainUid);
		}
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