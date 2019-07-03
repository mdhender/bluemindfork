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
package net.bluemind.core.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class SchemaDescriptor {

	private String name;
	private String version;
	private URL schema;
	private List<String> requiredSchemas;
	private boolean ignoreErrors;

	public SchemaDescriptor(String name, String version, URL url, List<String> requiredSchemas, boolean ignoreErrors) {
		this.name = name;
		this.version = version;
		this.schema = url;
		this.requiredSchemas = requiredSchemas;
		this.ignoreErrors = ignoreErrors;
	}

	public List<String> getRequiredSchemas() {
		return requiredSchemas;
	}

	public String getId() {
		return name;
	}

	public InputStream read() throws IOException {
		return schema.openStream();
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public boolean isIgnoreErrors() {
		return ignoreErrors;
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		SchemaDescriptor other = (SchemaDescriptor) obj;
		return getId().equals(other.getId());
	}

}
