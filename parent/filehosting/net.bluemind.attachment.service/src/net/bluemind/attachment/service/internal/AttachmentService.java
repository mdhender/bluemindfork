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

import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.attachment.api.Configuration;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.IFileHosting;
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
		Calendar expiration = getDefaultExpiration();

		FileHostingPublicLink publicLink = service.share(path, -1,
				BmDateTimeWrapper.toIso8601(expiration.getTimeInMillis(), "UTC"));

		AttachedFile attachedFile = new AttachedFile();
		attachedFile.name = path.replace(FOLDER, "");
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

		List<FileHostingItem> list = service.list(FOLDER);

		String filename = name;
		for (int i = 1; i < 999; i++) {
			final String f = filename;
			if (list.stream().filter(item -> item.name.equals(f)).count() == 0) {
				return FOLDER + f;
			}
			filename = extendFilename(name, i);
		}
		return UUID.randomUUID().toString();
	}

	private String extendFilename(String name, int i) {
		int ext = name.lastIndexOf('.');
		if (ext == -1) {
			return name + "_" + i;
		} else {
			return name.substring(0, ext) + "_" + i + name.substring(ext);
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
