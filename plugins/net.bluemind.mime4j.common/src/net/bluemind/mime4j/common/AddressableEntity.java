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
package net.bluemind.mime4j.common;

import java.util.stream.Collectors;

import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.codec.DecoderUtil;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.dom.field.ContentDispositionField;
import org.apache.james.mime4j.dom.field.ContentTypeField;

public class AddressableEntity implements Entity {

	private Entity entity;
	private String mimeAddress;

	public AddressableEntity(Entity e, String mimeAddress) {
		this.entity = e;
		this.mimeAddress = mimeAddress;
	}

	public Entity getParent() {
		return entity.getParent();
	}

	public void dispose() {
		entity.dispose();
	}

	public void setParent(Entity parent) {
		entity.setParent(parent);
	}

	public Header getHeader() {
		return entity.getHeader();
	}

	public void setHeader(Header header) {
		entity.setHeader(header);
	}

	public Body getBody() {
		return entity.getBody();
	}

	public void setBody(Body body) {
		entity.setBody(body);
	}

	public Body removeBody() {
		return entity.removeBody();
	}

	public boolean isMultipart() {
		return entity.isMultipart();
	}

	public String getMimeType() {
		return entity.getMimeType();
	}

	public String getCharset() {
		return entity.getCharset();
	}

	public String getContentTransferEncoding() {
		return entity.getContentTransferEncoding();
	}

	public String getDispositionType() {
		return entity.getDispositionType();
	}

	public String getFilename() {
		return AddressableEntity.getFileName(entity);
	}

	public static String getFileName(Entity e) {
		String filename = e.getFilename();
		if (filename == null) {
			ContentTypeField ctype = (ContentTypeField) e.getHeader().getField("Content-Type");
			ContentDispositionField cdf = (ContentDispositionField) e.getHeader().getField("Content-Disposition");
			if (ctype != null && ctype.getParameter("name") != null) {
				filename = ctype.getParameter("name");
			} else if (cdf != null && cdf.getParameter("filename") != null) {
				filename = cdf.getParameter("filename");
			} else if (cdf != null) {
				if (cdf.isInline()) {
					return null;
				}
				String tfilename = cdf.getParameters().entrySet().stream()
						.filter(entry -> entry.getKey().startsWith("filename*"))
						.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).map(entry -> entry.getValue())
						.collect(Collectors.joining());

				if (tfilename.length() > 0) {
					filename = tfilename;
				}
			}
		}
		if (filename != null) {
			filename = DecoderUtil.decodeEncodedWords(filename, DecodeMonitor.SILENT);
		}

		return filename;
	}

	public String getMimeAddress() {
		return mimeAddress;
	}

}
