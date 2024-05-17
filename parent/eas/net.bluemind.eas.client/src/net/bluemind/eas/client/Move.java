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
package net.bluemind.eas.client;

import org.w3c.dom.Element;

public class Move {

	private String srcMsgId;
	private String srcFldId;
	private String dstFldId;

	private Element xml;

	public String getSrcMsgId() {
		return srcMsgId;
	}

	public void setSrcMsgId(String srcMsgId) {
		this.srcMsgId = srcMsgId;
	}

	public String getDstFldId() {
		return dstFldId;
	}

	public void setDstFldId(String dstFldId) {
		this.dstFldId = dstFldId;
	}

	public String getSrcFldId() {
		return srcFldId;
	}

	public void setSrcFldId(String srcFldId) {
		this.srcFldId = srcFldId;
	}

	public Element getXml() {
		return xml;
	}

	public void setXml(Element xml) {
		this.xml = xml;
	}

}
