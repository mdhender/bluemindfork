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
package net.bluemind.filehosting.service.internal;

import static net.bluemind.filehosting.service.internal.PathValidator.validate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.filehosting.api.Configuration;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingInfo.Type;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;

public class FileHostingService implements IFileHosting {
	public final List<IFileHostingService> delegates;
	private final SecurityContext context;
	private final String domainUid;

	public FileHostingService(SecurityContext context, String domainUid) {
		this.context = context;
		this.delegates = searchExtensionPoints();
		this.domainUid = domainUid;
	}

	protected List<IFileHostingService> searchExtensionPoints() {
		RunnableExtensionLoader<IFileHostingService> epLoader = new RunnableExtensionLoader<>();
		List<IFileHostingService> extensions = epLoader.loadExtensions("net.bluemind.filehosting", "service", "service",
				"api");
		Collections.sort(extensions,
				(o1, o2) -> o1.isDefaultImplementation() ? 1 : o2.isDefaultImplementation() ? -1 : 0);

		return extensions;
	}

	@Override
	public List<FileHostingItem> list(String path) throws ServerFault {
		validate(path);
		return delegate(context).list(context, path);
	}

	@Override
	public List<FileHostingItem> find(String query) throws ServerFault {
		return delegate(context).find(context, query);
	}

	@Override
	public Stream get(String path) throws ServerFault {
		validate(path);
		return delegate(context).get(context, path);
	}

	@Override
	public FileHostingItem getComplete(String uid) throws ServerFault {
		return delegate(context).getComplete(context, uid);
	}

	@Override
	public Stream getSharedFile(String uid) throws ServerFault {
		return delegate(context).getSharedFile(context, uid);
	}

	@Override
	public FileHostingPublicLink share(String path, Integer downloadLimit, String expirationDate) throws ServerFault {
		validate(path);
		return delegate(context).share(context, path, downloadLimit, expirationDate);
	}

	@Override
	public void unShare(String url) throws ServerFault {
		delegate(context).unShare(context, url);
	}

	@Override
	public void store(String path, Stream document) throws ServerFault {
		validate(path);
		delegate(context).store(context, path, document);
	}

	@Override
	public void delete(String path) throws ServerFault {
		validate(path);
		delegate(context).delete(context, path);
	}

	@Override
	public boolean exists(String path) throws ServerFault {
		validate(path);
		return delegate(context).exists(context, path);
	}

	@Override
	public FileHostingInfo info() throws ServerFault {
		FileHostingInfo info = new FileHostingInfo();
		if (delegates.isEmpty()) {
			info.present = false;
			return info;
		}
		int externalSystems = delegates.stream().map(d -> d.info(context).type)
				.filter(t -> t == null || t == Type.EXTERNAL).collect(Collectors.toList()).size();
		info.type = externalSystems > 0 ? Type.EXTERNAL : Type.INTERNAL;
		info.info = delegates.stream().map(d -> d.info(context).info).reduce("",
				(sum, infoString) -> sum.concat(infoString).concat("\n"));
		info.present = true;
		return info;
	}

	@Override
	public Configuration getConfiguration() throws ServerFault {
		IGlobalSettings settingsGlobal = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		Map<String, String> values = settingsGlobal.get();

		IDomainSettings settingsDomain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> valuesDomain = settingsDomain.get();

		values.putAll(valuesDomain);

		Configuration config = new Configuration();
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

	private IFileHostingService delegate(SecurityContext ctx) {
		if (ctx == null && !delegates.isEmpty()) {
			return delegates.get(0);
		}
		for (IFileHostingService impl : delegates) {
			if (impl.supports(ctx)) {
				return impl;
			}
		}
		throw new ServerFault("filehosting service is not available");
	}

}
