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
package net.bluemind.dav.server.proto.props.appleical;

public enum BMCalColor {

	BLUE1("bm.blue1", "#7097F5"), //
	ORANGE1("bm.orange1", "#F59B4C"), //
	GREEN1("bm.green1", "#2FD997"), //
	RED1("bm.red1", "#E75757"), //
	PURPLE1("bm.purple1", "#BB6EE4"), //
	GREY1("bm.grey1", "#A3A3A4"), //
	PINK1("bm.pink1", "#FF99FF"), //

	BLUE2("bm.blue2", "#81B4E9"), //
	ORANGE2("bm.orange2", "#FCAA0B"), //
	GREEN2("bm.green2", "#5AD442"), //
	RED2("bm.red2", "#FC7438"), //
	PURPLE2("bm.purple2", "#D688DB"), //
	GREY2("bm.grey2", "#C1B281"), //
	PINK2("bm.pink2", "#FF05FF"), //

	BLUE3("bm.blue3", "#3E52C6"), //
	ORANGE3("bm.orange3", "#B8784E"), //
	GREEN3("bm.green3", "#578E83"), //
	RED3("bm.red3", "#D0347F"), //
	PURPLE3("bm.purple3", "#B936C3"), //
	GREY3("bm.grey3", "#A29C9F"), //
	PINK3("bm.pink3", "#D100D1");

	private final String symbolic;
	private final String rgb;

	private BMCalColor(String symbolic, String rgb) {
		this.symbolic = symbolic;
		this.rgb = rgb + "FF";
	}

	public String getSymbolic() {
		return symbolic;
	}

	public String getRgb() {
		return rgb;
	}

}
