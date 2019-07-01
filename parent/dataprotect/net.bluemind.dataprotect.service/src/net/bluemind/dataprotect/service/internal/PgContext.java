/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.dataprotect.service.internal;

import net.bluemind.dataprotect.api.PartGeneration;
import net.bluemind.dataprotect.service.IBackupWorker;
import net.bluemind.pool.Pool;

public class PgContext {

	public Pool pool;
	public IBackupWorker pgWorker;
	public PartGeneration pgPart;
	public String databaseName;

	public static PgContext create(Pool pool, IBackupWorker pgWorker, PartGeneration pgPart, String databaseName) {
		PgContext ret = new PgContext();
		ret.pool = pool;
		ret.pgWorker = pgWorker;
		ret.pgPart = pgPart;
		ret.databaseName = databaseName;
		return ret;
	}

}
