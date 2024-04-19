package net.bluemind.milter;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.system.api.SysConfKeys;

/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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

public class SysconfHelper {
	public static final Supplier<String> defaultDomain;
	public static AtomicReference<SharedMap<String, String>> sysconf;

	static {
		sysconf = new AtomicReference<>();
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));

		defaultDomain = () -> Optional.ofNullable(sysconf.get())
				.map(sm -> sm.get(SysConfKeys.default_domain.name()) != null
						&& !sm.get(SysConfKeys.default_domain.name()).isEmpty()
								? sm.get(SysConfKeys.default_domain.name())
								: null)
				.orElse(null);
	}
}
