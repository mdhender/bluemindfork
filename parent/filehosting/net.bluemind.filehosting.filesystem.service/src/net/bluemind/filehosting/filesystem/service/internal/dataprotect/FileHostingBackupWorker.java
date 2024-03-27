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
package net.bluemind.filehosting.filesystem.service.internal.dataprotect;

import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.IBackupWorker;
import net.bluemind.dataprotect.api.IDPContext;
import net.bluemind.filehosting.filesystem.service.internal.FileSystemFileHostingService;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;

public class FileHostingBackupWorker implements IBackupWorker {

	@Override
	public boolean supportsTag(String tag) {
		return TagDescriptor.bm_filehosting.getTag().equals(tag);
	}

	@Override
	public void prepareDataDirs(IDPContext ctx, String tag, ItemValue<Server> toBackup) throws ServerFault {
		// faudra m'expliquer ce qu'on backup la ?
	}

	@Override
	public Set<String> getDataDirs() {
		return Sets.newHashSet(FileSystemFileHostingService.DEFAULT_STORE_PATH);
	}

	@Override
	public String getDataType() {
		return "filehosting";
	}

}
