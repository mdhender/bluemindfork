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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.rest.http;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.core.api.AsyncHandler;

public class CachingLocator {

	private static final Logger logger = LoggerFactory.getLogger(CachingLocator.class);

	private static class CacheLocatorImpl implements ILocator {
		private final Cache<String, String[]> resultsCache = CacheBuilder.newBuilder()
				.expireAfterWrite(2, TimeUnit.MINUTES).build();
		private final ILocator delegate;

		public CacheLocatorImpl(ILocator delegate) {
			this.delegate = delegate;
		}

		@Override
		public void locate(String service, AsyncHandler<String[]> asyncHandler) {
			String[] cachedValues = resultsCache.getIfPresent(service);
			if (cachedValues != null) {
				logger.debug("Using cache value {}", (Object) cachedValues);
				asyncHandler.success(cachedValues);
			} else {
				delegate.locate(service, new AsyncHandler<String[]>() {

					@Override
					public void success(String[] value) {
						resultsCache.put(service, value);
						asyncHandler.success(value);
					}

					@Override
					public void failure(Throwable e) {
						asyncHandler.failure(e);
					}

				});
			}
		}
	}

	public static ILocator addCache(ILocator delegate) {
		if (delegate instanceof CacheLocatorImpl) {
			return delegate;
		}
		return new CacheLocatorImpl(delegate);
	}

}
