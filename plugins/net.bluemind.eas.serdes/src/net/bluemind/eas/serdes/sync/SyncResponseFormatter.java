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
		builder.start(NamespaceMapping.Sync);
		Callback<IResponseBuilder> afterAll = new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder data) {
				data.endContainer().end(completion); // end Collections
			}
		};
		if (response.status == SyncStatus.OK) {
			appendSyncOk(builder, protocolVersion, response, afterAll);
		} else {
			builder.text(NamespaceMapping.Sync, "Status", response.status.asXmlValue());
			if (response.limit != null) {
				builder.text("Limit", Integer.toString(response.limit));
			}
			builder.end(completion);
		}
	}

	private void appendSyncOk(IResponseBuilder b, final double protocolVersion, SyncResponse response,
			final Callback<IResponseBuilder> onDoc) {
		b.text(NamespaceMapping.Sync, "Status", response.status.asXmlValue());
		b.container(NamespaceMapping.Sync, "Collections");
		IBuildOperation<CollectionSyncResponse, IResponseBuilder> collectionBuild = new IBuildOperation<CollectionSyncResponse, IResponseBuilder>() {

			@Override
			public void beforeAsync(IResponseBuilder b, CollectionSyncResponse csr,
					final Callback<IResponseBuilder> forAsync) {
				b.container(NamespaceMapping.Sync, "Collection");
				b.text(NamespaceMapping.Sync, "SyncKey", csr.syncKey);
				b.text(NamespaceMapping.Sync, "CollectionId", Integer.toString(csr.collectionId));
				b.text(NamespaceMapping.Sync, "Status", csr.status.asXmlValue());

				if (csr.moreAvailable) {
					// LG G2 m'a tuer
					// <MoreAvailable/> on the top because of LG G2
					b.token(NamespaceMapping.Sync, "MoreAvailable");
				}

				int expectedLoads = 0;
				if (csr.commands != null && !csr.commands.isEmpty()) {
					expectedLoads += csr.commands.size();
				}
				if (csr.responses != null && !csr.responses.isEmpty()) {
					expectedLoads += csr.responses.size();
				}

				if (expectedLoads > 0) {
					logger.info("{} async load(s) expected.", expectedLoads);
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
			b.container(NamespaceMapping.Sync, "Commands");

			Callback<IResponseBuilder> afterBuild = new Callback<IResponseBuilder>() {
				@Override
				public void onResult(IResponseBuilder data) {
					data.endContainer(); // Commands
					onDoc.onResult(data);
				}
			};

			IBuildOperation<ServerChange, IResponseBuilder> commandBuild = new AsyncBuildHelper.IBuildOperation<ServerChange, IResponseBuilder>() {

				@Override
				public void beforeAsync(IResponseBuilder b, ServerChange srvChange,
						final Callback<IResponseBuilder> forAsync) {
					b.container(NamespaceMapping.Sync, srvChange.type.name());
					b.text(NamespaceMapping.Sync, "ServerId", srvChange.item.toString());
					if (srvChange.data.isPresent()) {
						b.container(NamespaceMapping.Sync, "ApplicationData");
						AppData data = srvChange.data.get();

						adf.append(b, protocolVersion, data, new Callback<IResponseBuilder>() {

							@Override
							public void onResult(IResponseBuilder data) {
								data.endContainer();
								forAsync.onResult(data);
							}
						});
					} else {
						if (srvChange.type != ChangeType.Delete) {
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
					csr.commands.iterator(), commandBuild, afterBuild);
			asyncBuild.build(b);
		} else {
			onDoc.onResult(b);
		}
	}

	private void buildResponses(IResponseBuilder b, final double protocolVersion, final CollectionSyncResponse csr,
			final AppDataFormatter adf, final Callback<IResponseBuilder> onDoc) {
		if (csr.responses != null && !csr.responses.isEmpty()) {
			b.container(NamespaceMapping.Sync, "Responses");

			Callback<IResponseBuilder> afterBuild = new Callback<IResponseBuilder>() {

				@Override
				public void onResult(IResponseBuilder data) {
					data.endContainer(); // Responses
					buildCommands(data, protocolVersion, csr, adf, onDoc);
				}
			};

			IBuildOperation<ServerResponse, IResponseBuilder> responseBuild = new IBuildOperation<CollectionSyncResponse.ServerResponse, IResponseBuilder>() {

				@Override
				public void beforeAsync(IResponseBuilder b, ServerResponse srvResp,
						final Callback<IResponseBuilder> forAsync) {
					b.container(NamespaceMapping.Sync, srvResp.operation.name());
					if (srvResp.clientId != null) {
						b.text("ClientId", srvResp.clientId);
					}
					b.text("ServerId", srvResp.item.toString());
					b.text("Status", srvResp.ackStatus.asXmlValue());
					if (srvResp.fetch == null || !srvResp.fetch.isPresent()) {
						logger.info("Data is missing {}", srvResp);
						forAsync.onResult(b);
					} else {
						b.container(NamespaceMapping.Sync, "ApplicationData");
						AppData data = srvResp.fetch.get();
						adf.append(b, protocolVersion, data, new Callback<IResponseBuilder>() {

							@Override
							public void onResult(IResponseBuilder data) {
								data.endContainer(); // ApplicationData
								forAsync.onResult(data);
							}
						});
					}
				}

				@Override
				public void afterAsync(IResponseBuilder b, ServerResponse t) {
					b.endContainer(); // </Add>
				}
			};

			AsyncBuildHelper<ServerResponse, IResponseBuilder> asyncBuild = new AsyncBuildHelper<>(
					csr.responses.iterator(), responseBuild, afterBuild);
			asyncBuild.build(b);
		} else {
			buildCommands(b, protocolVersion, csr, adf, onDoc);
		}
	}

}
