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
package net.bluemind.system.auth;

import java.net.URL;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class CasAuthConfigurationValidator implements ISystemConfigurationValidator {

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {

		if (!"CAS".equals(modifications.get(SysConfKeys.auth_type.name()))) {
			return;
		}

		String url = modifications.get(SysConfKeys.cas_url.name());

		// validate url
		if (StringUtils.isEmpty(url)) {
			throw new ServerFault("CAS url must be setted", ErrorCode.INVALID_PARAMETER);
		}

		URL purl = null;
		try {
			purl = new URL(url);
		} catch (Exception e) {
			throw new ServerFault("CAS url is not valid", ErrorCode.INVALID_PARAMETER);
		}

		if (!"https".equals(purl.getProtocol())) {
			throw new ServerFault("CAS url must use HTTPS protocol", ErrorCode.INVALID_PARAMETER);
		}

		if (!purl.getPath().endsWith("/")) {
			throw new ServerFault("CAS url must finish with '/'", ErrorCode.INVALID_PARAMETER);
		}
	}
}
