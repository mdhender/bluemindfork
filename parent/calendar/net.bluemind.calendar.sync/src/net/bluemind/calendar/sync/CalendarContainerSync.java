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
package net.bluemind.calendar.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.asynchttpclient.handler.BodyDeferringAsyncHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.calendar.api.internal.IInternalCalendar;
import net.bluemind.calendar.service.internal.ICSImportTask;
import net.bluemind.calendar.service.internal.SingleCalendarICSImport;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerSyncStatus.Status;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.persistence.ContainerSettingsStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.container.sync.ISyncableContainer;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.icalendar.parser.CalendarOwner;
import net.bluemind.proxy.support.AHCWithProxy;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.TagRef;

public class CalendarContainerSync implements ISyncableContainer {

	private static final String HTTP_PREFIX = "http://";

	private static final String HTTPS_PREFIX = "https://";

	public static final String DOMAIN_SETTING_MIN_DELAY_KEY = "domain.setting.calendar.sync.min.delay";

	/** The default delay between two synchronizations of a same calendar. */
	private static final long DEFAULT_NEXT_SYNC_DELAY = TimeUnit.DAYS.toMillis(1);

	private static final String SYNC_TOKEN_KEY_ETAG = "etag";

	private static final String SYNC_TOKEN_KEY_MODIFIED_SINCE = "modified-since";

	private static final String SYNC_TOKEN_KEY_SYNC_DELAY = "current-sync-delay";

	private static final String SYNC_TOKEN_KEY_MD5_HASH = "md5";

	private static final double MAX_SYNC_OPERATIONS = 50;

	private static final long MAX_SYNC_DELAY = TimeUnit.DAYS.toMillis(3);

	static class SyncData {
		private long timestamp;
		private String modifiedSince;
		private String etag;
		private long nextSync;
		private InputStream body;
	}

	private static final Logger logger = LoggerFactory.getLogger(CalendarContainerSync.class);

	private BmContext context;
	private Container container;

	protected Map<String, String> calendarSettings;

	protected Map<String, String> domainSettings;

	protected CalendarContainerSync() {

	}

	public CalendarContainerSync(BmContext context, Container container) {
		this.context = context;
		this.container = container;
		final ContainerSettingsStore containerSettingsStore = new ContainerSettingsStore(
				DataSourceRouter.get(context, container.uid), container);
		try {
			this.calendarSettings = containerSettingsStore.getSettings();
		} catch (SQLException e) {
			logger.warn("Unable to load settings for calendar {}", container.name);
		}
		this.domainSettings = context.provider().instance(IDomainSettings.class, container.domainUid).get();
	}

	@Override
	public ContainerSyncResult sync(Map<String, String> syncTokens, IServerTaskMonitor monitor) {
		monitor.begin(3, null);

		ContainerSyncResult ret = new ContainerSyncResult();
		ret.status = new ContainerSyncStatus();
		ret.status.syncStatus = Status.ERROR;
		SyncData syncData = new SyncData();
		syncData.timestamp = System.currentTimeMillis();

		try {
			if (this.calendarSettings != null && this.calendarSettings.containsKey("icsUrl")) {
				monitor.progress(1, "Parsing ics...");
				final String modifiedSince = syncTokens.get(SYNC_TOKEN_KEY_MODIFIED_SINCE);
				final String etag = syncTokens.get(SYNC_TOKEN_KEY_ETAG);
				final String md5Hash = syncTokens.get(SYNC_TOKEN_KEY_MD5_HASH);

				fetchData(modifiedSince, etag, md5Hash, this.calendarSettings.get("icsUrl"), syncData);
				logger.info("Sync calendar {} (uid:{})", container.name, container.uid);
				syncCalendar(syncData, ret);
				logger.info("{} calendar sync done. created: {}, updated: {}, deleted: {}", container.name, ret.added,
						ret.updated, ret.removed);

				ret.status.nextSync = Math.max(syncData.timestamp + this.nextSyncDelay(), syncData.nextSync);
				ret.status.syncStatusInfo = "OK: sync done";
				return ret;
			}

			logger.error("Fail to fetch container settings for calendar {} (uid: {})", container.name, container.uid);
			return null;
		} catch (ExternalServerException e) {
			setNextSync(syncTokens, ret, 1.5f, true);
			ret.status.syncStatusInfo = e.getErrorInfo();
		} catch (HttpAuthException | UnknownServerException | SyncElementsNotModifiedException e) {
			setNextSync(syncTokens, ret, 2f, true);
			ret.status.syncStatusInfo = e.getErrorInfo();
		} catch (MalformedURLException e) {
			int penalty = 4;
			ret.status.nextSync = System.currentTimeMillis() + this.nextSyncDelay() * penalty;
			ret.status.syncStatusInfo = new MalformedIcsUrlException().getErrorInfo();
		} catch (NoSyncDoneException e) {
			ret.status.nextSync = Math.max(syncData.timestamp + this.nextSyncDelay(), syncData.nextSync);
			ret.status.syncStatusInfo = new NoSyncDoneException().getErrorInfo();
			ret.status.syncStatus = Status.SUCCESS;
		} catch (TooManySyncElementsException e) {
			double penalty = 2 - MAX_SYNC_OPERATIONS / e.operationCount;
			setNextSync(syncTokens, ret, penalty, false);
			ret.status.syncStatusInfo = e.getErrorInfo();
		} catch (Exception e) {
			logger.warn("Internal error while syncing ICS", e);
			setNextSync(syncTokens, ret, 2f, true);
			ret.status.syncStatusInfo = new InternalServerException().getErrorInfo();
		} finally {
			ret.status.syncTokens.put(SYNC_TOKEN_KEY_MODIFIED_SINCE, syncData.modifiedSince);
			ret.status.syncTokens.put(SYNC_TOKEN_KEY_ETAG, syncData.etag);
		}
		return ret;
	}

	private void setNextSync(Map<String, String> syncTokens, ContainerSyncResult ret, double penalty,
			boolean accumulatePenalty) {
		long baseDelay = accumulatePenalty ? getCurrentSyncDelay(syncTokens) : this.nextSyncDelay();
		long delay = (long) Math.min(MAX_SYNC_DELAY, Math.max(baseDelay * penalty, this.nextSyncDelay()));
		ret.status.nextSync = System.currentTimeMillis() + delay;
		ret.status.syncTokens.put(SYNC_TOKEN_KEY_SYNC_DELAY, "" + delay);
	}

	private long getCurrentSyncDelay(Map<String, String> syncTokens) {
		return Long.parseLong(syncTokens.getOrDefault(SYNC_TOKEN_KEY_SYNC_DELAY, "" + this.nextSyncDelay()));
	}

	static long getNextSyncDelay(Map<String, String> domainSettings) {
		if (domainSettings != null && domainSettings.containsKey(DOMAIN_SETTING_MIN_DELAY_KEY)) {
			return Long.valueOf(domainSettings.get(DOMAIN_SETTING_MIN_DELAY_KEY));
		}
		return DEFAULT_NEXT_SYNC_DELAY;
	}

	private long nextSyncDelay() {
		return getNextSyncDelay(this.domainSettings);
	}

	private void syncCalendar(SyncData data, ContainerSyncResult ret) {
		if (data.body != null) {
			try (InputStream in = data.body) {
				IInternalCalendar service = context.provider().instance(IInternalCalendar.class, container.uid);
				TaskStatus status = TaskUtils.wait(context.provider(), syncIcs(service, in));

				logger.info("Sync ICS result: {}", status.result);
				ContainerUpdatesResult result = JsonUtils.read(status.result, ContainerUpdatesResult.class);
				ret.added = result.added.size();
				ret.updated = result.updated.size();
				ret.removed = result.removed.size();
				ret.unhandled = result.unhandled.size();
				ret.status.lastSync = new Date();
				ret.status.syncStatus = Status.SUCCESS;

				if (result.synced() > MAX_SYNC_OPERATIONS) {
					throw new TooManySyncElementsException(result.synced());
				}
			} catch (TooManySyncElementsException e) {
				ret.status.syncStatus = Status.SUCCESS;
				throw e;
			} catch (Exception e) {
				logger.warn("Cannot sync ics", e);
				ret.status.syncStatus = Status.ERROR;
			}
		}
	}

	protected SyncData fetchData(String modifiedSince, String etag, String md5Hash, String icsUrl, SyncData syncData)
			throws Exception {
		// webcal:// -> java.net.MalformedURLException: unknown protocol: webcal
		// quick fix
		final boolean originalIcsUrlHasWebcalProtocol;
		if (icsUrl.startsWith("webcal://")) {
			originalIcsUrlHasWebcalProtocol = true;
			icsUrl = icsUrl.replace("webcal://", HTTP_PREFIX);
		} else {
			originalIcsUrlHasWebcalProtocol = false;
		}

		ResponseData response;
		try {
			response = requestIcs(icsUrl, "HEAD", modifiedSince, etag);
		} catch (MalformedURLException e) {
			logger.error("invalid url '{}'", icsUrl, e);
			syncData.modifiedSince = "";
			throw e;
		}

		if (this.isNotModified(modifiedSince, response)) {
			logger.debug("{} Not Modified (304 or header)", icsUrl);
			syncData.modifiedSince = Long.toString(response.lastModified);
			syncData.etag = response.etag;
			throw new SyncElementsNotModifiedException();
		}

		if (response.status == 405) {
			logger.debug("{} 405 Method Not Allowed", icsUrl);
		}

		ResponseData get = requestIcs(icsUrl, "GET", null, null);

		syncData.nextSync = response.nextSync == 0 ? get.nextSync : response.nextSync;

		if (get.status == 200) {
			// GET returns a Last-Modified header
			syncData.modifiedSince = Long.toString(get.lastModified);
			syncData.etag = get.etag;
			syncData.body = get.body;
			return syncData;
		} else if (originalIcsUrlHasWebcalProtocol && icsUrl.startsWith(HTTP_PREFIX)) {
			// try now with https
			return this.fetchData(modifiedSince, etag, md5Hash, icsUrl.replace(HTTP_PREFIX, HTTPS_PREFIX), syncData);
		} else {
			if (get.status >= 400 && get.status < 500) {
				throw new HttpAuthException();
			} else if (get.status >= 500) {
				throw new ExternalServerException();
			} else {
				logger.warn("Unknown server exception while syncing ICS, server returned status {}", get.status);
				throw new UnknownServerException(get.status);
			}
		}
	}

	/**
	 * @return <code>true</code> if <code>response</code> is considered as not
	 *         modified
	 */
	private boolean isNotModified(String lastModificationTimestamp, ResponseData response) {
		Long lastModification = Strings.isNullOrEmpty(lastModificationTimestamp) ? 0
				: Long.parseLong(lastModificationTimestamp);
		return response.status == 304 || (response.lastModified > 0 && response.lastModified <= lastModification);
	}

	private ResponseData requestIcs(String icsUrl, String method, String syncToken, String etag)
			throws IOException, InterruptedException {
		return call(icsUrl, method, syncToken, etag);
	}

	private ResponseData call(String icsUrl, String method, String modifiedSince, String etag)
			throws IOException, InterruptedException {
		BoundRequestBuilder request = AHCWithProxy
				.build(context.provider().instance(ISystemConfiguration.class).getValues()).prepare(method, icsUrl)
				.addHeader("User-Agent",
						"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/604.4.7 (KHTML, like Gecko) Version/11.0.2 Safari/604.4.7");

		if (etag != null) {
			request = request.addHeader("If-None-Match", etag);
		}

		if (modifiedSince != null) {
			try {
				final ZonedDateTime modifiedSinceDate = ZonedDateTime.ofInstant(
						Instant.ofEpochMilli(Long.parseLong(modifiedSince)), ZoneId.ofOffset("UTC", ZoneOffset.UTC));
				final String formattedModifiedSince = DateTimeFormatter.RFC_1123_DATE_TIME.format(modifiedSinceDate);
				request.setHeader("If-Modified-Since", formattedModifiedSince);
			} catch (NumberFormatException e) {
				// nothing to do
			}
		}

		PipedInputStream pipedInputStream = new PipedInputStream();
		PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
		BodyDeferringAsyncHandler bodyDeferringAsyncHandler = new BodyDeferringAsyncHandler(pipedOutputStream);
		ListenableFuture<Response> futureResponse = request.execute(bodyDeferringAsyncHandler);

		Response response = bodyDeferringAsyncHandler.getResponse();

		long lastModified = Optional.ofNullable(response.getHeader("Last-Modified")).map(this::parseRFC1123DateTime)
				.orElseGet(() -> Optional.ofNullable(response.getHeader("Content-Disposition"))
						.map(this::extractModificationDate).orElse((long) 0));
		String newEtag = response.getHeader("etag");
		int status = response.getStatusCode();
		long nextSync = extractCacheControlMaxAge(response.getHeaders())
				.orElseGet(() -> extractExpires(response.getHeader("Expires")).orElse((long) 0));

		if (status != 200 || !"GET".equals(method)) {
			if (!futureResponse.isDone() || !futureResponse.isCancelled()) {
				futureResponse.cancel(true);
			}

			return new ResponseData(null, status, lastModified, newEtag, nextSync);
		}

		return new ResponseData(new BodyDeferringAsyncHandler.BodyDeferringInputStream(futureResponse,
				bodyDeferringAsyncHandler, pipedInputStream), status, lastModified, newEtag, nextSync);
	}

	private Long parseRFC1123DateTime(String dateTime) {
		try {
			return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateTime)).toEpochMilli();
		} catch (NullPointerException | DateTimeParseException dtpe) {
		}

		return null;
	}

	public TaskRef syncIcs(IInternalCalendar calendarService, InputStream stream) throws ServerFault {

		List<TagRef> allTags = new ArrayList<>();

		BaseDirEntry.Kind calOwnerType = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDirectory.class, container.domainUid).findByEntryUid(container.owner).kind;

		if (calOwnerType != Kind.CALENDAR) {
			// owner tags
			ITags service = context.provider().instance(ITags.class, ITagUids.defaultUserTags(container.owner));
			allTags.addAll(
					service.all().stream().map(tag -> TagRef.create(ITagUids.defaultUserTags(container.owner), tag))
							.collect(Collectors.toList()));
		}

		// domain tags
		ITags service = context.provider().instance(ITags.class, ITagUids.defaultUserTags(container.domainUid));
		allTags.addAll(
				service.all().stream().map(tag -> TagRef.create(ITagUids.defaultUserTags(container.domainUid), tag))
						.collect(Collectors.toList()));

		return context.provider().instance(ITasksManager.class)
				.run(new SingleCalendarICSImport(calendarService, stream,
						Optional.of(new CalendarOwner(container.domainUid, container.owner, calOwnerType)), allTags,
						ICSImportTask.Mode.SYNC));
	}

	/** @see RFC-2183 https://tools.ietf.org/html/rfc2183 */
	private static final Pattern CONTENT_DISPO_MODIF_DATE_PATTERN = Pattern.compile("modification-date=\"(.*?)\";?");

	private Long extractModificationDate(String contentDisposition) {
		final Matcher matcher = CONTENT_DISPO_MODIF_DATE_PATTERN.matcher(contentDisposition);
		if (matcher.find()) {
			return parseRFC1123DateTime(matcher.group(1));
		}

		return null;
	}

	/**
	 * @return the time in milliseconds when the response is considered as obsolete
	 *         or zero if not found
	 */
	private Optional<Long> extractExpires(String expiresString) {
		if (Strings.isNullOrEmpty(expiresString)) {
			return Optional.empty();
		}

		return Optional.ofNullable(parseRFC1123DateTime(expiresString));
	}

	/**
	 * For finding <i>max-age</i> value (and avoiding <i>s-max-age</i>) in a
	 * <i>Cache-Control</i> list of directives.
	 */
	private static final Pattern MAX_AGE_PATTERN = Pattern.compile("(?<!s-)max-age=(\\d+)");

	/**
	 * @return the time in milliseconds when the content's cache expires or zero if
	 *         not found
	 */
	private Optional<Long> extractCacheControlMaxAge(HttpHeaders headers) {
		String maxAge = Optional.ofNullable(headers.get("Cache-Control")).map(value -> MAX_AGE_PATTERN.matcher(value))
				.filter(Matcher::find).map(m -> m.group(1)).filter(Objects::nonNull).filter(s -> !s.isEmpty())
				.orElse(null);

		try {
			return Optional.of(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Long.parseLong(maxAge)));
		} catch (NumberFormatException nfe) {
		}

		return Optional.empty();
	}

	class ResponseData {
		public final int status;
		public final long lastModified;
		public final String etag;
		public final long nextSync;
		public final InputStream body;

		public ResponseData(InputStream body, int status, long lastModified, String etag, long nextSync) {
			this.body = body;
			this.status = status;
			this.lastModified = lastModified;
			this.etag = etag;
			this.nextSync = nextSync;
		}

	}

}
