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
package net.bluemind.eas.client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import net.bluemind.eas.client.commands.Autodiscover;
import net.bluemind.eas.client.commands.FetchItemSync;
import net.bluemind.eas.client.commands.FolderSync;
import net.bluemind.eas.client.commands.GetChangesSync;
import net.bluemind.eas.client.commands.GetItemEstimate;
import net.bluemind.eas.client.commands.Options;
import net.bluemind.eas.client.commands.Settings;
import net.bluemind.eas.client.commands.Sync;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.utils.FileUtils;
import net.bluemind.eas.wbxml.WBXMLTools;

public class OPClient {

	private ProtocolVersion protocolVersion;

	protected Logger logger = LoggerFactory.getLogger(getClass());
	private AccountInfos ai;
	private AsyncHttpClient ahc;
	private boolean dumpMode;

	static {
		XTrustProvider.install();
	}

	public OPClient(String loginAtDomain, String password, String devId, String devType, String userAgent, String url)
			throws Exception {

		setProtocolVersion(ProtocolVersion.V121);
		this.ai = new AccountInfos(loginAtDomain, password, devId, devType, url, userAgent);

		this.ahc = createHttpClient();
	}

	public void setDevType(String type) {
		ai.setDevType(type);
	}

	public void destroy() throws Exception {
		this.ahc.close();
	}

	private <T> T run(IEasCommand<T> cmd) throws Exception {
		return cmd.run(ai, this, ahc);
	}

	private AsyncHttpClient createHttpClient() {
		AsyncHttpClientConfig config = new DefaultAsyncHttpClientConfig.Builder().setFollowRedirect(false)
				.setMaxRedirects(0).setMaxRequestRetry(0).setRequestTimeout(180000).build();
		return new DefaultAsyncHttpClient(config);
	}

	public void autodiscover() throws Exception {
		run(new Autodiscover());
	}

	public void options() throws Exception {
		run(new Options());
	}

	public FolderSyncResponse folderSync(String key) throws Exception {
		return run(new FolderSync(key));
	}

	public SyncResponse initialSync(Folder... folders) throws Exception {
		return run(new Sync(folders));
	}

	public SyncResponse sync(Document doc) throws Exception {
		return run(new Sync(doc));
	}

	public EstimateResponse estimate(Folder f) throws Exception {
		return run(new GetItemEstimate(f));
	}

	public Document postXml(String namespace, Document doc, String cmd) throws Exception {
		return postXml(namespace, doc, cmd, null, false);
	}

	public Document postXml(String namespace, Document doc, String cmd, Map<String, String> headers) throws Exception {
		return postXml(namespace, doc, cmd, null, false, headers);
	}

	public Document postXml(String namespace, Document doc, String cmd, String policyKey, boolean multipart)
			throws Exception {
		return postXml(namespace, doc, cmd, policyKey, multipart, new HashMap<String, String>());
	}

	public Document postXml(String namespace, Document doc, String cmd, String policyKey, boolean multipart,
			Map<String, String> addHeaders) throws Exception {

		if (logger.isDebugEnabled() || dumpMode) {
			DOMUtils.logDom(doc);
		}

		byte[] data = WBXMLTools.toWbxml(namespace, doc);
		String url = ai.getUrl() + "?User=" + ai.getLogin() + "&DeviceId=" + ai.getDevId() + "&DeviceType="
				+ ai.getDevType() + "&Cmd=" + cmd;

		BoundRequestBuilder pm = ahc.preparePost(url);

		pm.setBody(data);
		pm.setHeader("Content-Length", "" + data.length);
		pm.setHeader("Content-Type", "application/vnd.ms-sync.wbxml");
		Realm realm = new Realm.Builder(ai.getLogin(), ai.getPassword()).setScheme(AuthScheme.BASIC)
				.setCharset(StandardCharsets.UTF_8).build();
		pm.setRealm(realm);
		pm.setHeader("User-Agent", ai.getUserAgent());
		pm.setHeader("Ms-ASProtocolVersion", protocolVersion.toString());
		pm.setHeader("Accept", "*/*");
		pm.setHeader("Accept-Language", "fr-fr");
		// pm.setHeader("X-MS-PolicyKey", "0");
		for (String s : addHeaders.keySet()) {
			pm.setHeader(s, addHeaders.get(s));
		}
		pm.setHeader("Connection", "keep-alive");
		if (multipart) {
			pm.setHeader("MS-ASAcceptMultiPart", "T");
			pm.setHeader("Accept-Encoding", "gzip");
		}

		if (policyKey != null) {
			pm.setHeader("X-MS-PolicyKey", policyKey);
		}
		byte[] all = post(namespace, doc, cmd, policyKey, multipart);

		Document xml = null;
		if (multipart) {
			int idx = 0;
			byte[] buffer = new byte[4];
			for (int i = 0; i < buffer.length; i++) {
				buffer[i] = all[idx++];
			}
			int nbPart = byteArrayToInt(buffer);

			for (int p = 0; p < nbPart; p++) {
				for (int i = 0; i < buffer.length; i++) {
					buffer[i] = all[idx++];
				}
				int start = byteArrayToInt(buffer);

				for (int i = 0; i < buffer.length; i++) {
					buffer[i] = all[idx++];
				}
				int length = byteArrayToInt(buffer);

				byte[] value = new byte[length];
				for (int j = 0; j < length; j++) {
					value[j] = all[start++];
				}
				if (p == 0) {
					xml = WBXMLTools.toXml(value);
					if (logger.isDebugEnabled() || dumpMode) {
						DOMUtils.logDom(xml);
					}
				} else {
					String file = new String(value);
					logger.info("File: " + file);
				}

			}
		} else if (all.length > 0) {
			xml = WBXMLTools.toXml(all);
			if (logger.isDebugEnabled() || dumpMode) {
				DOMUtils.logDom(xml);
			}
		}
		return xml;
	}

	public byte[] post(String namespace, Document doc, String cmd, String policyKey, boolean multipart)
			throws Exception {

		if (logger.isDebugEnabled() || dumpMode) {
			DOMUtils.logDom(doc);
		}

		byte[] data = WBXMLTools.toWbxml(namespace, doc);
		String url = ai.getUrl() + "?User=" + ai.getLogin() + "&DeviceId=" + ai.getDevId() + "&DeviceType="
				+ ai.getDevType() + "&Cmd=" + cmd;
		logger.info("Posting to {}", url);
		BoundRequestBuilder pm = ahc.preparePost(url);

		pm.setBody(data);
		pm.setHeader("Content-Length", "" + data.length);
		pm.setHeader("Content-Type", "application/vnd.ms-sync.wbxml");
		Realm realm = new Realm.Builder(ai.getLogin(), ai.getPassword()).setScheme(AuthScheme.BASIC)
				.setCharset(StandardCharsets.UTF_8).build();
		pm.setRealm(realm);
		pm.setHeader("User-Agent", ai.getUserAgent());
		pm.setHeader("Ms-ASProtocolVersion", protocolVersion.toString());
		pm.setHeader("Accept", "*/*");
		pm.setHeader("Accept-Language", "fr-fr");
		// pm.setHeader("X-MS-PolicyKey", "0");
		pm.setHeader("Connection", "keep-alive");
		if (multipart) {
			pm.setHeader("MS-ASAcceptMultiPart", "T");
			// pm.setHeader("Accept-Encoding", "gzip");
		}

		if (policyKey != null) {
			pm.setHeader("X-MS-PolicyKey", policyKey);
		}

		try {
			ListenableFuture<Response> future = pm.execute();
			Response response = future.get();
			int code = response.getStatusCode();
			if (code != 200) {
				logger.error("method failed:\n" + response.getStatusText() + "\n" + response.getResponseBody());
				return null;
			} else {
				logger.info("HTTP Status: " + code);
				List<Entry<String, String>> srvHeaders = response.getHeaders().entries();
				for (Entry<String, String> entry : srvHeaders) {
					logger.info("S: Header '{}' => '{}'", entry.getKey(), entry.getValue());
				}
				InputStream in = response.getResponseBodyAsStream();
				if (response.getHeader("Content-Encoding") != null
						&& response.getHeader("Content-Encoding").contains("gzip")) {
					logger.info("Input is compressed.");
					in = new GZIPInputStream(in);
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				FileUtils.transfer(in, out, true);
				return out.toByteArray();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	private final int byteArrayToInt(byte[] b) {
		byte[] inverse = new byte[b.length];
		int in = b.length - 1;
		for (int i = 0; i < b.length; i++) {
			inverse[in--] = b[i];
		}
		return (inverse[0] << 24) + ((inverse[1] & 0xFF) << 16) + ((inverse[2] & 0xFF) << 8) + (inverse[3] & 0xFF);
	}

	public byte[] postGetAttachment(String attachmentName) throws Exception {
		BoundRequestBuilder pm = ahc.preparePost(ai.getUrl() + "?User=" + ai.getLogin() + "&DeviceId=" + ai.getDevId()
				+ "&DeviceType=" + ai.getDevType() + "&Cmd=GetAttachment&AttachmentName=" + attachmentName);
		pm.setHeader("Authorization", ai.authValue());
		pm.setHeader("User-Agent", ai.getUserAgent());
		pm.setHeader("Ms-ASProtocolVersion", protocolVersion.toString());
		pm.setHeader("Accept", "*/*");
		pm.setHeader("Accept-Language", "fr-fr");
		pm.setHeader("Connection", "keep-alive");

		ListenableFuture<Response> future = pm.execute();
		Response r = future.get();

		byte[] ret = null;
		if (r.getStatusCode() != 200) {
			logger.error("method failed:\n" + r.getStatusText() + "\n" + r.getResponseBody());
		} else {
			InputStream is = r.getResponseBodyAsStream();
			ret = FileUtils.streamBytes(is, true);
		}

		return ret;

	}

	public ProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}

	public void setProtocolVersion(ProtocolVersion protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public SyncResponse syncChanges(String sk, Folder f, int pageSize) throws Exception {
		return syncChanges(sk, f, pageSize, null);
	}

	public SyncResponse syncChanges(String sk, Folder f, int pageSize, List<Long> toFlag) throws Exception {
		logger.info("syncChanges on collection " + f.getServerId() + " type: " + f.getType());
		GetChangesSync s = new GetChangesSync(f);
		s.setKey(f, sk);
		s.setPageSize(pageSize);
		if (toFlag != null) {
			s.setToFlag(toFlag);
		}

		return run(s);
	}

	public SyncResponse syncChanges(String sk, Folder f, int pageSize, boolean asHTML) throws Exception {
		logger.info("syncChanges on collection " + f.getServerId() + " type: " + f.getType());
		GetChangesSync s = new GetChangesSync(f);
		s.setKey(f, sk);
		s.setPageSize(pageSize);
		s.setHtml(asHTML);

		return run(s);
	}

	public SyncResponse fetch(String sk, Folder f, String serverId) throws Exception {
		logger.info("fetch on collection " + f.getServerId() + " type: " + f.getType());
		Sync s = new FetchItemSync(f, serverId, 4);
		s.setKey(f, sk);

		return run(s);
	}

	public SyncResponse fetch(String sk, Folder f, String serverId, int msEmailBodyType) throws Exception {
		logger.info("fetch on collection " + f.getServerId() + " type: " + f.getType());
		Sync s = new FetchItemSync(f, serverId, msEmailBodyType);
		s.setKey(f, sk);

		return run(s);
	}

	public boolean isDumpMode() {
		return dumpMode;
	}

	public void setDumpMode(boolean dumpMode) {
		this.dumpMode = dumpMode;
	}

	public SettingsResponse settings() throws Exception {
		return run(new Settings());
	}

}
