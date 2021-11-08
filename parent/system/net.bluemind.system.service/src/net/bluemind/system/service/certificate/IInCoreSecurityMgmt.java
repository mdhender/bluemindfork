/*BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.service.certificate;

import java.util.Map;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.api.ISecurityMgmt;

public interface IInCoreSecurityMgmt extends ISecurityMgmt {

	/**
	 * Filtered domains on ssl_certif_engine as LETS_ENCRYPT
	 * 
	 * @return Map of domains with external URL as key, and domain as value
	 */
	public Map<String, ItemValue<Domain>> getLetsEncryptDomainExternalUrls();
}
