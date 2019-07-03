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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.jdbc.JdbcAbstractStore;

public class FileHostingStore extends JdbcAbstractStore {

	Logger logger = LoggerFactory.getLogger(FileHostingStore.class);

	public FileHostingStore(DataSource dataSource) {
		super(dataSource);
	}

	public void create(FileHostingEntityInfo value) throws ServerFault {
		logger.debug("Creating FileHosting entity info:\r\n {}", value);
		StringBuilder query = new StringBuilder("INSERT INTO t_filehosting_file_info ( ");
		FileHostingInfoColumns.cols.appendNames(null, query);
		query.append(") VALUES (");
		FileHostingInfoColumns.cols.appendValues(query);
		query.append(")");

		try {
			insert(query.toString(), value, FileHostingInfoColumns.statementValues());
		} catch (SQLException e) {
			throw new ServerFault(
					"Cannot create FileHosting entity info from " + value.toString() + ": " + e.getMessage());
		}
	}

	public List<FileHostingEntityInfo> getExpiredFiles(int retentionTime) throws SQLException {
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		String now = date.format(new Date());

		StringBuilder query = new StringBuilder("SELECT ");
		FileHostingInfoColumns.cols.appendNames(null, query);
		query.append(" FROM t_filehosting_file_info ");
		query.append(String.format(" WHERE created < ('%s'::date - '%d day'::interval);", now, retentionTime));

		return select(query.toString(), FileHostingEntityInfo.filehostingInfoCreator,
				FileHostingInfoColumns.populator(), new Object[0]);
	}

	public List<FileHostingEntity> getActiveSharedFiles() throws SQLException {
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		String now = date.format(new Date());

		StringBuilder query = new StringBuilder("SELECT ");
		FileHostingColumns.cols.appendNames(null, query);
		query.append(" FROM t_filehosting_file ");
		query.append(String.format(" WHERE expiration_date is null or expiration_date > ('%s'::date);", now));

		return select(query.toString(), FileHostingEntity.filehostingCreator, FileHostingColumns.populator(),
				new Object[0]);
	}

	public void create(FileHostingEntity value) throws ServerFault {
		logger.debug("Creating FileHosting entity:\r\n {}", value);
		StringBuilder query = new StringBuilder("INSERT INTO t_filehosting_file ( ");
		FileHostingColumns.cols.appendNames(null, query);
		query.append(") VALUES (");
		FileHostingColumns.cols.appendValues(query);
		query.append(")");

		try {
			insert(query.toString(), value, FileHostingColumns.statementValues());
		} catch (SQLException e) {
			throw new ServerFault("Cannot create FileHosting entity from " + value.toString() + ": " + e.getMessage());
		}
	}

	public void delete(String uid) {
		try {
			delete("DELETE FROM t_filehosting_file WHERE uid = ?", new Object[] { uid });
		} catch (SQLException e) {
			logger.warn("Cannot delete FileHosting item " + uid, e);
		}
	}

	public FileHostingEntity getByPath(String path) throws ServerFault {
		try {
			return getBy("path", path);
		} catch (SQLException e) {
			throw new ServerFault("FileHosting path " + path + " not found: " + e.getMessage());
		}
	}

	public FileHostingEntity getByUid(String uid) throws ServerFault {
		try {
			return getBy("uid", uid);
		} catch (SQLException e) {
			throw new ServerFault("FileHosting UID " + uid + " not found: " + e.getMessage());
		}
	}

	public List<FileHostingEntity> getByOwner(String owner) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");

		FileHostingColumns.cols.appendNames(null, query);

		query.append(" FROM t_filehosting_file");
		query.append(" WHERE owner = ?");
		return select(query.toString(), FileHostingEntity.filehostingCreator, FileHostingColumns.populator(),
				new Object[] { owner });

	}

	private FileHostingEntity getBy(String column, String value) throws SQLException {
		StringBuilder query = new StringBuilder("SELECT ");

		FileHostingColumns.cols.appendNames(null, query);

		query.append(" FROM t_filehosting_file");
		query.append(" WHERE " + column + " = ?");
		FileHostingEntity entity = unique(query.toString(), FileHostingEntity.filehostingCreator,
				FileHostingColumns.populator(), new Object[] { value });

		return entity;
	}

	public void update(FileHostingEntity value) throws SQLException {
		StringBuilder query = new StringBuilder("UPDATE t_filehosting_file SET (");

		FileHostingColumns.cols.appendNames(null, query);
		query.append(") = (");
		FileHostingColumns.cols.appendValues(query);
		query.append(") WHERE uid = '").append(value.uid).append("'");

		update(query.toString(), value, FileHostingColumns.statementValues());
	}

	public List<FileHostingEntityInfo> deleteExpiredFiles(int retentionTimeInDays, String domainUid)
			throws SQLException {
		final List<FileHostingEntity> activeSharedFiles = getActiveSharedFiles();
		List<FileHostingEntityInfo> expiredFiles = getExpiredFiles(retentionTimeInDays).stream()
				.filter(fileinfo -> fileinfo.path.startsWith(domainUid)) //
				.filter(fileinfo -> {
					return activeSharedFiles.stream().filter(activeFile -> {
						return activeFile.path.equals(fileinfo.path);
					}).count() == 0;
				}) //
				.collect(Collectors.toList());
		delete(expiredFiles);
		return expiredFiles;
	}

	private void delete(List<FileHostingEntityInfo> expiredFiles) throws SQLException {
		String deleteTemplateInfo = "DELETE FROM t_filehosting_file WHERE owner = ? AND path = ?";
		String deleteTemplate = "DELETE FROM t_filehosting_file_info WHERE owner = ? AND path = ?";

		for (FileHostingEntityInfo fileHostingEntityInfo : expiredFiles) {
			delete(deleteTemplate, new Object[] { fileHostingEntityInfo.owner, fileHostingEntityInfo.path });
			delete(deleteTemplateInfo, new Object[] { fileHostingEntityInfo.owner, fileHostingEntityInfo.path });
		}
	}

	public Long getExpirationDate(String uid) throws ServerFault {

		try {
			FileHostingEntity fhe = getBy("uid", uid);
			if (fhe != null && fhe.expirationDate != null) {
				return fhe.expirationDate.getTime();
			}
		} catch (SQLException e) {
			throw new ServerFault("FileHosting uid " + uid + " not found: " + e.getMessage());
		}

		return null;
	}

}
