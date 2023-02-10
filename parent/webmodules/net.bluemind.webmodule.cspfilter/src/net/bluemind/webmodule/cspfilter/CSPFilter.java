/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.webmodule.cspfilter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.SecurityConfig;
import net.bluemind.webmodule.server.WebserverConfiguration;
import net.bluemind.webmodule.server.forward.ForwardedLocation;

public class CSPFilter implements IWebFilter {
	private static final Logger logger = LoggerFactory.getLogger(CSPFilter.class);

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request, WebserverConfiguration conf) {

		if (needCspHeaders(request, conf)) {
			request.response().putHeader("Content-Security-Policy",
					"connect-src 'self' ws: wss: https: blob:; default-src 'self' ws: wss: blob: 'unsafe-inline' 'unsafe-eval'; img-src * data: blob: ");

			request.response().putHeader("Feature-Policy",
					"accelerometer 'none'; ambient-light-sensor 'none'; autoplay 'self'; battery 'none';"
							+ " camera 'none'; display-capture 'none'; document-domain 'none'; encrypted-media 'none';"
							+ " execution-while-not-rendered 'self'; execution-while-out-of-viewport 'self';"
							+ " fullscreen 'self'; geolocation 'none'; gyroscope 'none'; layout-animations 'none'; layout-animations 'none';"
							+ " layout-animations 'none'; legacy-image-formats 'none'; magnetometer 'none'; microphone 'none';"
							+ " midi 'none'; navigation-override 'none'; oversized-images 'none'; payment 'none'; picture-in-picture 'none';"
							+ " publickey-credentials 'none'; sync-xhr 'none'; usb 'none'; vr 'none'; wake-lock 'none'; xr-spatial-tracking 'none'; ");
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("No CSP Headers for {}", request.path());
			}
		}

		return CompletableFuture.completedFuture(request);
	}

	private boolean needCspHeaders(HttpServerRequest request, WebserverConfiguration conf) {
		if (!SecurityConfig.cspHeader) {
			return false;
		}

		Optional<ForwardedLocation> forwardedLocation = conf.getForwardedLocations().stream()
				.filter(fl -> request.path().startsWith(fl.getPathPrefix())).findFirst();
		if (forwardedLocation.isPresent()) {
			ForwardedLocation fl = forwardedLocation.get();
			return fl.cspEnabled();
		}

		return true;
	}

}
