/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.index.schema;

import net.bluemind.lib.elasticsearch.ISchemaMatcher;

public class MailspoolSchemaMatcher implements ISchemaMatcher {

	@Override
	public boolean supportsIndexName(String indexName) {
		if (indexName.equals("mailspool")) {
			return true;
		}
		if (indexName.startsWith("mailspool_")) {
			String enumeration = indexName.split("_")[1];
			return enumeration.matches("\\d+");
		}

		return false;
	}

}
