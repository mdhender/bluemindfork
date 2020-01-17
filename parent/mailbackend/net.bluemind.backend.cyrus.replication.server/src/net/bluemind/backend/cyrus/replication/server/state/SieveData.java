/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.server.state;

import java.util.Optional;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.api.SieveScript;

public class SieveData {

	final SieveScript script;
	final Optional<String> literalRef;

	private SieveData(SieveScript sc, String literal) {
		script = sc;
		literalRef = Optional.ofNullable(literal);
	}

	public static SieveData of(JsonObject js) {
		return new SieveData(SieveScript.of(js), js.getString("CONTENT"));
	}

	public static SieveData of(SieveScript script) {
		return new SieveData(script, null);
	}

	/**
	 * @return a copy with the compiled bytecode filename
	 */
	public SieveData compiled() {
		return new SieveData(new SieveScript(script.userId, script.fileName.replace(".sieve.script", ".sieve.bc"),
				script.lastUpdate, script.isActive), null);
	}

}
