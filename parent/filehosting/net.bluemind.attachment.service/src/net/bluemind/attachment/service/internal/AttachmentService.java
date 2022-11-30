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
package net.bluemind.attachment.service.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.attachment.api.Configuration;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.GlobalSettingsKeys;

public class AttachmentService implements IAttachment {

	private final SecurityContext securityContext;
	private final String domainUid;
	public static final String FOLDER = "Attachments/";
	private final IFileHosting service;

	private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);

	public AttachmentService(SecurityContext securityContext, String domainUid) {
		this.securityContext = securityContext;
		this.domainUid = domainUid;
		this.service = getFileHostingService();
	}

	@Override
	public AttachedFile share(String name, Stream document) {
		String path = findName(service, name);
		service.store(path, document);

		return finishSharing(path, name);
	}

	@Override
	public AttachedFile shareDedup(String extension, Stream document) {
		String ext = extension;
		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		File tmp = null;
		try {
			tmp = File.createTempFile("dedup", "." + ext);
			GenericStream.streamToFile(document, tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			String hash = Files.asByteSource(tmp).hash(Hashing.murmur3_128()).toString();
			String path = FOLDER + hash + "." + ext;
			if (!service.exists(path)) {
				AsyncFile readStream = VertxPlatform.getVertx().fileSystem().openBlocking(tmp.getAbsolutePath(),
						new OpenOptions().setRead(true));
				Stream asStream = VertxStream.stream(readStream);
				service.store(path, asStream);
			}

			return finishSharing(path, hash + "." + ext);
		} catch (IOException e) {
			throw new ServerFault(e);
		} finally {
			if (tmp != null) {
				tmp.delete();// NOSONAR
			}
		}
	}

	private AttachedFile finishSharing(String path, String name) {
		Calendar expiration = getDefaultExpiration();

		FileHostingPublicLink publicLink = service.share(path, -1,
				BmDateTimeWrapper.toIso8601(expiration.getTimeInMillis(), "UTC"));

		AttachedFile attachedFile = new AttachedFile();
		attachedFile.name = name;
		attachedFile.publicUrl = publicLink.url;
		attachedFile.expirationDate = publicLink.expirationDate;

		return attachedFile;
	}

	@Override
	public void unShare(String url) {
		if (securityContext.isAnonymous()) {
			throw new ServerFault("Login needed to use Attachment service", ErrorCode.PERMISSION_DENIED);
		}

		service.unShare(url);
	}

	@Override
	public Configuration getConfiguration() {
		if (securityContext.isAnonymous()) {
			throw new ServerFault("Login needed to use Attachment service", ErrorCode.PERMISSION_DENIED);
		}

		IDomainSettings domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> values = domainSettings.get();
		Configuration config = new Configuration();
		config.autoDetachmentLimit = longValue(values, GlobalSettingsKeys.mail_autoDetachmentLimit.name(), 0);
		config.maxFilesize = longValue(values, GlobalSettingsKeys.filehosting_max_filesize.name(), 0);
		config.retentionTime = longValue(values, GlobalSettingsKeys.filehosting_retention.name(), 365).intValue();
		return config;
	}

	private Long longValue(Map<String, String> map, String key, long defaultValue) {
		String value = map.get(key);
		if (value == null) {
			return defaultValue;
		} else {
			return Long.valueOf(value);
		}
	}

	private Calendar getDefaultExpiration() {
		Calendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_YEAR, getConfiguration().retentionTime);
		return cal;
	}

	private String findName(IFileHosting service, String name) {
		name = Paths.get(name).getFileName().toString();

		boolean exists = service.exists(FOLDER + name);
		if (!exists) {
			return FOLDER + name;
		} else {
			return FOLDER + extendFilename(name, "" + System.currentTimeMillis());
		}

	}

	private String extendFilename(String name, String suffix) {
		int ext = name.lastIndexOf('.');
		if (ext == -1) {
			return name + "_" + suffix;
		} else {
			return name.substring(0, ext) + "_" + suffix + name.substring(ext);
		}
	}

	private IFileHosting getFileHostingService() {
		try {
			return ServerSideServiceProvider.getProvider(securityContext).instance(IFileHosting.class, domainUid);
		} catch (ServerFault e) {
			logger.debug("Cannot load filehosting service", e);
			return null;
		}
	}

}
