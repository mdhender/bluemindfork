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
package net.bluemind.system.service.certificate;

import java.util.Optional;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.ICertificateSecurityMgmt;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.service.certificate.engine.CertifEngineFactory;
import net.bluemind.system.service.certificate.engine.ICertifEngine;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;
import net.bluemind.system.service.helper.SecurityCertificateHelper;

public class CertificateSecurityMgmt implements ICertificateSecurityMgmt {

	private BmContext context;
	private RBACManager rbac;
	private SecurityCertificateHelper systemHelper;

	public CertificateSecurityMgmt(BmContext context) {
		this.context = context;
		rbac = new RBACManager(context);
		systemHelper = new SecurityCertificateHelper(context);
	}

	@Override
	public State renewLetsEncryptCertificate(String domainUid, String externalUrl, String contactEmail) {
		rbac.check(BasicRoles.ROLE_MANAGE_CERTIFICATE);

		if (Strings.isNullOrEmpty(domainUid)) {
			throw new ServerFault("Domain UID is mandatory");
		}

		if (!Strings.isNullOrEmpty(externalUrl) && !externalUrl.equals(systemHelper.getExternalUrl(domainUid))) {
			throw new ServerFault(
					String.format("Given external URL '%s' does not match to domain '%s'", externalUrl, domainUid));
		}

		if (Strings.isNullOrEmpty(externalUrl)) {
			externalUrl = systemHelper.getExternalUrl(domainUid);
		}

		Optional<ICertifEngine> optional = CertifEngineFactory.get(domainUid);
		if (optional.isPresent()) {
			optional.get().authorizeLetsEncrypt();
			optional.get().getCertData().email = Strings.isNullOrEmpty(contactEmail)
					? LetsEncryptCertificate.getContactProperty(optional.get().getDomain().value)
					: contactEmail;
			TaskRef tr = context.su().provider().instance(ISecurityMgmt.class)
					.generateLetsEncrypt(optional.get().getCertData());
			return TaskUtils.wait(context.su().provider(), tr).state;
		}

		return TaskStatus.State.InError;
	}

}
