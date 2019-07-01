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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bluemind.core.jdbc.JdbcAbstractStore.Creator;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileType;
import net.bluemind.filehosting.api.Metadata;

public class FileHostingEntity {

	public String uid;
	public String path;
	public Map<String, String> metadata;
	public String owner;
	public Integer downloadLimit;
	public Date expirationDate;
	public Integer accessCount;
	public Date lastAccess;

	public FileHostingEntity() {
		metadata = new HashMap<>();
	}

	public static final Creator<FileHostingEntity> filehostingCreator = new Creator<FileHostingEntity>() {
		@Override
		public FileHostingEntity create(ResultSet con) throws SQLException {
			return new FileHostingEntity();
		}
	};

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("UID: %s\r\n", uid));
		builder.append(String.format("PATH: %s\r\n", path));
		builder.append(String.format("OWNER: %s\r\n", owner));
		for (String key : metadata.keySet()) {
			builder.append(String.format("METADATA: %s -> %s\r\n", key, metadata.get(key)));
		}
		return builder.toString();
	}

	public FileHostingItem toFileHostingItem(File file) {
		FileHostingItem item = new FileHostingItem();
		item.path = path;
		item.name = file.getName();
		item.type = FileType.FILE;
		List<Metadata> meta = new ArrayList<>();
		for (String key : metadata.keySet()) {
			Metadata md = new Metadata(key, metadata.get(key));
			meta.add(md);
		}
		try {
			meta.add(new Metadata("mime-type", Files.probeContentType(file.toPath())));
		} catch (IOException e) {
			meta.add(new Metadata("mime-type", "application/octet-stream"));
		}
		meta.add(new Metadata("content-length", "" + file.length()));
		item.metadata = meta;
		return item;
	}

}
