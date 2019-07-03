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
package net.bluemind.dav.server.proto.report.webdav;

import javax.xml.namespace.QName;

public class PropMatch {

	private QName prop;
	private String value;
	private MatchStyle style;

	public QName getProp() {
		return prop;
	}

	public void setProp(QName prop) {
		this.prop = prop;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public MatchStyle getStyle() {
		return style;
	}

	public void setStyle(MatchStyle style) {
		this.style = style;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(32);
		sb.append(prop.toString()).append(' ');
		sb.append(style).append(' ');
		sb.append(value);
		return sb.toString();
	}

}
