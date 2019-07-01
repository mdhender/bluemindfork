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
package net.bluemind.core.jdbc.schemaextractor;

import java.io.File;
import java.io.IOException;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import net.bluemind.core.jdbc.SchemaDescriptor;
import net.bluemind.core.jdbc.SchemaDescriptors;

public class SchemaExtractor {

	public void extract() throws IOException {
		SchemaDescriptors descriptors = new SchemaDescriptors();

		StringBuilder sb = new StringBuilder();
		for (SchemaDescriptor descr : descriptors.getDescriptors()) {
			byte[] schema = ByteStreams.toByteArray(descr.read());
			sb.append("-- schema ").append(descr.getName()).append(descr.getVersion()).append("\n");
			sb.append(new String(schema));
			sb.append("\n\n");
			System.out.println("registred schema " + descr.getName());
		}

		Files.write(sb.toString().getBytes(), new File("target/schema.sql"));
	}
}
