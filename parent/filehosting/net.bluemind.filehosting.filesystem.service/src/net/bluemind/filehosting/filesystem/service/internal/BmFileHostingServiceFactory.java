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

import java.util.List;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.filehosting.api.Configuration;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.filehosting.api.IInternalBMFileSystem;
import net.bluemind.filehosting.service.export.IInternalFileHostingService;
import net.bluemind.filehosting.service.internal.FileHostingService;

public class BmFileHostingServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInternalBMFileSystem> {

	@Override
	public Class<IInternalBMFileSystem> factoryClass() {
		return IInternalBMFileSystem.class;
	}

	@Override
	public IInternalBMFileSystem instance(BmContext context, String... params) throws ServerFault {
		FileHostingService service = (FileHostingService) ServerSideServiceProvider.getProvider(context)
				.instance(IFileHosting.class, "global.virt");
		IInternalFileHostingService internalService = (IInternalFileHostingService) service
				.delegate(context.getSecurityContext());
		return new IInternalBMFileSystem() {

			@Override
			public Stream getSharedFile(String uid) throws ServerFault {
				return internalService.getSharedFile(context.getSecurityContext(), uid);
			}

			@Override
			public List<String> getShareUidsByPath(String path) throws ServerFault {
				return internalService.getShareUidsByPath(path);
			}

			@Override
			public FileHostingItem getComplete(String uid) throws ServerFault {
				return internalService.getComplete(context.getSecurityContext(), uid);
			}

			@Override
			public Configuration getConfiguration() throws ServerFault {
				return ((FileHostingService) ServerSideServiceProvider.getProvider(context).instance(IFileHosting.class,
						context.getSecurityContext().getContainerUid())).getConfiguration();
			}

			@Override
			public List<FileHostingItem> list(String path) throws ServerFault {
				return internalService.list(context.getSecurityContext(), path);
			}

			@Override
			public List<FileHostingItem> find(String query) throws ServerFault {
				return internalService.find(context.getSecurityContext(), query);
			}

			@Override
			public boolean exists(String path) throws ServerFault {
				return internalService.exists(context.getSecurityContext(), path);
			}

			@Override
			public Stream get(String path) throws ServerFault {
				return internalService.get(context.getSecurityContext(), path);
			}

			@Override
			public FileHostingPublicLink share(String path, Integer downloadLimit, String expirationDate)
					throws ServerFault {
				return internalService.share(context.getSecurityContext(), path, downloadLimit, expirationDate);
			}

			@Override
			public void unShare(String url) throws ServerFault {
				internalService.unShare(context.getSecurityContext(), url);
			}

			@Override
			public void store(String path, Stream document) throws ServerFault {
				internalService.store(context.getSecurityContext(), path, document);
			}

			@Override
			public void delete(String path) throws ServerFault {
				internalService.delete(context.getSecurityContext(), path);
			}

			@Override
			public FileHostingInfo info() throws ServerFault {
				return ((FileHostingService) ServerSideServiceProvider.getProvider(context).instance(IFileHosting.class,
						context.getSecurityContext().getContainerUid())).info();
			}
		};
	}

}
