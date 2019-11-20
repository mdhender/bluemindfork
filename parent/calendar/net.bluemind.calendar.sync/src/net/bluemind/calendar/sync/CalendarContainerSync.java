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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.Security;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.hash.Hashing;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.VEventChanges;
import net.bluemind.calendar.api.VEventChanges.ItemAdd;
import net.bluemind.calendar.api.VEventChanges.ItemDelete;
import net.bluemind.calendar.api.VEventChanges.ItemModify;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerSyncStatus.Status;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerSettingsStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.sync.ISyncableContainer;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.utils.Trust;

public class CalendarContainerSync implements ISyncableContainer {

	public static final String DOMAIN_SETTING_MIN_DELAY_KEY = "domain.setting.calendar.sync.min.delay";

	/** The default delay between two synchronizations of a same calendar. */
	private static final long DEFAULT_NEXT_SYNC_DELAY = TimeUnit.DAYS.toMillis(1);

	private static final String SYNC_TOKEN_KEY_ETAG = "etag";

	private static final String SYNC_TOKEN_KEY_MODIFIED_SINCE = "modified-since";

	private static final String SYNC_TOKEN_KEY_MD5_HASH = "md5";

	private static final int STEP = 50;

	private class SyncData {
		public long timestamp;
		public String modifiedSince;
		public String etag;
		public String ics;
		public String md5Hash;
		public long nextSync;
	}

	private static final Logger logger = LoggerFactory.getLogger(CalendarContainerSync.class);

	private BmContext context;
	private Container container;

	private Map<String, String> calendarSettings;

	private Map<String, String> domainSettings;

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
	public ContainerSyncResult sync(Map<String, String> syncTokens, IServerTaskMonitor monitor) throws ServerFault {
		monitor.begin(3, null);
		try {
			if (this.calendarSettings != null && this.calendarSettings.containsKey("icsUrl")) {
				monitor.progress(1, "Parsing ics...");
				final String modifiedSince = syncTokens.get(SYNC_TOKEN_KEY_MODIFIED_SINCE);
				final String etag = syncTokens.get(SYNC_TOKEN_KEY_ETAG);
				final String md5Hash = syncTokens.get(SYNC_TOKEN_KEY_MD5_HASH);
				final SyncData syncData = fetchData(modifiedSince, etag, md5Hash, this.calendarSettings.get("icsUrl"));
				logger.info("Sync calendar {} (uid:{})", container.name, container.uid);
				final ContainerSyncResult ret = syncCalendar(syncData, container.name, monitor.subWork(2));
				logger.info(String.format("%s calendar sync done. created: %d, updated: %d, deleted: %d",
						container.name, ret.added, ret.updated, ret.removed));
				return ret;
			}

			logger.error("Fail to fetch container settings for calendar {} (uid: {})", container.name, container.uid);
			return null;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	private long nextSyncDelay() {
		if (this.domainSettings != null && this.domainSettings.containsKey(DOMAIN_SETTING_MIN_DELAY_KEY)) {
			return Long.valueOf(this.domainSettings.get(DOMAIN_SETTING_MIN_DELAY_KEY));
		}
		return DEFAULT_NEXT_SYNC_DELAY;
	}

	private ContainerSyncResult syncCalendar(SyncData data, String calendarName, IServerTaskMonitor monitor)
			throws ServerFault {
		ContainerSyncResult ret = new ContainerSyncResult();
		ret.status = new ContainerSyncStatus();
		ret.status.nextSync = Math.max(data.timestamp + this.nextSyncDelay(), data.nextSync);
		ret.status.syncTokens.put(SYNC_TOKEN_KEY_MODIFIED_SINCE, data.modifiedSince);
		ret.status.syncTokens.put(SYNC_TOKEN_KEY_ETAG, data.etag);
		ret.status.syncTokens.put(SYNC_TOKEN_KEY_MD5_HASH, data.md5Hash);
		ret.status.syncStatus = Status.ERROR;

		if (data.ics != null && !data.ics.isEmpty()) {
			List<ItemValue<VEventSeries>> events = new ArrayList<>();
			try {
				events = VEventServiceHelper.convertToVEventList(data.ics, Optional.empty());
			} catch (ServerFault sf) {
				logger.error(sf.getMessage(), sf);
				return ret;
			}
			monitor.begin(events.size(), "Going to import " + events.size() + " events");

			ICalendar service = context.provider().instance(ICalendar.class, container.uid);

			List<String> uids = service.all();
			ArrayList<String> icsUids = new ArrayList<String>(events.size());
			VEventChanges changes = new VEventChanges();
			changes.add = new ArrayList<VEventChanges.ItemAdd>();
			changes.modify = new ArrayList<VEventChanges.ItemModify>();
			changes.delete = new ArrayList<VEventChanges.ItemDelete>();

			int added = 0;
			int updated = 0;
			int removed = 0;
			int i = 0;

			for (ItemValue<VEventSeries> itemValue : events) {
				VEventSeries event = itemValue.value;
				ItemValue<VEventSeries> oldEvent = service.getComplete(itemValue.uid);

				String uid = itemValue.uid;

				if (uid != null) {
					icsUids.add(uid);
				} else {
					uid = UUID.randomUUID().toString();
				}

				if (oldEvent == null) {
					changes.add.add(ItemAdd.create(uid, event, false));
					i++;
				} else {
					if (itemValue.updated == null || itemValue.updated.after(oldEvent.updated)) {
						changes.modify.add(ItemModify.create(oldEvent.uid, event, false));
						i++;
					}
				}

				if (i == STEP) {
					ContainerUpdatesResult result = service.updates(changes);
					monitor.progress(i, null);
					added += result.added.size();
					updated += result.updated.size();
					changes.add = new ArrayList<VEventChanges.ItemAdd>();
					changes.modify = new ArrayList<VEventChanges.ItemModify>();
					changes.delete = new ArrayList<VEventChanges.ItemDelete>();
					i = 0;
				}
			}

			uids.removeAll(icsUids);
			for (String uid : uids) {
				changes.delete.add(ItemDelete.create(uid, false));
				i++;
				if (i == STEP) {
					ContainerUpdatesResult result = service.updates(changes);
					monitor.progress(i, null);
					added += result.added.size();
					updated += result.updated.size();
					removed += result.removed.size();
					changes.add = new ArrayList<VEventChanges.ItemAdd>();
					changes.modify = new ArrayList<VEventChanges.ItemModify>();
					changes.delete = new ArrayList<VEventChanges.ItemDelete>();
					i = 0;
				}
			}

			if (i > 0) {
				ContainerUpdatesResult result = service.updates(changes);
				monitor.progress(i, null);
				removed += result.removed.size();
				added += result.added.size();
				updated += result.updated.size();
			}

			ret.added = added;
			ret.updated = updated;
			ret.removed = removed;
			ret.status.lastSync = new Date();
		}

		ret.status.syncStatus = Status.SUCCESS;
		return ret;
	}

	private SyncData fetchData(String modifiedSince, String etag, String md5Hash, String icsUrl) throws Exception {
		SyncData ret = new SyncData();
		ret.timestamp = System.currentTimeMillis();

		// webcal:// -> java.net.MalformedURLException: unknown protocol: webcal
		// quick fix
		final boolean originalIcsUrlHasWebcalProtocol;
		if (icsUrl.startsWith("webcal://")) {
			originalIcsUrlHasWebcalProtocol = true;
			icsUrl = icsUrl.replace("webcal://", "http://");
		} else {
			originalIcsUrlHasWebcalProtocol = false;
		}

		ResponseData response;
		try {
			response = requestIcs(icsUrl, "HEAD", modifiedSince, etag);
		} catch (MalformedURLException e2) {
			logger.error("invalid url '{}'", icsUrl, e2);
			ret.modifiedSince = "";
			return ret;
		}

		if (response.status == 304) {
			logger.debug("{} 304 Not Modified", icsUrl);
			ret.modifiedSince = Long.toString(response.lastModified);
			ret.etag = response.etag;
			return ret;
		}

		if (response.status == 405) {
			logger.debug("{} 405 Method Not Allowed", icsUrl);
		}

		ResponseData get = requestIcs(icsUrl, "GET", null, null);

		ret.nextSync = response.nextSync == 0 ? get.nextSync : response.nextSync;

		if (get.status == 200) {
			// GET returns a Last-Modified header
			ret.modifiedSince = Long.toString(get.lastModified);
			ret.etag = get.etag;

			@SuppressWarnings("deprecation")
			final String newMd5 = get.data != null ? Hashing.md5().hashUnencodedChars(get.data).toString() : "";
			ret.md5Hash = newMd5;
			if (newMd5.equals(md5Hash)) {
				// exit without setting ret.ics in order to avoid a calendar
				// update
				return ret;
			}
		} else if (originalIcsUrlHasWebcalProtocol && icsUrl.startsWith("http://")) {
			// try now with https
			return this.fetchData(modifiedSince, etag, md5Hash, icsUrl.replace("http://", "https://"));
		} else {
			return ret;
		}
		ret.ics = get.data;
		return ret;
	}

	private ResponseData requestIcs(String icsUrl, String method, String syncToken, String etag) throws Exception {
		try {
			return call(icsUrl, method, syncToken, etag);
		} catch (InvalidAlgorithmParameterException e) {
			BouncyCastleProvider bc = new BouncyCastleProvider();
			boolean installed = Security.getProvider(bc.getName()) != null;
			try {
				Security.removeProvider(bc.getName());
				Security.insertProviderAt(bc, 1);
				return call(icsUrl, method, syncToken, etag);
			} finally {
				Security.removeProvider(bc.getName());
				if (installed) {
					// add provider to the end of the provider's list
					Security.addProvider(bc);
				}
			}
		}
	}

	private ResponseData call(String icsUrl, String method, String modifiedSince, String etag) throws Exception {
		URL url = new URL(icsUrl);

		HttpURLConnection conn = null;

		try {
			conn = (HttpURLConnection) url.openConnection();
			if (conn instanceof HttpsURLConnection) {
				((HttpsURLConnection) conn).setHostnameVerifier(Trust.acceptAllVerifier());
				SSLContext sc = Trust.createSSLContext();
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());

			}
			conn.setRequestMethod(method);

			conn.setRequestProperty("User-Agent",
					"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/604.4.7 (KHTML, like Gecko) Version/11.0.2 Safari/604.4.7");

			if (etag != null) {
				conn.setRequestProperty("If-None-Match", etag);
			}

			if (modifiedSince != null) {
				try {
					final ZonedDateTime modifiedSinceDate = ZonedDateTime.ofInstant(
							Instant.ofEpochMilli(Long.parseLong(modifiedSince)),
							ZoneId.ofOffset("UTC", ZoneOffset.UTC));
					final String formattedModifiedSince = DateTimeFormatter.RFC_1123_DATE_TIME
							.format(modifiedSinceDate);
					conn.setRequestProperty("If-Modified-Since", formattedModifiedSince);
				} catch (NumberFormatException e) {
				}
			}

			long lastModified = conn.getLastModified();
			if (lastModified == 0) {
				lastModified = this.extractModificationDate(conn);
			}
			String newEtag = conn.getHeaderField("etag");
			int status = conn.getResponseCode();
			String data = null;
			if (method.equals("GET")) {
				StringBuilder ics = new StringBuilder();
				try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String line;
					while ((line = rd.readLine()) != null) {
						ics.append(line).append("\n");
					}
				}
				data = ics.toString();
			}

			// Cache-Control/max-age has precedence over Expires
			long nextSync = this.extractCacheControlMaxAge(conn);
			if (nextSync == 0) {
				nextSync = this.extractExpires(conn);
			}

			return new ResponseData(status, lastModified, data, newEtag, nextSync);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

	}

	/** @see RFC-2183 https://tools.ietf.org/html/rfc2183 */
	private final static Pattern CONTENT_DISPO_MODIF_DATE_PATTERN = Pattern.compile("modification-date=\"(.*?)\";?");

	private long extractModificationDate(final HttpURLConnection connection) {
		final String contentDispo = connection.getHeaderField("Content-Disposition");
		if (contentDispo != null) {
			final Matcher matcher = CONTENT_DISPO_MODIF_DATE_PATTERN.matcher(contentDispo);
			if (matcher.find()) {
				final String dateString = matcher.group(1);
				return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(dateString)).toEpochMilli();
			}
		}
		return 0;
	}

	/**
	 * @return the time in milliseconds when the response is considered as
	 *         obsolete or zero if not found
	 */
	private long extractExpires(final HttpURLConnection connection) {
		final String expiresString = connection.getHeaderField("Expires");
		if (expiresString != null) {
			return Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(expiresString)).toEpochMilli();
		}
		return 0;
	}

	/**
	 * For finding <i>max-age</i> value (and avoiding <i>s-max-age</i>) in a
	 * <i>Cache-Control</i> list of directives.
	 */
	private static final Pattern MAX_AGE_PATTERN = Pattern.compile("(?<!s-)max-age=(\\d+)");

	/**
	 * @return the time in milliseconds when the content's cache expires or zero
	 *         if not found
	 */
	private long extractCacheControlMaxAge(final HttpURLConnection connection) {
		// retrieve Cache-Control directives and extract max-age if present
		final Optional<String> cacheControlWithMaxAge = connection.getHeaderFields().entrySet().stream()
				.filter(entry -> entry.getKey() != null ? entry.getKey().equalsIgnoreCase("Cache-Control") : false)
				.flatMap(entry -> entry.getValue().stream()).filter(v -> MAX_AGE_PATTERN.matcher(v).find()).findFirst();

		if (cacheControlWithMaxAge.isPresent()) {
			// may contain other directives, comma separated
			final Matcher matcher = MAX_AGE_PATTERN.matcher(cacheControlWithMaxAge.get());
			final String maxAgeValueString = matcher.find() ? matcher.group(1) : "0";
			return System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Long.valueOf(maxAgeValueString));
		}
		return 0;
	}

	class ResponseData {
		public final int status;
		public final String data;
		public final long lastModified;
		public final String etag;
		public final long nextSync;

		public ResponseData(int status, long lastModified, String data, String etag, long nextSync) {
			this.status = status;
			this.lastModified = lastModified;
			this.data = data;
			this.etag = etag;
			this.nextSync = nextSync;
		}

	}

}
