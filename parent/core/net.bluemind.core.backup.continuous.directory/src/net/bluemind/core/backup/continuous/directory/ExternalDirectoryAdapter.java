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
package net.bluemind.core.backup.continuous.directory;

import java.util.Collections;
import java.util.List;

import net.bluemind.config.InstallationId;
import net.bluemind.directory.external.IExternalDirectory;
import net.bluemind.directory.external.IExternalDirectoryProvider;

public class ExternalDirectoryAdapter implements IExternalDirectoryProvider {

	private final List<IExternalDirectory> realDomainDirs;

	public ExternalDirectoryAdapter() {
		System.err.println("ext dir adapter");
		String install = InstallationId.getIdentifier();
//		realDomainDirs = DefaultBackupStore.get().forInstallation(install).listAvailable().stream()
//				.filter(ls -> ls.type().equals("dir") && !ls.domainUid().equals("global.virt"))
//				.map(LiveStreamAdapter::new).collect(Collectors.toList());
		realDomainDirs = Collections.emptyList();
	}

	@Override
	public List<IExternalDirectory> getAvailable() {
		return realDomainDirs;
	}

}
