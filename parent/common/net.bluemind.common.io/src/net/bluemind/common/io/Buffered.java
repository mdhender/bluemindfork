/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.common.io;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

import net.bluemind.configfile.ReloadableConfig;
import net.bluemind.configfile.core.CoreConfig;
import net.bluemind.lib.vertx.VertxPlatform;

public class Buffered {

	private static final ReloadableConfig coreConf = new ReloadableConfig(VertxPlatform.getVertx(), CoreConfig::get);

	private Buffered() {

	}

	public static BufferedOutputStream output(OutputStream raw) {
		return new BufferedOutputStream(raw, writeBuffer());
	}

	private static final int writeBuffer() {
		return (int) coreConf.config().getMemorySize(CoreConfig.Io.WRITE_BUFFER).toBytes();
	}

}
