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
package net.bluemind.filehosting.service.export;

import java.util.List;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.filehosting.api.FileHostingItem;

public interface IInternalFileHostingService extends IFileHostingService {

	public List<String> getShareUidsByPath(String path) throws ServerFault;

	public FileHostingItem getComplete(SecurityContext ctx, String uid) throws ServerFault;

	public Stream getSharedFile(SecurityContext ctx, String uid) throws ServerFault;

}
