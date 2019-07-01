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
package net.bluemind.imap.mime;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.bluemind.imap.mime.impl.LeafPartsIterator;

public class MimePart implements Iterable<MimePart> {

	protected List<MimePart> children;

	private String mimeType;
	private String mimeSubtype;
	private MimePart parent;
	private int idx;
	private String contentTransfertEncoding;
	private String contentId;
	private Map<String, BodyParam> bodyParams;

	public MimePart() {
		children = new LinkedList<MimePart>();
		bodyParams = Collections.emptyMap();
	}

	public void addPart(MimePart child) {
		children.add(child);
		child.idx = children.size();
		child.parent = this;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMimeSubtype() {
		return mimeSubtype;
	}

	public void setMimeSubtype(String mimeSubtype) {
		this.mimeSubtype = mimeSubtype;
	}

	public List<MimePart> getChildren() {
		return children;
	}

	private boolean isRoot() {
		return parent == null;
	}

	public String getAddress() {
		StringBuilder sb = new StringBuilder();
		MimePart cur = this;
		while (!cur.isRoot()) {
			if (sb.length() > 0) {
				sb.insert(0, ".");
			}
			sb.insert(0, cur.idx);
			cur = cur.parent;
		}
		return sb.toString();
	}

	public String getFullMimeType() {
		StringBuilder sb = new StringBuilder(50);
		sb.append(getMimeType() != null ? getMimeType().toLowerCase() : "null");
		sb.append("/");
		sb.append(getMimeSubtype() != null ? getMimeSubtype().toLowerCase() : "null");
		return sb.toString();
	}

	public String getContentTransfertEncoding() {
		return contentTransfertEncoding;
	}

	public void setContentTransfertEncoding(String contentTransfertEncoding) {
		this.contentTransfertEncoding = contentTransfertEncoding;
	}

	public Collection<BodyParam> getBodyParams() {
		return bodyParams.values();
	}

	public BodyParam getBodyParam(final String param) {
		return bodyParams.get(param);
	}

	public void setBodyParams(Set<BodyParam> bodyParams) {
		HashMap<String, BodyParam> params = new HashMap<String, BodyParam>();
		for (BodyParam param : bodyParams) {
			params.put(param.getKey(), param);
		}
		this.bodyParams = params;
	}

	public boolean isAttachment() {
		return (idx > 1 && getMimeType() != null && !"html".equalsIgnoreCase(getMimeSubtype()))
				|| !"text".equalsIgnoreCase(getMimeType());
	}

	public String getContentId() {
		return contentId;
	}

	public void setContentId(String contentId) {
		this.contentId = contentId;
	}

	public int getIdx() {
		return idx;
	}

	public MimePart getParent() {
		return parent;
	}

	@Override
	public Iterator<MimePart> iterator() {
		return new LeafPartsIterator(this);
	}

	private String retrieveMethodFromCalendarPart() {
		if ("text/calendar".equals(getFullMimeType())) {
			BodyParam method = getBodyParam("method");
			if (method != null) {
				return method.getValue();
			}
		}
		return null;
	}

	public boolean isInvitation() {
		String method = retrieveMethodFromCalendarPart();
		return "REQUEST".equalsIgnoreCase(method);
	}

	public boolean isCancelInvitation() {
		String method = retrieveMethodFromCalendarPart();
		return "CANCEL".equalsIgnoreCase(method);
	}
}
