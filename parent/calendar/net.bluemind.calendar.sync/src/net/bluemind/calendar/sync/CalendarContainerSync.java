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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.Security;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

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
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerSettingsStore;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.container.sync.ISyncableContainer;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.utils.Trust;

public class CalendarContainerSync implements ISyncableContainer {

	private static final int STEP = 50;

	private class SyncData {
		public long timestamp;
		public String syncToken;
		public String ics;
	}

	private static final Logger logger = LoggerFactory.getLogger(CalendarContainerSync.class);

	private BmContext context;
	private Container container;

	public CalendarContainerSync(BmContext context, Container container) {
		this.context = context;
		this.container = container;
	}

	@Override
	public ContainerSyncResult sync(String syncToken, IServerTaskMonitor monitor) throws ServerFault {
		monitor.begin(3, null);
		try {
			DataSource ds = DataSourceRouter.get(context, container.uid);
			ContainerSettingsStore containerSettingsStore = new ContainerSettingsStore(ds, container);
			Map<String, String> settings = containerSettingsStore.getSettings();
			if (settings != null && settings.containsKey("icsUrl")) {
				SyncData data = fetchData(syncToken, settings.get("icsUrl"));
				monitor.progress(1, "Parsing ics...");
				logger.info("Sync calendar {} (uid:{})", container.name, container.uid);
				ContainerSyncResult ret = syncCalendar(data, monitor.subWork(2));
				return ret;
			}

			logger.error("Fail to fetch container settings for calendar {} (uid: {})", container.name, container.uid);
			return null;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	/**
	 * @param iServerTaskMonitor
	 * @param ics
	 * @throws ServerFault
	 */
	private ContainerSyncResult syncCalendar(SyncData data, IServerTaskMonitor monitor) throws ServerFault {
		ContainerSyncResult ret = new ContainerSyncResult();
		ret.status = new ContainerSyncStatus();
		ret.status.nextSync = data.timestamp + 3600000;
		ret.status.syncToken = data.syncToken;

		if (data.ics != null && !data.ics.isEmpty()) {
			List<ItemValue<VEventSeries>> events = new ArrayList<>();
			try {
				events = VEventServiceHelper.convertToVEventList(data.ics, Optional.empty());
			} catch (ServerFault sf) {
				logger.error(sf.getMessage(), sf);
				return ret;
			}
			if (monitor != null) {
				monitor.begin(events.size(), "Going to import " + events.size() + " events");
			}

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
					if (monitor != null) {
						monitor.progress(i, null);
					}
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
					if (monitor != null) {
						monitor.progress(i, null);
					}
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
				if (monitor != null) {
					monitor.progress(i, null);
				}
				removed += result.removed.size();
				added += result.added.size();
				updated += result.updated.size();
			}

			ret.added = added;
			ret.updated = updated;
			ret.removed = removed;
		}
		return ret;
	}

	/**
	 * @return
	 * @throws SQLException
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws ProtocolException
	 * @throws ParseException
	 * @throws ServerFault
	 */
	private SyncData fetchData(String syncToken, String icsUrl) throws Exception {
		SyncData ret = new SyncData();
		ret.timestamp = System.currentTimeMillis();

		// webcal:// -> java.net.MalformedURLException: unknown protocol: webcal
		// quick fix
		if (icsUrl.startsWith("webcal://")) {
			icsUrl = icsUrl.replace("webcal://", "http://");
		}

		ResponseData response;
		try {
			response = requestIcs(icsUrl, "HEAD", syncToken);
		} catch (MalformedURLException e) {
			logger.error("invalid url '{}'", icsUrl, e);
			ret.syncToken = "";
			return ret;
		}

		if (response.status == 304) {
			logger.debug("{} 304 Not Modified", icsUrl);
			ret.syncToken = Long.toString(response.lastModified);
			return ret;
		}

		if (response.status == 405) {
			logger.debug("{} 405 Method Not Allowed", icsUrl);
		}

		ResponseData get = requestIcs(icsUrl, "GET", null);

		if (get.status == 200) {
			// HEAD returns a Last-Modified header
			ret.syncToken = Long.toString(get.lastModified);
		} else {
			String md5 = Hashing.md5().hashUnencodedChars(get.data).toString();
			if (syncToken.equals(md5)) {
				logger.info("{} ICS MD5 not modified", icsUrl);
				ret.syncToken = syncToken;
				return ret;
			}
			ret.syncToken = md5;
		}

		ret.ics = get.data;
		return ret;
	}

	private ResponseData requestIcs(String icsUrl, String method, String syncToken) throws Exception {
		try {
			return call(icsUrl, method, syncToken);
		} catch (InvalidAlgorithmParameterException e) {
			BouncyCastleProvider bc = new BouncyCastleProvider();
			boolean installed = Security.getProvider(bc.getName()) != null;
			try {
				Security.removeProvider(bc.getName());
				Security.insertProviderAt(bc, 1);
				return call(icsUrl, method, syncToken);
			} finally {
				Security.removeProvider(bc.getName());
				if (installed) {
					// add provider to the end of the provider's list
					Security.addProvider(bc);
				}
			}
		}
	}

	private ResponseData call(String icsUrl, String method, String syncToken) throws Exception {
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

			if (syncToken != null) {
				try {
					Long lm = Long.parseLong(syncToken);
					SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
					conn.setRequestProperty("If-Modified-Since", sdf.format(new Date(lm)));
				} catch (NumberFormatException e) {

				}
			}

			long lastModified = conn.getLastModified();
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
			return new ResponseData(status, lastModified, data);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

	}

	class ResponseData {
		public final int status;
		public final String data;
		public final long lastModified;

		public ResponseData(int status, long lastModified, String data) {
			this.status = status;
			this.lastModified = lastModified;
			this.data = data;
		}

	}

}
