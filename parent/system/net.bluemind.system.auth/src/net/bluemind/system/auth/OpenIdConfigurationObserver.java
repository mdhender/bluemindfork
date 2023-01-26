/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.auth;

import java.util.Optional;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.openid.utils.AccessTokenValidator;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class OpenIdConfigurationObserver implements ISystemConfigurationObserver {

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		String prev = Optional.ofNullable(previous.stringValue(SysConfKeys.openid_host.name())).orElse("");
		String now = Optional.ofNullable(conf.stringValue(SysConfKeys.openid_host.name())).orElse("");
		if (!now.equals(prev)) {
			AccessTokenValidator.invalidateCache();
		}
	}

}
