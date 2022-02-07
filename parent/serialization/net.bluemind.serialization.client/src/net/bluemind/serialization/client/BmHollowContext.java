package net.bluemind.serialization.client;

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.HollowConsumer.Blob;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;

import net.bluemind.common.hollow.IAnnouncementWatcher;

public class BmHollowContext {

	public HollowContext create(String set, String subset) {
		IAnnouncementWatcher announcementWatcher = new BmAnnouncementWatcher(set, subset);
		BlobRetriever blobRetriever = new BmBlobRetriever(set, subset);
		return new HollowContext(blobRetriever, announcementWatcher);
	}

	public class BmBlobRetriever implements BlobRetriever {
		private final String set;
		private final String subset;

		public BmBlobRetriever(String set, String subset) {
			this.set = set;
			this.subset = subset;
		}

		private Path getPath(BmHollowClient.Type type, long desiredVersion) throws IOException {
			return Files.createTempFile(type.name(), ".v" + desiredVersion);
		}

		@Override
		public Blob retrieveSnapshotBlob(long desiredVersion) {
			try {
				Path file = getPath(BmHollowClient.Type.snapshot, desiredVersion);
				try (BmHollowClient client = new BmHollowClient(BmHollowClient.Type.snapshot, set, subset,
						desiredVersion); InputStream in = client.openStream()) {
					Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
				}
				return new HollowConsumer.Blob(desiredVersion) {

					@Override
					public InputStream getInputStream() throws IOException {
						return new BufferedInputStream(Files.newInputStream(file)) {
							@Override
							public void close() throws IOException {
								super.close();
								Files.delete(file);
							}
						};
					}

				};
			} catch (IOException e) {
				throw new HollowRetrievalException(e);
			}
		}

		@Override
		public Blob retrieveDeltaBlob(long currentVersion) {
			try {
				Path file = getPath(BmHollowClient.Type.delta, currentVersion);
				long newVersion = 0;
				try (BmHollowClient client = new BmHollowClient(BmHollowClient.Type.delta, set, subset, currentVersion);
						InputStream in = client.openStream()) {
					Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
					newVersion = client.getVersionHeader();
				}
				return new HollowConsumer.Blob(currentVersion, newVersion) {

					@Override
					public InputStream getInputStream() throws IOException {
						return new BufferedInputStream(Files.newInputStream(file)) {
							@Override
							public void close() throws IOException {
								super.close();
								Files.delete(file);
							}
						};
					}

				};
			} catch (IOException e) {
				throw new HollowRetrievalException(e);
			}

		}

		@Override
		public Blob retrieveReverseDeltaBlob(long currentVersion) {
			throw new UnsupportedOperationException();
		}

	}

	public class BmAnnouncementWatcher implements IAnnouncementWatcher, HollowVersionObserver {
		private List<HollowConsumer> observers = new ArrayList<>();
		private final String set;
		private final String subset;

		public BmAnnouncementWatcher(String set, String subset) {
			this.set = set;
			this.subset = subset;
			HollowVersion.registerObserver(this, set, subset);
		}

		@Override
		public long getLatestVersion() {
			return HollowVersion.getVersion(set, subset);
		}

		@Override
		public void subscribeToUpdates(HollowConsumer consumer) {
			observers.add(consumer);
		}

		@Override
		public void onUpdate(String set, String subset, long version) {
			if (set.equals(this.set) && subset.equals(this.subset)) {
				observers.forEach(HollowConsumer::triggerAsyncRefresh);
			}
		}

		@Override
		public boolean isListening() {
			return HollowVersion.isListening();
		}

	}

}
