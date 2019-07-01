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
package net.bluemind.tag.api;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.Item;

/**
 * This class is used to associate a color and a label to an item
 *
 */
@BMApi(version = "3")
public class Tag {

	/**
	 * A keyword we want to associate to an {@link Item}
	 */
	public String label;

	/**
	 * hexadecimal code for a color, eg. 6f6f6f means grey.
	 */
	public String color;

	public static Tag create(String label, String color) {
		Tag ret = new Tag();
		ret.label = label;
		ret.color = color;
		return ret;
	}
}
