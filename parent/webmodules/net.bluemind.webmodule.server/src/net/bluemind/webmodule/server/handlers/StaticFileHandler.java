/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bluemind.webmodule.server.handlers;

import java.io.File;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.webmodule.server.WebResource;

/**
 * A Handler implementation specifically for serving HTTP requests from the file
 * system.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author pidster
 * 
 *         PATCHED !
 */
public class StaticFileHandler implements Handler<HttpServerRequest> {

	private static final Logger logger = LoggerFactory.getLogger(StaticFileHandler.class);

	private FileSystem fileSystem;
	private boolean gzipFiles;
	private boolean caching;

	private String webRoot;
	private List<WebResource> resources;
	private String index;

	private final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory = new IdFactory("staticFile", registry, StaticFileHandler.class);

	public StaticFileHandler(Vertx vertx, String webRoot, String index, List<WebResource> resources, boolean gzipFiles,
			boolean caching) {
		super();
		this.webRoot = webRoot;
		this.index = index;
		this.fileSystem = vertx.fileSystem();
		this.resources = resources;
		this.gzipFiles = gzipFiles;
		this.caching = caching;
	}

	@Override
	public void handle(HttpServerRequest req) {
		// browser gzip capability check

		String acceptEncoding = req.headers().get(HttpHeaders.ACCEPT_ENCODING);
		boolean acceptEncodingGzip = acceptEncoding != null && acceptEncoding.contains("gzip");

		if (logger.isDebugEnabled()) {
			logger.debug("trying to resolve [{}], module root [{}]", req.path(), webRoot);
		}
		try {
			String file = req.path().substring(webRoot.equals("/") ? 0 : webRoot.length());

			if ("/".equals(file) || file.length() == 0) {
				// redirect to index
				req.response().headers().add("Location", webRoot + "/" + index);
				req.response().setStatusCode(302);
				req.response().end();
				return;
			}

			file = file.substring(1);
			// index file may also be zipped
			boolean zipped = false;
			File resourceFile = null;
			WebResource source = null;
			for (WebResource r : resources) {
				resourceFile = r.getResource(file);

				if (resourceFile != null) {
					source = r;
					if (gzipFiles && acceptEncodingGzip) {
						File gzipped = r.getResource(file + ".gz");

						if (gzipped != null) {
							resourceFile = gzipped;
							zipped = true;
						}
					}

					// SOURCEMAP
					if (file.endsWith(".js")) {
						File mapFile = r.getResource(file + ".map");
						if (mapFile != null) {
							req.response().headers().add("X-SourceMap", req.path() + ".map");
							req.response().headers().add("SourceMap", req.path() + ".map");
						}
					}
					break;
				}
			}

			String fileName = "unknow";
			if (resourceFile != null) {
				fileName = resourceFile.getAbsolutePath();
			}
			int error = 200;

			if (file.contains("..")) {
				// Prevent accessing files outside webroot
				error = 403;
			} else if (caching && isCached(fileName) && source != null) {
				// TODO MD5 or something for etag?
				String etag = String.format("W/%d-%s", source.getBundleName().hashCode(),
						source.getBundle().getVersion());

				if (req.headers().contains(HttpHeaders.IF_MATCH)) {
					String checkEtags = req.headers().get(HttpHeaders.IF_MATCH);
					if (checkEtags.indexOf(',') > -1) {
						// there may be multiple etags
						boolean matched = false;
						LOOP: for (String checkEtag : checkEtags.split(", *")) {
							if (etag.equals(checkEtag)) {
								matched = true;
								break LOOP;
							}
						}
						if (!matched)
							error = 412;
					}
					// wildcards are allowed
					else if ("*".equals(checkEtags) && !fileSystem.existsBlocking(fileName)) {
						error = 412;
					} else if (etag.equals(checkEtags)) {
						error = 304;
					}
				}

				// either if-none-match or if-modified-since header, then...
				else if (req.headers().contains(HttpHeaders.IF_NONE_MATCH)) {
					String checkEtags = req.headers().get(HttpHeaders.IF_NONE_MATCH);

					// only HEAD or GET are allowed
					if (HttpMethod.HEAD == req.method() || HttpMethod.GET == req.method()) {
						if (checkEtags.indexOf(',') > -1) {
							// there may be multiple etags
							LOOP: for (String checkEtag : checkEtags.split(", *")) {

								if (etag.equals(checkEtag)) {
									error = 304;
									break LOOP;
								}
							}
						}
						// wildcards are allowed
						else if ("*".equals(checkEtags)) {
							error = 304;
						} else if (etag.equals(checkEtags)) {
							error = 304;
						}
					} else {
						sendError(req, 412);
						return;
					}
				}

				setResponseHeader(req, HttpHeaders.ETAG, etag);
			} else {
				req.response().headers().add("Cache-Control", Arrays.<String>asList("no-cache", "must-revalidate"));
				req.response().headers().add("Pragma", "no-cache");
			}

			addMimetype(req, file);
			if (zipped)
				setResponseHeader(req, HttpHeaders.CONTENT_ENCODING, "gzip");
			if (error != 200) {
				sendError(req, error);
			} else {
				if (HttpMethod.HEAD == req.method()) {
					req.response().end();
				} else {
					registry.counter(idFactory.name("requests", "status", "200")).increment();
					req.response().sendFile(fileName, res -> {
						if (res.failed()) {
							req.response().setStatusCode(404).end();
						}
					});
				}
			}

		} catch (Exception e) {
			throw new IllegalStateException("Failed to check file: " + e.getMessage(), e);
		}
	}

	private boolean isCached(String fileName) {
		return !fileName.contains("nocache");
	}

	private void addMimetype(HttpServerRequest req, String file) {
		String mt = URLConnection.guessContentTypeFromName(file);
		if (mt != null) {
			req.response().headers().add("Content-Type", mt);
		}
	}

	private void setResponseHeader(HttpServerRequest req, CharSequence header, String value) {
		req.response().putHeader(header, value);
	}

	private void sendError(HttpServerRequest req, int error) {
		sendError(req, error, "");
	}

	private void sendError(HttpServerRequest req, int error, String message) {
		registry.counter(idFactory.name("requests", "status", Integer.toString(error))).increment();
		req.response().setStatusMessage(message);
		req.response().setStatusCode(error);
		req.response().end();
	}

}
