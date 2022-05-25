/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.xfer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

public class CleanupOpsAccumulator {
	@FunctionalInterface
	public static interface SQLRunnable {
		public abstract void run() throws SQLException;
	}

	private List<SQLRunnable> ops;

	public CleanupOpsAccumulator() {
		ops = new ArrayList<>();
	}

	public void accept(SQLRunnable op) {
		ops.add(op);
	}

	public void executeAll(Logger logger) {
		for (SQLRunnable op : ops) {
			try {
				op.run();
			} catch (Throwable t) {
				logger.warn("cleanup operation failed: {}", t.getMessage());
			}
		}
	}
}
