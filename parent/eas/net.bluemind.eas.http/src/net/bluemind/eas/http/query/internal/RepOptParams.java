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
package net.bluemind.eas.http.query.internal;

import io.vertx.core.MultiMap;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.http.EasHeaders;

public final class RepOptParams implements OptionalParams {

	private final MultiMap reqParams;
	private final MultiMap headers;

	public RepOptParams(MultiMap reqParams, MultiMap headers) {
		this.headers = headers;
		this.reqParams = reqParams;
	}

	@Override
	public String attachmentName() {
		return reqParams.get("AttachmentName");
	}

	@Override
	public String collectionId() {
		return reqParams.get("CollectionId");
	}

	@Override
	public String collectionName() {
		return reqParams.get("CollectionName");
	}

	@Override
	public String itemId() {
		return reqParams.get("ItemId");
	}

	@Override
	public String longId() {
		return reqParams.get("LongId");
	}

	@Override
	public String parentId() {
		return reqParams.get("ParentId");
	}

	@Override
	public String occurrence() {
		return reqParams.get("Occurrence");
	}

	@Override
	public String saveInSent() {
		return reqParams.get("SaveInSent");
	}

	@Override
	public String acceptEncoding() {
		return headers.get("Accept-Encoding");
	}

	@Override
	public String acceptMultiPart() {
		String fromHeader = headers.get(EasHeaders.Client.ACCEPT_MULTIPART);
		return fromHeader != null ? fromHeader : reqParams.get("AcceptMultiPart");
	}

}
