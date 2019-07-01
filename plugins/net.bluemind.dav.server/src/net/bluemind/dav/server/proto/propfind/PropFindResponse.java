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
package net.bluemind.dav.server.proto.propfind;

import java.util.List;

import net.bluemind.dav.server.proto.IPropertyValue;

public class PropFindResponse {

	// response/href
	private String href;

	// response/propstat/status
	private int status;

	// response/propstat/prop
	private List<IPropertyValue> propValues;

	private String etag;

	private PropFindResponse next;

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public List<IPropertyValue> getPropValues() {
		return propValues;
	}

	public void setPropValues(List<IPropertyValue> propValues) {
		this.propValues = propValues;
	}

	public PropFindResponse getNext() {
		return next;
	}

	public void setNext(PropFindResponse next) {
		this.next = next;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

}
