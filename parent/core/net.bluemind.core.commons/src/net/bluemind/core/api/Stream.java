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
package net.bluemind.core.api;

import java.util.Optional;

/**
 * Marker interface used to model binary streams
 *
 */
public interface Stream {

	/**
	 * A non empty {@link java.util.Optional} will alter the Content-Type header
	 * when the {@link Stream} is sent over http
	 * 
	 * @return an optional charset name (eg. UTF-8)
	 */
	default Optional<String> charset() {
		return Optional.empty();
	}

	/**
	 * A non empty {@link java.util.Optional} will alter the Content-Type header
	 * when the {@link Stream} is sent over http
	 * 
	 * @return an optional mime type (eg. text/html)
	 */
	default Optional<String> mime() {
		return Optional.empty();
	}

	/**
	 * A non empty {@link java.util.Optional} will add a content-disposition
	 * filename header when the {@link Stream} is sent over http. Only ascii chars
	 * will be preserved when the filename is sent.
	 * 
	 * @return an optional filename
	 */
	default Optional<String> fileName() {
		return Optional.empty();
	}

}
