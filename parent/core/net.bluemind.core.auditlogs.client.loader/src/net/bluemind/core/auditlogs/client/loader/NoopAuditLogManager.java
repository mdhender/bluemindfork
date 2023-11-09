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

package net.bluemind.core.auditlogs.client.loader;

import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.exception.AuditLogCreationException;

public final class NoopAuditLogManager implements IAuditLogMgmt {

	public static final IAuditLogMgmt INSTANCE = new NoopAuditLogManager();

	@Override
	public void setupAuditBackingStore() throws AuditLogCreationException {
		//

	}

	@Override
	public void setupAuditBackingStoreForDomain(String domainUid) throws AuditLogCreationException {
		//

	}

	@Override
	public void removeAuditBackingStore() {
		//

	}

	@Override
	public void removeAuditBackingStoreForDomain(String domainUid) {
		//

	}

	@Override
	public boolean hasAuditBackingStoreForDomain(String domainUid) {
		return false;
	}

	@Override
	public boolean hasAuditBackingStore() {
		return false;
	}

}
