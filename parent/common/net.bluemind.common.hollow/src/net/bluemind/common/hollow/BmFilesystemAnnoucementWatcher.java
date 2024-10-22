/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.common.hollow;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;

public class BmFilesystemAnnoucementWatcher implements IAnnouncementWatcher {

	private static final Logger logger = LoggerFactory.getLogger(BmFilesystemAnnoucementWatcher.class);
	private static final AtomicLong tid = new AtomicLong();
	private final List<HollowConsumer> sub;
	private final Path announcePath;
	private final Path toWatch;
	private final Thread watcherThread;

	private volatile long version;
	private volatile boolean stopped;

	public BmFilesystemAnnoucementWatcher(Path publishPath) {
		logger.info("Setup watcher on {}", publishPath.toFile().getAbsolutePath());
		this.sub = new CopyOnWriteArrayList<>();
		this.toWatch = publishPath;
		this.announcePath = publishPath.resolve(HollowFilesystemAnnouncer.ANNOUNCEMENT_FILENAME);
		this.version = readLatestVersion();
		try {
			watcherThread = setupWatchService();
		} catch (Exception e) {
			throw new BmHollowException(e);
		}
		logger.info("STARTED with version {}", version);

	}

	private static final Modifier[] modifiers = reflectSensitivity();

	/**
	 * JVM on OSX only provide the polling implementation.
	 * 
	 * @return HIGH (every 2sec) modifier for polling implementation
	 */
	private static Modifier[] reflectSensitivity() {
		try {
			Class<?> klass = Class.forName("com.sun.nio.file.SensitivityWatchEventModifier");
			Modifier mod = Modifier.class.cast(klass.getMethod("valueOf", String.class).invoke(null, "HIGH"));
			return new Modifier[] { mod };
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new Modifier[0];
		}
	}

	private Thread setupWatchService() {
		FilesystemWatcherRunnable runnable = new FilesystemWatcherRunnable();
		Thread t = new Thread(runnable, "hollow-watch-" + toWatch.toFile().getName() + "-" + tid.incrementAndGet());
		t.setDaemon(true);
		t.start();
		return t;
	}

	@Override
	protected void finalize() throws Throwable { // NOSONAR
		// cancel watch service
		logger.info("Clearing watcher {}", this);
		stopped = true;

		// we don't call super because Object#finalize is empty by definition
	}

	private long readLatestVersion() {
		if (!Files.isReadable(announcePath)) {
			logger.warn("{} is not readable", announcePath.toFile().getAbsolutePath());
			return NO_ANNOUNCEMENT_AVAILABLE;
		}
		try {
			return Long.parseLong(new String(Files.readAllBytes(announcePath), StandardCharsets.US_ASCII));
		} catch (IOException e) {
			throw new BmHollowException(e);
		}
	}

	@Override
	public long getLatestVersion() {
		return version;
	}

	@Override
	public void subscribeToUpdates(HollowConsumer consumer) {
		sub.add(consumer);
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	@Override
	public boolean isListening() {
		return watcherThread != null && watcherThread.isAlive() && !watcherThread.isInterrupted();
	}

	@Override
	public void stop() {
		this.stopped = true;
		watcherThread.interrupt();
	}

	class FilesystemWatcherRunnable implements Runnable {

		private WatchService watcher;

		@Override
		public void run() {
			while (!stopped) {
				try {
					watcher = FileSystems.getDefault().newWatchService();
					WatchKey key = toWatch.register(watcher, new Kind[] { ENTRY_CREATE, ENTRY_MODIFY }, modifiers);
					logger.info("Registering WatchKey {} for {}", key, toWatch.toFile().getAbsolutePath());
					watch(watcher);
				} catch (IOException e) {
					throw new BmHollowException("Unable to setup hollow filesystem watcher on " + toWatch, e);
				} catch (InterruptedException e) {
					logger.error("Hollow filesystem watcher interrupted while watching, leaving");
					stopped = true;
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					logger.error("Hollow filesystem watcher failed while watching, going to retry: {}", !stopped, e);
				} finally {
					logger.info("Closing current watch service, going to retry: {}", !stopped);
					close();
				}

				if (!stopped) {
					checkVersion();
				}
			}

		}

		private void watch(WatchService service) throws InterruptedException {
			while (!stopped) {
				WatchKey key = service.take();
				for (WatchEvent<?> event : key.pollEvents()) {
					if (event.kind() != OVERFLOW) {
						checkFsEvent(event);
					}
				}
				if (!key.reset()) {
					throw new RuntimeException("Failed to reset hollow filesystem watcher watch key");
				}
			}
		}

		private void checkFsEvent(WatchEvent<?> event) {
			WatchEvent<Path> ev = cast(event);
			File changed = toWatch.resolve(ev.context()).toFile();
			if (changed.getName().equals(HollowFilesystemAnnouncer.ANNOUNCEMENT_FILENAME)) {
				checkVersion();
			}
		}

		private void checkVersion() {
			long freshVersion = readLatestVersion();
			if (freshVersion > version) {
				version = freshVersion;
				logger.info("Announce hollow version {}", freshVersion);
				for (HollowConsumer cons : sub) {
					cons.triggerAsyncRefresh();
				}
			}
		}

		public void close() {
			try {
				watcher.close();
			} catch (Exception e) {
				logger.warn("Fails to stop hollow filesystem watcher", e);
			}
		}

	}

}
