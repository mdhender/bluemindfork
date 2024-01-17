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
package net.bluemind.eas.dto.base;

public enum BodyType {

	PlainText(1), HTML(2), RTF(3), MIME(4);

	private final String xmlValue;

	private BodyType(int value) {
		xmlValue = Integer.toString(value);
	}

	public String xmlValue() {
		return xmlValue;
	}

	public static final BodyType getValueOf(int s) {
		switch (s) {
		case 1:
			return BodyType.PlainText;
		case 2:
			return HTML;
		case 3:
			return RTF;
		case 4:
			return MIME;
		default:
			return null;
		}
	}
}
