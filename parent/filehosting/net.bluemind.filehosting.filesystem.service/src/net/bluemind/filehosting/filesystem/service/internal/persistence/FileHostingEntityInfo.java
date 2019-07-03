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
package net.bluemind.filehosting.filesystem.service.internal.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;

public class FileHostingEntityInfo {

	public String path;
	public Date created;
	public String owner;

	public FileHostingEntityInfo() {
	}

	public FileHostingEntityInfo(String relativePath, String owner) {
		this.path = relativePath;
		this.owner = owner;
		this.created = new Date();
	}

	public static final Creator<FileHostingEntityInfo> filehostingInfoCreator = new Creator<FileHostingEntityInfo>() {
		@Override
		public FileHostingEntityInfo create(ResultSet con) throws SQLException {
			return new FileHostingEntityInfo();
		}
	};

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("PATH: %s\r\n", path));
		builder.append(String.format("CREATED: %s\r\n", created.toString()));
		builder.append(String.format("OWNER: %s\r\n", owner));
		return builder.toString();
	}

}
