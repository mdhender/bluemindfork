/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.filehosting.webdav.service.internal;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.filehosting.webdav.service.WebDavFileHostingService;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.ExternalSystem.AuthKind;
import net.bluemind.user.api.UserAccount;

public class WebDavTestImpl extends WebDavFileHostingService implements IFileHostingService {

	@Override
	protected ConnectionContext getConnectionContext(SecurityContext context) {
		UserAccount account = new UserAccount();
		account.login = "admin";
		account.credentials = "admin";
		ExternalSystem system = new ExternalSystem();
		system.authKind = AuthKind.SIMPLE_CREDENTIALS;
		system.description = "Jackrabbit demo server";
		system.identifier = "Jackrabbit";
		String baseUrl = detectBaseUrl();
		ConnectionContext ctx = new ConnectionContext(account, system, baseUrl);
		return ctx;
	}

	private String detectBaseUrl() {
		String ip = DockerEnv.getIp(DockerContainer.WEBDAV.getName());
		return String.format("http://%s/repository/default", ip);
	}

	@Override
	public FileHostingInfo info(SecurityContext context) {
		FileHostingInfo info = new FileHostingInfo();
		info.info = "Jackrabbit in a box";
		return info;
	}

	@Override
	public FileHostingPublicLink share(SecurityContext context, String path, Integer downloadLimit,
			String expirationDate) throws ServerFault {
		throw new UnsupportedOperationException("Not natively supported by the WebDav protocol");
	}

	@Override
	public void unShare(SecurityContext context, String url) throws ServerFault {
		throw new UnsupportedOperationException("Not natively supported by the WebDav protocol");
	}

}
