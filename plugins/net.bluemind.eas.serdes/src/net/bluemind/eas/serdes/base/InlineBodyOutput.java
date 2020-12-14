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
package net.bluemind.eas.serdes.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.serdes.IResponseBuilder;

public class InlineBodyOutput implements IBodyOutput {

	private static final Logger logger = LoggerFactory.getLogger(InlineBodyOutput.class);

	public InlineBodyOutput() {
	}

	@Override
	public void appendBody(final IResponseBuilder builder, final double protocolVersion, AppData ad,
			final Callback<IResponseBuilder> done) {

		if (ad.body == LazyLoaded.NOOP) {
			done.onResult(builder);
			return;
		}

		if (ad.body != null) {
			ad.body.load(new Callback<AirSyncBaseResponse>() {

				@Override
				public void onResult(AirSyncBaseResponse body) {
					if (body == null) {
						if (ad.type == ItemDataType.EMAIL) {
							logger.error("Missing body for inline fetch");
						}
						done.onResult(builder);
					} else {
						AirSyncBaseResponseFormatter formatter = new AirSyncBaseResponseFormatter();
						formatter.append(builder, protocolVersion, body, done);
					}
				}
			});
		} else {
			logger.warn("Missing body loader in AppData");
			done.onResult(builder);
		}
	}

	@Override
	public void appendAttachment(final IResponseBuilder builder, double protocolVersion, final AppData ad,
			final Callback<IResponseBuilder> done) {

		if (ad.body != null) {
			ad.body.load(new Callback<AirSyncBaseResponse>() {

				@Override
				public void onResult(final AirSyncBaseResponse body) {
					if (body == null) {
						logger.warn("NO BODY FOR APPEND ATTACHMENT IN INLINE FETCH");
						done.onResult(builder);
					} else {
						logger.info("Streaming {} attachment.", body.contentType);

						// BM-9841
						if (ad.options != null && ad.options.range != null) {
							int len = (int) body.body.data.size();
							builder.text(NamespaceMapping.ItemOperations, "Range", "0-" + (len - 1));
							builder.text(NamespaceMapping.ItemOperations, "Total", Integer.toString(len));
						}

						builder.base64(NamespaceMapping.ItemOperations, "Data", body.body.data, done);
					}
				}
			});
		} else {
			if (ad.type == ItemDataType.EMAIL) {
				logger.warn("Missing body loader in AppData");
			}
			done.onResult(builder);
		}
	}

}
