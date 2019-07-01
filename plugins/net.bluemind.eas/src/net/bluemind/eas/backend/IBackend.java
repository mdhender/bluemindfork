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
package net.bluemind.eas.backend;

import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.ActiveSyncException;

/**
 * Main interface for EAS data access
 * 
 */
public interface IBackend {

	IHierarchyImporter getHierarchyImporter(BackendSession bs);

	IHierarchyExporter getHierarchyExporter(BackendSession bs);

	IContentsImporter getContentsImporter(BackendSession bs);

	IContentsExporter getContentsExporter(BackendSession bs);

	MSUser getUser(String loginAtDomain, String password) throws ActiveSyncException;

	String getPictureBase64(BackendSession bs, int photoId);

	public void acknowledgeRemoteWipe(BackendSession bs);

	/**
	 * Called on EAS request start
	 * 
	 * @param bs
	 */
	void initInternalState(BackendSession bs);

	void purgeSessions();

}
