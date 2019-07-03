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
package net.bluemind.calendar.pdf.internal;

import java.awt.Color;

public class ColorPalette {

	public static final String[] DEFAULT_COLORS = { "#3D99FF", "#FF6638", "#62CD00", "#D07BE3", "#FFAD40", "#9E9E9E",
			"#00D5D5", "#F56A9E", "#E9D200", "#A77F65", "#B3CB00", "#B6A5E9", "#4C3CD9", "#B00021", "#6B9990",
			"#A8A171", "#860072", "#8C98BA", "#C98FA4", "#725299", "#5C5C5C" };

	public static Color highContrast(Color c) {
		// http://www.w3.org/TR/AERT#color-contrast

		Color[] suggestions = { Color.BLACK, Color.WHITE };

		int diff = 0;
		int retIndex = -1;
		for (int i = 0; i < suggestions.length; i++) {
			Color sugg = suggestions[i];
			int currDiff = Math.abs(brightness(sugg) - brightness(c));
			if (currDiff > diff) {
				diff = currDiff;
				retIndex = i;
			}
		}

		return suggestions[retIndex];
	}

	private static int brightness(Color c) {
		return Math.round((c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000f);
	}

	public static Color decode(String color) {
		Color c = Color.decode(color);
		return c;
	}

	public static String lighter(String color) {
		return encode(decode(color).brighter());
	}

	public static String darker(String color) {
		return encode(decode(color).darker());
	}

	private static String encode(Color col) {
		return "#" + Integer.toHexString((col.getRGB() & 0xffffff) | 0x1000000).substring(1);
	}

	public static String textColor(String color) {
		return encode(highContrast(decode(color)));
	}

	public static void main(String[] args) {
		System.out.println(encode(highContrast(decode("#dcffe1"))));
	}
}
