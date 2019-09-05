/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.filehosting.webdav.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.client.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;

import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.utils.InputReadStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.filehosting.api.Configuration;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.FileType;
import net.bluemind.filehosting.api.IFileHosting;
import net.bluemind.filehosting.api.Metadata;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.filehosting.service.export.SizeLimitedReadStream;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.user.api.UserAccount;

public abstract class WebDavFileHostingService implements IFileHostingService {

	protected static final Logger logger = LoggerFactory.getLogger(WebDavFileHostingService.class);
	private static final long SEARCH_TIMEOUT_MILLIS = 10 * 1000;

	protected abstract ConnectionContext getConnectionContext(SecurityContext context);

	@Override
	public abstract FileHostingInfo info(SecurityContext context);

	@Override
	public abstract FileHostingPublicLink share(SecurityContext context, String path, Integer downloadLimit,
			String expirationDate) throws ServerFault;

	@Override
	public abstract void unShare(SecurityContext context, String url) throws ServerFault;

	@Override
	public boolean supports(SecurityContext context) {
		try {
			ConnectionContext con = getConnectionContext(context);
			return con != null;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public List<FileHostingItem> list(SecurityContext context, String path) throws ServerFault {
		if (!path.isEmpty() && !path.endsWith("/")) {
			path += "/";
		}
		try {
			return listResource(context, path);
		} catch (ServerFault e) {
			if (e.getCause() != null && e.getCause() instanceof HttpResponseException) {
				HttpResponseException ex = (HttpResponseException) e.getCause();

				if (ex.getStatusCode() == 404) {
					return Collections.emptyList();
				}
			}
			throw e;
		}
	}

	private List<FileHostingItem> listResource(SecurityContext context, String path) {
		String encodedPath = sanitizePath(path);

		WebdavContext webdavContext = getWebdavContext(context);
		URL uri;
		try {
			uri = new URL(createUri(encodedPath, webdavContext.connectionContext));
		} catch (MalformedURLException e) {
			throw new ServerFault(e);
		}
		String uriDecoded = URLDecoder.decode(uri.getPath());
		return webdav(() -> {
			logger.info("Listing {}", uri.toString());
			return webdavContext.sardine.list(uri.toString()).stream().filter(dav -> {
				return !uriDecoded.replaceAll("/", "").equals(URLDecoder.decode(dav.getPath()).replaceAll("/", ""));
			}).map(dav -> {
				String itemPath = path + dav.getName();
				List<Metadata> metadata = new ArrayList<>();
				metadata.add(new Metadata("Content-Length", String.valueOf(dav.getContentLength())));
				metadata.add(new Metadata("Content-Type", dav.getContentType()));
				return new FileHostingItem(itemPath, dav.getName(), WebDavFileHostingService.this.getFileType(dav),
						dav.getContentLength(), metadata);
			}).collect(Collectors.toList());
		});
	}

	@Override
	public List<FileHostingItem> find(SecurityContext context, String query) throws ServerFault {
		final List<FileHostingItem> matches = new ArrayList<>();
		final String[] filenameQuery = new String[] { URLEncoder.encode(query), query };

		long timeout = System.currentTimeMillis() + SEARCH_TIMEOUT_MILLIS;
		traverse(context, "", matches, filenameQuery, "/", timeout);
		return matches;
	}

	private void traverse(SecurityContext context, String filepath, List<FileHostingItem> matches,
			String[] filenameQuery, String root, long timeout) throws ServerFault {
		if (System.currentTimeMillis() > timeout) {
			return;
		}
		List<FileHostingItem> listFiles = list(context, filepath);
		for (FileHostingItem fileDescription : listFiles) {
			if (fileDescription.type == FileType.DIRECTORY) {
				traverse(context, fileDescription.path, matches, filenameQuery, root, timeout);
			} else {
				for (String query : filenameQuery) {
					if (fileDescription.name.toLowerCase().indexOf(query) != -1) {
						matches.add(fileDescription);
						break;
					}
				}
			}
		}
	}

	@Override
	public Stream get(SecurityContext context, String path) throws ServerFault {
		String encoded = sanitizePath(path);
		WebdavContext webdavContext = getWebdavContext(context);
		return webdav(() -> {
			InputStream openStream;
			try {
				openStream = webdavContext.sardine.get(createUri(encoded, webdavContext.connectionContext));
			} catch (Exception e) {
				if (e.getMessage().contains("404")) {
					return null;
				}
				throw e;
			}
			return VertxStream.stream(new InputReadStream(openStream));
		});
	}

	@Override
	public void store(SecurityContext context, String path, Stream document) throws ServerFault {
		String encoded = sanitizePath(path);
		WebdavContext webdavContext = getWebdavContext(context);
		long maxAttachmentSize = getConfiguration(context).maxFilesize;
		logger.info(String.format("Storing file to %s", encoded));
		webdav(() -> {
			String davPath = encoded;
			String[] parts = davPath.split("/");
			String check = "";
			for (int i = 0; i < parts.length - 1; i++) {
				check += parts[i] + "/";
				String checkUri = createUri(check, webdavContext.connectionContext);
				if (!webdavContext.sardine.exists(checkUri)) {
					webdavContext.sardine.createDirectory(checkUri);
				}
			}
			SizeLimitedReadStream readInputStream = new SizeLimitedReadStream(VertxStream.read(document),
					maxAttachmentSize);
			String uri = createUri(davPath, webdavContext.connectionContext);
			webdavContext.sardine.put(uri, readInputStream);
			if (readInputStream.exception != null) {
				throw new ServerFault(readInputStream.exception);
			}
			return null;
		});
	}

	@Override
	public void delete(SecurityContext context, String path) throws ServerFault {
		String encoded = sanitizePath(path);
		WebdavContext webdavContext = getWebdavContext(context);
		logger.info("Deleting file %s", path);
		webdav(() -> {
			webdavContext.sardine.delete(createUri(encoded, webdavContext.connectionContext));
			return null;
		});
	}

	@Override
	public FileHostingItem getComplete(SecurityContext context, String uid) throws ServerFault {
		throw new UnsupportedOperationException("Unsupported deprecated API call IFileHosting#getComplete");
	}

	@Override
	public Stream getSharedFile(SecurityContext context, String uid) throws ServerFault {
		throw new UnsupportedOperationException("Shared files are only exposed via public links");
	}

	protected String createUri(String path, ConnectionContext connectionContext) {
		String pattern = path.startsWith("/") ? "%s%s" : "%s/%s";
		return String.format(pattern, connectionContext.baseUrl, path);
	}

	private FileType getFileType(DavResource dav) {
		return dav.isDirectory() ? FileType.DIRECTORY : FileType.FILE;
	}

	private WebdavContext getWebdavContext(SecurityContext context) {
		Sardine sardine = null;
		ConnectionContext con = getConnectionContext(context);
		switch (con.system.authKind) {
		case NONE:
			sardine = TrustAllSardineFactory.begin();
			break;
		case SIMPLE_CREDENTIALS:
		case API_KEY:
			sardine = TrustAllSardineFactory.begin(con.account.login, con.account.credentials);
			sardine.setCredentials(con.account.login, con.account.credentials);
			break;
		}
		return new WebdavContext(con, sardine);
	}

	private <T> T webdav(SardineCall<T> op) {
		try {
			return op.execute();
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	@FunctionalInterface
	private static interface SardineCall<T> {
		public T execute() throws IOException;
	}

	protected static class ConnectionContext {
		public final String baseUrl;
		public final UserAccount account;
		public final ExternalSystem system;

		public ConnectionContext(UserAccount account, ExternalSystem system, String baseUrl) {
			this.account = account;
			this.system = system;
			this.baseUrl = baseUrl;
		}
	}

	private static class WebdavContext {
		public final ConnectionContext connectionContext;
		public final Sardine sardine;

		public WebdavContext(ConnectionContext connectionContext, Sardine sardine) {
			this.connectionContext = connectionContext;
			this.sardine = sardine;
			try {
				String host = new URL(connectionContext.baseUrl).getHost();
				this.sardine.enablePreemptiveAuthentication(host, 80, 443);
			} catch (MalformedURLException e) {
				logger.warn("Cannot set preemptive auth for url {}", connectionContext.baseUrl);
			}
		}
	}

	private String sanitizePath(String path) {
		StringBuilder sb = new StringBuilder();
		for (String splitted : path.split("/")) {
			sb.append(encodeURIComponent(splitted)).append("/");
		}
		String encoded = sb.toString();
		if (encoded.endsWith("/")) {
			encoded = encoded.substring(0, encoded.length() - 1);
		}
		return encoded;
	}

	private String encodeURIComponent(String path) {
		try {
			return URLEncoder.encode(path, "UTF-8").replaceAll("\\+", "%20").replaceAll("\\%21", "!")
					.replaceAll("\\%27", "'").replaceAll("\\%28", "(").replaceAll("\\%29", ")")
					.replaceAll("\\%7E", "~");
		} catch (UnsupportedEncodingException e) {
			return path;
		}
	}

	private Configuration getConfiguration(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IFileHosting.class, context.getContainerUid()).getConfiguration();
	}

}
