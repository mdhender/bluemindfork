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

package net.bluemind.directory.hollow.datamodel;

import java.util.List;

import com.netflix.hollow.core.write.objectmapper.HollowInline;

public class Email {

	@HollowInline
	public String address;
	public List<String> ngrams;
	public boolean allAliases;
	public boolean isDefault;

	public Email(String address, List<String> ngrams, boolean isDefault, boolean allAliases) {
		this.address = address;
		this.ngrams = ngrams;
		this.isDefault = isDefault;
		this.allAliases = allAliases;
	}

}