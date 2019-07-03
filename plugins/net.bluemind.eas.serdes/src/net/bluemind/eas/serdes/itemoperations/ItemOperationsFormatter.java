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
package net.bluemind.eas.serdes.itemoperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.AirSyncBaseRequest.BodyPreference;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.EmptyFolderContents;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Fetch;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Move;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Response;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Status;
import net.bluemind.eas.dto.itemoperations.ResponseStyle;
import net.bluemind.eas.serdes.AsyncBuildHelper;
import net.bluemind.eas.serdes.AsyncBuildHelper.IBuildOperation;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.base.AppDataFormatter;
import net.bluemind.eas.serdes.base.IBodyOutput;
import net.bluemind.eas.serdes.base.InlineBodyOutput;

public class ItemOperationsFormatter implements IEasResponseFormatter<ItemOperationsResponse> {

	private static final Logger logger = LoggerFactory.getLogger(ItemOperationsFormatter.class);

	public void format(IResponseBuilder builder, final double protocolVersion, ItemOperationsResponse response,
			final Callback<Void> completion) {
		IResponseBuilder b = builder;
		b.start(NamespaceMapping.ItemOperations);
		b.text("Status", response.status.xmlValue());

		IBodyOutput tmp = null;
		if (response.style == ResponseStyle.Inline) {
			tmp = new InlineBodyOutput();
		} else {
			tmp = new MultipartBodyOutput();
		}
		final IBodyOutput output = tmp;
		b.container(NamespaceMapping.ItemOperations, "Response");
		IBuildOperation<Response, IResponseBuilder> buildOp = new IBuildOperation<ItemOperationsResponse.Response, IResponseBuilder>() {

			@Override
			public void beforeAsync(IResponseBuilder b, Response item, Callback<IResponseBuilder> forAsync) {

				if (item instanceof ItemOperationsResponse.Move) {
					appendMove(b, (ItemOperationsResponse.Move) item, forAsync);
				} else if (item instanceof ItemOperationsResponse.EmptyFolderContents) {
					appendEmptyFolderContents(b, (ItemOperationsResponse.EmptyFolderContents) item, forAsync);
				} else if (item instanceof ItemOperationsResponse.Fetch) {
					appendFetch(b, protocolVersion, output, (ItemOperationsResponse.Fetch) item, forAsync);
				}

			}

			@Override
			public void afterAsync(IResponseBuilder b, Response t) {
				// do nothing
			}
		};
		Callback<IResponseBuilder> afterBuild = new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder b) {
				b.endContainer(); // Response
				logger.debug("After build");
				b.end(completion);
			}

		};
		AsyncBuildHelper<Response, IResponseBuilder> buildHelper = new AsyncBuildHelper<>(response.responses.iterator(),
				buildOp, afterBuild);
		buildHelper.build(builder);
	}

	private void appendMove(IResponseBuilder b, Move item, Callback<IResponseBuilder> cb) {
		b.container(NamespaceMapping.ItemOperations, "Move");
		b.text("Status", item.status.xmlValue());
		b.text("ConversationId", item.conversationId);
		b.endContainer();
		cb.onResult(b);
	}

	private void appendEmptyFolderContents(IResponseBuilder b, EmptyFolderContents item,
			Callback<IResponseBuilder> cb) {
		b.container("EmptyFolderContents");
		b.text("Status", item.status.xmlValue());
		b.text(NamespaceMapping.Sync, "CollectionId", item.collectionId);
		b.endContainer();
		cb.onResult(b);
	}

	private void appendFetch(final IResponseBuilder b, double protocolVersion, IBodyOutput output, final Fetch item,
			final Callback<IResponseBuilder> done) {
		b.container(NamespaceMapping.ItemOperations, "Fetch");

		b.text("Status", item.status.xmlValue());
		if (item.collectionId != null) {
			b.text(NamespaceMapping.Sync, "CollectionId", item.collectionId);
		}
		if (item.serverId != null) {
			b.text(NamespaceMapping.Sync, "ServerId", item.serverId);
		}
		if (item.longId != null) {
			b.text(NamespaceMapping.Search, "LongId", item.longId);
		}
		if (item.dataClass != null) {
			b.text(NamespaceMapping.Sync, "Class", item.dataClass);
		}
		if (item.linkId != null) {
			b.text(NamespaceMapping.ItemOperations, "LinkId", item.linkId);
		}
		if (item.fileReference != null) {
			b.text(NamespaceMapping.AirSyncBase, "FileReference", item.fileReference);
		}

		if (item.status != Status.Success) {
			b.endContainer();
			done.onResult(b);
			return;
		}
		b.container(NamespaceMapping.ItemOperations, "Properties");
		AppDataFormatter adf = new AppDataFormatter(output);
		adf.append(b, protocolVersion, item.properties, new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder data) {
				// BM-10562 fetch is not always on an attachment but sometime
				// for the mail body, eg. fetch by longId
				String contentType = "application/octet-stream";
				if (item.properties.metadata.attachment != null) {
					contentType = item.properties.metadata.attachment.contentType;
				} else if (item.properties.body.query != null) {
					BodyOptions query = item.properties.body.query;
					if (query.bodyPrefs != null && !query.bodyPrefs.isEmpty()) {
						BodyPreference pref = query.bodyPrefs.get(0);
						if (pref.type != null) {
							switch (pref.type) {
							case HTML:
								contentType = "text/html";
								break;
							case MIME:
								contentType = "message/rfc822";
								break;
							case PlainText:
								contentType = "text/plain";
								break;
							default:
								logger.warn("unsupported type {}", pref.type);
								break;
							}
						}
					}
					logger.info("Getting mime from body preferences => {}", contentType);
				} else {
					logger.warn("Using default content type of application/octet-stream");
				}
				b.text(NamespaceMapping.AirSyncBase, "ContentType", contentType);
				data.endContainer(); // Properties
				data.endContainer(); // Fetch
				done.onResult(data);
			}
		});
	}

}