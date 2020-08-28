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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.api.consumer.HollowConsumer.Blob;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;

public class BmHollowContext {

	private static File tmpDir;

	static {
		BmHollowContext.tmpDir = new File(System.getProperty("java.io.tmpdir"), "bm-hollowed");
		tmpDir.mkdirs();
	}

	public HollowContext create(String set, String subset) {
		AnnouncementWatcher announcementWatcher = new BmAnnouncementWatcher(set, subset);
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

		private String getPath(BmHollowClient.Type type, long desiredVersion) {
			return new File(tmpDir,
					type + "-" + set + "-" + subset + "-" + desiredVersion + "-" + System.currentTimeMillis())
							.getAbsolutePath();
		}

		@Override
		public Blob retrieveSnapshotBlob(long desiredVersion) {
			try {
				String path = getPath(BmHollowClient.Type.snapshot, desiredVersion);
				Path file = new File(path).toPath();
				try (BmHollowClient client = new BmHollowClient(BmHollowClient.Type.snapshot, set, subset,
						desiredVersion)) {
					Files.copy(client.openStream(), file);
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
				String path = getPath(BmHollowClient.Type.delta, currentVersion);
				long newVersion = 0;
				Path file = new File(path).toPath();
				try (BmHollowClient client = new BmHollowClient(BmHollowClient.Type.delta, set, subset,
						currentVersion)) {
					Files.copy(client.openStream(), file);
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

	public class BmAnnouncementWatcher implements AnnouncementWatcher, HollowVersionObserver {
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

	}

}
