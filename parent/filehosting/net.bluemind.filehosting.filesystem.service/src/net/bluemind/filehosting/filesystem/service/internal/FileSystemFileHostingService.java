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
package net.bluemind.filehosting.filesystem.service.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.file.OpenOptions;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.filehosting.api.Configuration;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.ID;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.filehosting.filesystem.service.internal.persistence.FileHostingEntity;
import net.bluemind.filehosting.filesystem.service.internal.persistence.FileHostingEntityInfo;
import net.bluemind.filehosting.filesystem.service.internal.persistence.FileHostingStore;
import net.bluemind.filehosting.service.export.FileSizeExceededException;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.filehosting.service.export.SizeLimitedReadStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.node.api.FileDescription;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;

public class FileSystemFileHostingService implements IFileHostingService {
	private static final Logger logger = LoggerFactory.getLogger(FileSystemFileHostingService.class);
	private final File rootFolder;
	private final FileHostingStore store;
	public static final String DEFAULT_STORE_PATH = "/var/spool/bm-filehosting";
	private INodeClient nodeClient;

	public FileSystemFileHostingService() {
		this.rootFolder = new File(DEFAULT_STORE_PATH);
		store = new FileHostingStore(JdbcActivator.getInstance().getDataSource());
	}

	@Override
	public List<FileHostingItem> list(final SecurityContext context, String path) throws ServerFault {
		List<FileDescription> listFiles = getNodeClient().listFiles(getFilePath(path, context).getAbsolutePath());
		return FileHostingItemUtil.fromFileDescriptionList(listFiles, new File(rootFolder, getUserPath(context)));
	}

	@Override
	public List<FileHostingItem> find(final SecurityContext context, String query) throws ServerFault {
		File filepath = new File(rootFolder, getUserPath(context));
		final List<FileHostingItem> matches = new ArrayList<>();
		final String filenameQuery = query.toLowerCase();

		traverse(filepath.getAbsolutePath(), matches, filenameQuery, filepath);
		return matches;
	}

	private void traverse(String filepath, List<FileHostingItem> matches, String filenameQuery, File root)
			throws ServerFault {
		List<FileDescription> listFiles = getNodeClient().listFiles(filepath);
		for (FileDescription fileDescription : listFiles) {
			if (fileDescription.isDirectory()) {
				traverse(fileDescription.getPath(), matches, filenameQuery, root);
			} else {
				if (fileDescription.getName().toLowerCase().indexOf(filenameQuery) != -1) {
					matches.add(FileHostingItemUtil.fromFile(fileDescription, root));
				}
			}
		}
	}

	@Override
	public Stream get(SecurityContext context, String path) throws ServerFault {
		File file = createFilePath(path, context, true);
		if (!fileExists(file)) {
			throw new ServerFault(new FileNotFoundException(path));
		}
		return getFileStream(file);
	}

	@Override
	public Stream getSharedFile(SecurityContext context, String uid) throws ServerFault {
		FileHostingEntity entity = store.getByUid(uid);
		if (fileHasExpired(entity)) {
			throw new ServerFault(String.format("Shared file %s has reached expiration date: %s", uid,
					entity.expirationDate.toString()));
		}
		if (fileHasExceededDownloadLimit(entity)) {
			throw new ServerFault(
					String.format("Shared file %s has exceeded download limit: %s", uid, entity.downloadLimit));
		}
		try {
			updateAccess(entity);
		} catch (SQLException e) {
			logger.warn("Cannot update access infos of shared file {}:{}", uid, e.getMessage());
		}
		File file = new File(rootFolder, entity.path);
		return getFileStream(file);
	}

	private void updateAccess(FileHostingEntity entity) throws SQLException {
		entity.accessCount = entity.accessCount + 1;
		entity.lastAccess = new Date();
		store.update(entity);
	}

	private boolean fileHasExceededDownloadLimit(FileHostingEntity entity) {
		return entity.downloadLimit > 0 && (entity.accessCount >= entity.downloadLimit);
	}

	private boolean fileHasExpired(FileHostingEntity entity) {
		if (null == entity.expirationDate) {
			return false;
		}

		Calendar now = new GregorianCalendar();
		now.setTime(new Date());
		Calendar calExpiration = new GregorianCalendar();
		calExpiration.setTime(entity.expirationDate);
		return now.after(calExpiration);
	}

	@Override
	public FileHostingItem getComplete(SecurityContext context, String uid) throws ServerFault {
		FileHostingEntity entity = store.getByUid(uid);
		File file = new File(rootFolder, entity.path);
		// FIXME needs to get filesize via node client
		return entity.toFileHostingItem(file);
	}

	@Override
	public FileHostingPublicLink share(SecurityContext context, String path, Integer downloadLimit,
			String expirationDate) throws ServerFault {
		File fpath = createFilePath(path, context, true);
		if (!fileExists(fpath)) {
			throw new ServerFault(new FileNotFoundException(path));
		}
		String relativePath = rootFolder.toPath().relativize(fpath.toPath()).toString();
		String id = ID.generate();
		FileHostingEntity entity = new FileHostingEntity();
		entity.owner = context.getSubject();
		entity.path = relativePath;
		entity.uid = id;
		entity.accessCount = 0;
		entity.downloadLimit = null == downloadLimit || downloadLimit == -1 ? 0 : downloadLimit;
		entity.expirationDate = getDateFromIsoString(context, expirationDate);
		store.create(entity);
		String publicUri = String.format("%s/fh/bm-fh/%s", getServerAddress(), id);
		logger.debug("Sharing entity with path %s. uri: %s", relativePath, publicUri);

		FileHostingPublicLink ret = new FileHostingPublicLink();
		ret.url = publicUri;
		if (entity.expirationDate != null) {
			ret.expirationDate = entity.expirationDate.getTime();
		}

		return ret;
	}

	@Override
	public void unShare(SecurityContext context, String url) throws ServerFault {
		String uid = ID.extract(url);
		FileHostingEntity entity = store.getByUid(uid);
		if (entity.owner.equals(context.getSubject())) {
			store.delete(uid);
		}
	}

	private String getServerAddress() {
		BmConfIni ini = new BmConfIni();
		return "https://" + ini.get("external-url");
	}

	@Override
	public void store(SecurityContext context, String path, Stream document) throws ServerFault {
		long maxAttachmentSize = getConfiguration(context).maxFilesize;
		File file = createFilePath(path, context, false);
		logger.info(String.format("Storing file to %s", file.getAbsolutePath()));
		try (SizeLimitedReadStream readInputStream = new SizeLimitedReadStream(VertxStream.read(document),
				maxAttachmentSize)) {
			String relativePath = rootFolder.toPath().relativize(file.toPath()).toString();
			FileHostingEntityInfo info = new FileHostingEntityInfo(relativePath, context.getSubject());
			store.create(info);

			getNodeClient().writeFile(file.getAbsolutePath(), readInputStream);
			if (readInputStream.exception != null) {
				throw readInputStream.exception;
			}
		} catch (FileSizeExceededException e) {
			logger.warn("Cannot write file. File {} exceeds max file size", file.getAbsolutePath());
			cleanupFile(context, path);
			throw new ServerFault(e.getMessage(), ErrorCode.ENTITY_TOO_LARGE);
		} catch (Exception e) {
			logger.warn("Cannot write file {}:{}", file.getAbsolutePath(), e.getMessage());
			cleanupFile(context, path);
			throw new ServerFault(e);
		}
	}

	private void cleanupFile(SecurityContext context, String path) {
		try {
			delete(context, path);
		} catch (Exception e1) {
			// ignore, cannot delete partially written file
		}
	}

	@Override
	public void delete(SecurityContext context, String path) throws ServerFault {
		File filepath = createFilePath(path, context, true);
		logger.info(String.format("Deleting file %s", filepath.getAbsolutePath()));
		getNodeClient().deleteFile(filepath.getAbsolutePath());
	}

	public int cleanup(int retentionTimeInDays, String domainUid) throws Exception {
		List<FileHostingEntityInfo> paths = store.deleteExpiredFiles(retentionTimeInDays, domainUid);
		for (FileHostingEntityInfo entity : paths) {
			String path = entity.path;
			File sharedFile = new File(rootFolder, path);
			logger.info("Deleting obsolete shared file {}", sharedFile);
			getNodeClient().deleteFile(sharedFile.getAbsolutePath());
		}
		return paths.size();
	}

	private File createFilePath(String path, SecurityContext context, boolean checkExistence) throws ServerFault {
		File file = getFilePath(path, context);
		if (checkExistence && (!fileExists(file))) {
			throw new ServerFault(String.format("Cannot create file path %s. File does not exist or is not a file",
					file.getAbsolutePath()));
		}
		return file;
	}

	private Stream getFileStream(File file) throws ServerFault {
		if (file.exists()) {
			return VertxStream.stream(
					VertxPlatform.getVertx().fileSystem().openBlocking(file.getAbsolutePath(), new OpenOptions()));
		} else {
			InputStream openStream = getNodeClient().openStream(file.getAbsolutePath());
			return VertxStream.stream(new InputReadStream(openStream));
		}
	}

	private File getFilePath(String path, SecurityContext context) {
		String userPath = getUserPath(context);
		File filepath = new File(rootFolder, userPath + path);
		return filepath;
	}

	private String getUserPath(SecurityContext context) {
		if (StringUtils.isBlank(context.getSubject()) || StringUtils.isBlank(context.getContainerUid())) {
			return "global/_others/anonymous/";
		}
		String subject = context.getSubject().toLowerCase();
		for (char c : subject.toCharArray()) {
			if ((c >= 48 && c <= 57) || (c >= 97 && c <= 122)) {
				return String.format("%s/%s/%s/", context.getContainerUid(), String.valueOf(c), context.getSubject());
			}
		}
		return String.format("%s/%s/%s/", context.getContainerUid(), "_others/", context.getSubject());
	}

	private Date getDateFromIsoString(SecurityContext context, String iso8601) throws ServerFault {
		if (StringUtils.isBlank(iso8601)) {
			return getDefaultExpiration(context).getTime();
		}
		try {
			return new Date(BmDateTimeWrapper.toTimestamp(iso8601, TimeZone.getDefault().getID()));
		} catch (Exception e) {
			logger.warn("Cannot parse ISO-8601 string {}:{}", iso8601, e.getMessage());
			return null;
		}
	}

	private Calendar getDefaultExpiration(SecurityContext context) throws ServerFault {
		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_YEAR, getConfiguration(context).retentionTime);
		return cal;
	}

	private boolean fileExists(File file) throws ServerFault {
		String folder = file.getParent();
		String name = file.getName();
		return !getNodeClient().listFiles(folder, name).isEmpty();
	}

	private INodeClient getNodeClient() throws ServerFault {
		if (null == this.nodeClient) {
			LocatorClient lc = new LocatorClient();
			String ip = lc.locateHost("filehosting/data", "admin0@global.virt");
			this.nodeClient = NodeActivator.get(ip);
		}

		return this.nodeClient;
	}

	private Configuration getConfiguration(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IFileHosting.class, context.getContainerUid()).getConfiguration();
	}

	@Override
	public FileHostingInfo info(SecurityContext context) throws ServerFault {
		FileHostingInfo info = new FileHostingInfo();
		info.info = "BlueMind FileHosting";
		return info;
	}

	@Override
	public boolean isDefaultImplementation() {
		return true;
	}

}