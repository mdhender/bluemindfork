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
package net.bluemind.core.jdbc.persistence;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.jdbc.SchemaDescriptor;

@SuppressWarnings("serial")
public class JdbcSchemaException extends Exception {

	private List<JdbcSchemaException> exceptions;
	private SchemaDescriptor schema;

	public JdbcSchemaException(String message) {
		super(message);
	}

	public JdbcSchemaException(SchemaDescriptor schema) {
		super();
		this.schema = schema;

	}

	@Override
	public String getMessage() {
		if (exceptions != null) {
			StringBuilder sb = new StringBuilder();
			for (JdbcSchemaException e : exceptions) {
				sb.append(e.getMessage() + "\n");
			}
			return sb.toString();
		} else {
			return "schema version doesnt correspond " + schema;
		}
	}

	public JdbcSchemaException(List<JdbcSchemaException> exceptions) {
		this.exceptions = exceptions;
	}

	public static class Builder {
		private List<JdbcSchemaException> exceptions = new ArrayList<>();

		public void add(JdbcSchemaException e) {
			exceptions.add(e);
		}

		public void throwIfNotEmtpy() throws JdbcSchemaException {
			if (!exceptions.isEmpty()) {
				throw new JdbcSchemaException(exceptions);
			}
		}
	}

	public static Builder builder() {
		return new Builder();
	}

}
