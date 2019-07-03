package net.bluemind.dav.server.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Throwables;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Realm;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import com.ning.http.client.Response;
import com.ning.http.client.generators.InputStreamBodyGenerator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import net.bluemind.dav.server.proto.Depth;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.xml.DOMUtils;

public class DavClient {

	private static final Logger logger = LoggerFactory.getLogger(DavClient.class);

	private static final AsyncHttpClient newClient(String login, String password) {
		Realm realm = new Realm.RealmBuilder().setPrincipal(login).setPassword(password).setUsePreemptiveAuth(true)
				.setScheme(AuthScheme.BASIC).build();

		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setFollowRedirect(false).setMaxRedirects(0)
				.setPooledConnectionIdleTimeout(60000).setMaxRequestRetry(0).setRequestTimeout(30000).setRealm(realm)
				.setAllowPoolingConnections(true).setAllowPoolingSslConnections(true)
				.setHostnameVerifier(new HostnameVerifier() {

					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				}).setAcceptAnyCertificate(true).build();
		AsyncHttpClient client = new AsyncHttpClient(config);
		return client;
	}

	private final AsyncHttpClient client;
	private boolean closed;
	private final StringBuilder path;
	private final int len;
	private final AtomicLong cnt;
	private final Configuration fmConfig;

	public DavClient(String login, String password, String url) {
		this.client = newClient(login, password);
		closed = false;
		cnt = new AtomicLong();
		len = url.length() - (url.endsWith("/") ? 1 : 0);
		path = new StringBuilder(256);
		path.append(url, 0, len);
		this.fmConfig = new Configuration();
		fmConfig.setClassForTemplateLoading(getClass(), "/");
	}

	private String path(String s) {
		path.setLength(len);
		return path.append(s).toString();
	}

	private RequestBuilder req(String method, String path) {
		RequestBuilder rb = new RequestBuilder(method).setUrl(path(path));
		return rb;
	}

	private Response run(Request req) {
		long rid = cnt.incrementAndGet();
		long time = System.currentTimeMillis();
		try {
			Response r = client.executeRequest(req).get();
			time = System.currentTimeMillis() - time;
			logger.info("S[{}]: {} {} in {}ms.", rid, r.getStatusCode(), r.getStatusText(), time);
			for (String hn : r.getHeaders().keySet()) {
				String hv = r.getHeader(hn);
				logger.info("S[{}]: HEADER {}: {}", rid, hn, hv);
			}
			String ct = r.getContentType();
			if (ct != null && (ct.startsWith("text/") || ct.contains("/xml"))) {
				logger.info("BODY S[{}]:\n{}", rid, r.getResponseBody());
				logger.info("S[{}] ====== END OF BODY ======", rid, ct);
			} else {
				logger.info("S[{}] ====== Content-Type is {} ======", rid, ct);
			}
			return r;
		} catch (Exception e) {
			logger.error("S[" + rid + "]: " + e.getMessage(), e);
			throw Throwables.propagate(e);
		}
	}

	private Document templateRequest(String method, String path, String template, Depth depth) {
		return templateRequest(method, path, template, depth, new HashMap<String, Object>());
	}

	private Document templateRequest(String method, String path, String template, Depth depth,
			Map<String, Object> tplData) {
		RequestBuilder rb = req(method, path);
		if (depth != null) {
			rb.addHeader("Depth", depth.toString());
		}
		if (template.endsWith(".xml")) {
			rb.setBodyEncoding("utf-8");
			rb.setHeader("Content-Type", "text/xml");
		} else if (template.endsWith(".ics")) {
			rb.setBodyEncoding("utf-8");
			rb.setHeader("Content-Type", "text/calendar");
		} else {
			logger.error("Missing type for {}", template);
		}
		StringWriter sw = new StringWriter(1024 * 1024);
		try {
			Template fmTemplate = fmConfig.getTemplate("bodyTemplates/" + template);
			fmTemplate.process(tplData, sw);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		InputStream in = new ByteArrayInputStream(sw.toString().getBytes());
		rb.setBody(new InputStreamBodyGenerator(in));
		Response resp = run(rb.build());
		int sc = resp.getStatusCode();
		if (sc > 299) {
			throw new DavException(sc, resp.getStatusText());
		}
		if (sc == 207) {
			try {
				return DOMUtils.parse(resp.getResponseBodyAsStream());
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		} else {
			logger.info("Response is not multistatus, sc: " + sc);
			return null;
		}
	}

	public String getCurrentUserPrincipal(String root) {
		Element r = templateRequest("PROPFIND", root, "propfind_current-user-principal.xml", Depth.ZERO)
				.getDocumentElement();
		return href(r, NS.WEBDAV, "current-user-principal");
	}

	public Document proppatch(String path, String template) {
		logger.info("PROPPATCH {} with template {}", path, template);
		Document doc = templateRequest("PROPPATCH", path, template, null);
		return doc;
	}

	public Document post(String path, String template, Map<String, Object> params) {
		logger.info("POST {} with template {}", path, template);
		Document doc = templateRequest("POST", path, template, null, params);
		return doc;
	}

	public Document propfind(String path, String template, Depth d) {
		logger.info("PROPFIND {} with template {}", path, template);
		Document doc = templateRequest("PROPFIND", path, template, d);
		return doc;
	}

	public void mkcalendar(String path, String template) {
		logger.info("MKCALENDAR {} with template {}", path, template);
		templateRequest("MKCALENDAR", path, template, null);
	}

	public void put(String path, String template) {
		templateRequest("PUT", path, template, null);
	}

	public Document report(String path, String template) {
		logger.info("REPORT {} with template {}", path, template);
		Document doc = templateRequest("REPORT", path, template, null);
		return doc;
	}

	public UserResources getCalendarLocation(String principal) {
		Element r = templateRequest("PROPFIND", principal, "propfind_principal_depth0.xml", Depth.ZERO)
				.getDocumentElement();
		UserResources cr = new UserResources();
		cr.setCalendarHome(href(r, NS.CALDAV, "calendar-home-set"));
		cr.setScheduleInbox(href(r, NS.CALDAV, "schedule-inbox-URL"));
		cr.setScheduleOutbox(href(r, NS.CALDAV, "schedule-outbox-URL"));
		cr.setDropbox(href(r, NS.CSRV_ORG, "dropbox-home-URL"));
		cr.setAddressbookHome(href(r, NS.CARDDAV, "addressbook-home-set"));
		return cr;
	}

	public String get(String href) {
		RequestBuilder theReq = req("GET", href);
		Response response = run(theReq.build());
		try {
			return response.getResponseBody();
		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	public List<String> listAddressbooks(UserResources res) {
		// osx server crashes when ONE_NOROOT is used
		Element r = templateRequest("PROPFIND", res.getAddressbookHome(), "propfind_list_books.xml", Depth.ONE)
				.getDocumentElement();
		NodeList responses = r.getElementsByTagNameNS(NS.WEBDAV, "response");
		int len = responses.getLength();
		List<String> books = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			Element resp = (Element) responses.item(i);
			int abType = resp.getElementsByTagNameNS(NS.CARDDAV, "addressbook").getLength();
			if (abType == 0) {
				continue;
			}
			NodeList nl = resp.getChildNodes();
			int cLen = nl.getLength();
			for (int j = 0; j < cLen; j++) {
				Node n = nl.item(j);
				if ("href".equals(n.getLocalName())) {
					books.add(n.getTextContent());
				}
			}
		}
		return books;
	}

	private String href(Element r, String ns, String local) {
		NodeList nl = r.getElementsByTagNameNS(ns, local);
		if (nl.getLength() == 0) {
			logger.warn("Missing {} {}", ns, local);
			return null;
		}
		Element hrefContainer = (Element) nl.item(0);
		nl = hrefContainer.getElementsByTagNameNS(NS.WEBDAV, "href");
		if (nl.getLength() == 0) {
			logger.warn("No href in {} {}", ns, local);
			return null;
		}
		String href = nl.item(0).getTextContent();
		return href;
	}

	public synchronized void close() {
		if (!closed) {
			closed = true;
			client.close();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

}
