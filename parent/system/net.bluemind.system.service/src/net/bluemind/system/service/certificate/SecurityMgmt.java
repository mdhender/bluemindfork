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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.domain.api.Domain;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.hook.ISystemHook;
import net.bluemind.system.iptables.UpdateFirewallRulesTask;
import net.bluemind.system.service.certificate.engine.CertifEngineFactory;
import net.bluemind.system.service.certificate.engine.ICertifEngine;
import net.bluemind.system.service.certificate.lets.encrypt.GenerateLetsEncryptCertTask;
import net.bluemind.system.service.certificate.lets.encrypt.LetsEncryptCertificate;
import net.bluemind.system.service.helper.SecurityCertificateHelper;

public class SecurityMgmt implements ISecurityMgmt, IInCoreSecurityMgmt {
	private static final Logger logger = LoggerFactory.getLogger(SecurityMgmt.class);
	private BmContext context;
	private List<ISystemHook> hooks;
	private RBACManager rbac;
	private SecurityCertificateHelper systemHelper;

	public SecurityMgmt(BmContext context, List<ISystemHook> hooks) {
		this.context = context;
		this.hooks = hooks;
		rbac = new RBACManager(context);
		systemHelper = new SecurityCertificateHelper(context);
	}

	@Override
	public TaskRef updateFirewallRules() {
		rbac.check(BasicRoles.ROLE_MANAGE_SYSTEM_CONF);
		return context.provider().instance(ITasksManager.class).run(new UpdateFirewallRulesTask());
	}

	@Override
	public void updateCertificate(CertData certData) {
		rbac.check(BasicRoles.ROLE_MANAGE_CERTIFICATE);
		ICertifEngine certifEngine = CertifEngineFactory.get(certData, context);
		logger.info(
				"update certificate with " + certifEngine.getClass().getName() + " - " + certData.sslCertificateEngine);
		if (!certifEngine.authorizeUpdate()) {
			return;
		}
		certifEngine.doBeforeUpdate();
		certifEngine.certificateMgmt(getServers(), hooks);
	}

	@Override
	public TaskRef generateLetsEncrypt(CertData certData) throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGE_CERTIFICATE);
		ICertifEngine certifEngine = CertifEngineFactory.get(certData, context);
		certifEngine.authorizeLetsEncrypt();

		logger.info("generate let's encrypt certificate by {}", context.getSecurityContext().getSubject());

		LetsEncryptCertificate letsEncryptCertificate = new LetsEncryptCertificate(certifEngine, context);
		String tuid = String.format("generateLetsEncrypt-%s", certData.domainUid);
		TaskRef tr = context.provider().instance(ITasksManager.class).run(tuid,
				new GenerateLetsEncryptCertTask(letsEncryptCertificate, getServers(), hooks));
		return tr;
	}

	@Override
	public String getLetsEncryptTos() throws ServerFault {
		return new LetsEncryptCertificate(context).getTermsOfService();
	}

	@Override
	public void approveLetsEncryptTos(String domainUid) throws ServerFault {
		rbac.check(BasicRoles.ROLE_MANAGE_CERTIFICATE);
		new LetsEncryptCertificate(context).approveTermsOfService(domainUid);
	}

	@Override
	public Map<String, ItemValue<Domain>> getLetsEncryptDomainExternalUrls() {
		Map<String, ItemValue<Domain>> mapOfDomainByUrl = new HashMap<>();
		systemHelper.getDomainService().all().forEach(d -> {
			CertifEngineFactory.get(d.uid).ifPresent(c -> {
				if (c != null && LetsEncryptCertificate.isTosApproved(c.getDomain().value)) {
					Optional.ofNullable(systemHelper.getExternalUrl(d.uid)).ifPresent(e -> mapOfDomainByUrl.put(e, d));
				}
			});
		});
		return mapOfDomainByUrl;
	}

	private List<ItemValue<Server>> getServers() {
		IServer serverService = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> servers = serverService.allComplete();
		logger.info(servers.size() + " Servers found");
		return servers;
	}

}
