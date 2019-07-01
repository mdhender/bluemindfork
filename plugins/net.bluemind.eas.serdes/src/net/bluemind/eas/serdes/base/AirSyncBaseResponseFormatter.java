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
import net.bluemind.eas.dto.base.AirSyncBaseResponse.Attachment;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.serdes.IEasFragmentFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class AirSyncBaseResponseFormatter implements IEasFragmentFormatter<AirSyncBaseResponse> {

	private static final Logger logger = LoggerFactory.getLogger(AirSyncBaseResponseFormatter.class);

	public void append(IResponseBuilder builder, double protocolVersion, final AirSyncBaseResponse airSyncBase,
			final Callback<IResponseBuilder> completion) {

		if (airSyncBase.attachments != null && !airSyncBase.attachments.isEmpty()) {
			builder.container(NamespaceMapping.AirSyncBase, "Attachments");
			for (Attachment a : airSyncBase.attachments) {
				builder.container(NamespaceMapping.AirSyncBase, "Attachment");
				if (a.displayName != null) {
					builder.text("DisplayName", a.displayName);
				}

				if (a.contentId != null) {
					builder.text("ContentId", a.contentId);
				}

				builder.text("IsInline", a.isInline ? "1" : "0");

				if (a.fileReference != null) {
					builder.text("FileReference", a.fileReference);
				}
				if (a.method != null) {
					builder.text("AttMethod", a.method.xmlValue());
				}

				if (a.estimateDataSize != null) {
					builder.text("EstimatedDataSize", a.estimateDataSize.toString());
				}
				builder.endContainer(); // Attachment
			}
			builder.endContainer(); // Attachments
		}

		if (airSyncBase.body != null) {
			builder.container(NamespaceMapping.AirSyncBase, "Body");

			if (airSyncBase.body.type != null) {
				builder.text("Type", airSyncBase.body.type.xmlValue());
			}
			if (airSyncBase.body.estimatedDataSize != null) {
				builder.text("EstimatedDataSize", airSyncBase.body.estimatedDataSize.toString());
			}
			if (airSyncBase.body.truncated != null) {
				builder.text("Truncated", airSyncBase.body.truncated ? "1" : "0");
			}
			if (airSyncBase.body.data != null && airSyncBase.body.data.size() > 0) {
				builder.stream("Data", airSyncBase.body.data, new Callback<IResponseBuilder>() {

					@Override
					public void onResult(IResponseBuilder b) {
						b.endContainer();
						afterBody(b, airSyncBase, completion);
					}
				});
			} else {
				builder.endContainer();
				afterBody(builder, airSyncBase, completion);
			}
		} else {
			afterBody(builder, airSyncBase, completion);
		}

	}

	private void afterBody(IResponseBuilder builder, AirSyncBaseResponse airSyncBase,
			Callback<IResponseBuilder> completion) {
		if (airSyncBase.bodyPart != null) {
			logger.warn("bodyPart is not supported");
		}
		if (airSyncBase.nativeBodyType != null) {
			builder.text(NamespaceMapping.AirSyncBase, "NativeBodyType", airSyncBase.nativeBodyType.xmlValue());
		}

		if (airSyncBase.contentType != null) {
			builder.text(NamespaceMapping.AirSyncBase, "ContentType", airSyncBase.contentType);
		}

		completion.onResult(builder);
	}

}
