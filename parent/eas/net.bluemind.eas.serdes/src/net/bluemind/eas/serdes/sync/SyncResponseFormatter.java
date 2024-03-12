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
package net.bluemind.eas.serdes.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.sync.CollectionSyncResponse;
import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerChange;
import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerChange.ChangeType;
import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerResponse;
import net.bluemind.eas.dto.sync.SyncResponse;
import net.bluemind.eas.dto.sync.SyncStatus;
import net.bluemind.eas.serdes.AsyncBuildHelper;
import net.bluemind.eas.serdes.AsyncBuildHelper.IBuildOperation;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.base.AppDataFormatter;

public class SyncResponseFormatter implements IEasResponseFormatter<SyncResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SyncResponseFormatter.class);

	public void format(IResponseBuilder builder, double protocolVersion, SyncResponse response,
			final Callback<Void> completion) {
		builder.start(NamespaceMapping.SYNC);
		if (response.status == SyncStatus.OK) {
			appendSyncOk(builder, protocolVersion, response,
					responseBuilder -> responseBuilder.endContainer().end(completion));
		} else {
			builder.text(NamespaceMapping.SYNC, "Status", response.status.asXmlValue());
			if (response.limit != null) {
				builder.text("Limit", Integer.toString(response.limit));
			}
			builder.end(completion);
		}
	}

	private void appendSyncOk(IResponseBuilder b, final double protocolVersion, SyncResponse response,
			final Callback<IResponseBuilder> onDoc) {
		b.text(NamespaceMapping.SYNC, "Status", response.status.asXmlValue());
		b.container(NamespaceMapping.SYNC, "Collections");
		IBuildOperation<CollectionSyncResponse, IResponseBuilder> collectionBuild = new IBuildOperation<CollectionSyncResponse, IResponseBuilder>() {

			@Override
			public void beforeAsync(IResponseBuilder b, CollectionSyncResponse csr,
					final Callback<IResponseBuilder> forAsync) {
				b.container(NamespaceMapping.SYNC, "Collection");
				b.text(NamespaceMapping.SYNC, "SyncKey", csr.syncKey);
				b.text(NamespaceMapping.SYNC, "CollectionId", csr.collectionId);
				b.text(NamespaceMapping.SYNC, "Status", csr.status.asXmlValue());

				if (csr.moreAvailable) {
					// LG G2 m'a tuer
					// <MoreAvailable/> on the top because of LG G2
					b.token(NamespaceMapping.SYNC, "MoreAvailable");
				}

				final AppDataFormatter adf = new AppDataFormatter();
				buildResponses(b, protocolVersion, csr, adf, forAsync);
			}

			@Override
			public void afterAsync(IResponseBuilder b, CollectionSyncResponse t) {
				b.endContainer(); // Collection
			}
		};
		AsyncBuildHelper<CollectionSyncResponse, IResponseBuilder> helper = new AsyncBuildHelper<>(
				response.collections.iterator(), collectionBuild, onDoc);
		helper.build(b);
	}

	private void buildCommands(IResponseBuilder b, final double protocolVersion, final CollectionSyncResponse csr,
			final AppDataFormatter adf, final Callback<IResponseBuilder> onDoc) {
		if (csr.commands != null && !csr.commands.isEmpty()) {
			b.container(NamespaceMapping.SYNC, "Commands");

			IBuildOperation<ServerChange, IResponseBuilder> commandBuild = new AsyncBuildHelper.IBuildOperation<ServerChange, IResponseBuilder>() {

				@Override
				public void beforeAsync(IResponseBuilder b, ServerChange srvChange,
						final Callback<IResponseBuilder> forAsync) {
					b.container(NamespaceMapping.SYNC, srvChange.type.xmlValue());
					b.text(NamespaceMapping.SYNC, "ServerId", srvChange.item.toString());
					if (srvChange.data.isPresent()) {
						b.container(NamespaceMapping.SYNC, "ApplicationData");
						AppData appData = srvChange.data.get();
						adf.append(b, protocolVersion, appData, responseBuilder -> {
							responseBuilder.endContainer();
							forAsync.onResult(responseBuilder);

						});
					} else {
						if (srvChange.type != ChangeType.DELETE) {
							logger.warn("Data is missing for {} {}", srvChange.type, srvChange.item);
						}
						forAsync.onResult(b);
					}
				}

				@Override
				public void afterAsync(IResponseBuilder b, ServerChange t) {
					b.endContainer(); // </Add> or </Change> or </Delete>
				}
			};
			AsyncBuildHelper<ServerChange, IResponseBuilder> asyncBuild = new AsyncBuildHelper<>(
					csr.commands.iterator(), commandBuild, responseBuilder -> {
						responseBuilder.endContainer(); // Commands
						onDoc.onResult(responseBuilder);
					});
			asyncBuild.build(b);
		} else {
			onDoc.onResult(b);
		}
	}

	private void buildResponses(IResponseBuilder b, final double protocolVersion, final CollectionSyncResponse csr,
			final AppDataFormatter adf, final Callback<IResponseBuilder> onDoc) {
		if (csr.responses != null && !csr.responses.isEmpty()) {
			b.container(NamespaceMapping.SYNC, "Responses");
			IBuildOperation<ServerResponse, IResponseBuilder> responseBuild = new IBuildOperation<CollectionSyncResponse.ServerResponse, IResponseBuilder>() {

				@Override
				public void beforeAsync(IResponseBuilder b, ServerResponse srvResp,
						final Callback<IResponseBuilder> forAsync) {
					b.container(NamespaceMapping.SYNC, srvResp.operation.xmlValue());
					if (srvResp.clientId != null) {
						b.text("ClientId", srvResp.clientId);
					}
					b.text("ServerId", srvResp.item.toString());
					b.text("Status", srvResp.ackStatus.asXmlValue());
					if (srvResp.fetch.isEmpty()) {
						// no data
						forAsync.onResult(b);
					} else {
						b.container(NamespaceMapping.SYNC, "ApplicationData");
						AppData data = srvResp.fetch.get();
						adf.append(b, protocolVersion, data, responseBuilder -> {
							responseBuilder.endContainer(); // ApplicationData
							forAsync.onResult(responseBuilder);

						});
					}
				}

				@Override
				public void afterAsync(IResponseBuilder b, ServerResponse t) {
					b.endContainer(); // </Add>
				}
			};

			AsyncBuildHelper<ServerResponse, IResponseBuilder> asyncBuild = new AsyncBuildHelper<>(
					csr.responses.iterator(), responseBuild, responseBuilder -> {
						responseBuilder.endContainer(); // Responses
						buildCommands(responseBuilder, protocolVersion, csr, adf, onDoc);
					});
			asyncBuild.build(b);
		} else {
			buildCommands(b, protocolVersion, csr, adf, onDoc);
		}
	}

}
