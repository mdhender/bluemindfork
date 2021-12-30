/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.api;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.tag.api.TagRef;

@BMApi(version = "3")
public class VNote {

	@BMApi(version = "3")
	public enum Color {
		BLUE, GREEN, PINK, YELLOW, WHITE;
	}

	public Integer height;
	public Integer width;
	public Integer posX;
	public Integer posY;
	public Color color = Color.YELLOW;
	public String body;
	public String subject;
	public List<TagRef> categories = new ArrayList<>();

}
