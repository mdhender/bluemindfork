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
package net.bluemind.eas.backend;

import net.bluemind.eas.dto.base.DisposableByteSource;

public class MSAttachementData {

	private final DisposableByteSource file;
	private final String contentType;

	public MSAttachementData(String contentType, DisposableByteSource file) {
		this.contentType = contentType;
		this.file = file;
	}

	public DisposableByteSource getFile() {
		return file;
	}

	public String getContentType() {
		return contentType;
	}
}
