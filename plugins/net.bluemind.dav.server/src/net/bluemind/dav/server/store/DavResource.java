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
package net.bluemind.dav.server.store;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.dav.server.DavActivator;

public abstract class DavResource {

	private static final Logger logger = LoggerFactory.getLogger(DavResource.class);

	protected final String path;
	private final ResType resType;
	private String etag;

	private String entryUid;

	protected DavResource(String path, ResType resType) {
		this.path = path;
		this.resType = resType;
	}

	public String getPath() {
		return path;
	}

	public abstract boolean hasProperty(QName prop);

	public abstract Set<QName> getDefinedProperties();

	@Override
	public String toString() {
		return "DavResource [path=" + path + ", definedProperties=" + getDefinedProperties() + "]";
	}

	public abstract List<QName> getTypes();

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public ResType getResType() {
		return resType;
	}

	public String getUid() {
		if (entryUid == null) {
			try {
				Matcher m = resType.matcher(path);
				if (m.find()) {
					entryUid = m.group(1);
				} else {
					throw new IllegalStateException("no match for path '" + path + "'");
				}
			} catch (IllegalStateException ise) {
				logger.error("[" + resType + "] " + ise.getMessage() + " in '" + path + "'", ise);
				if (DavActivator.devMode) {
					System.exit(1);
				}
			}
		}
		return entryUid;
	}

}
