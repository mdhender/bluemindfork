package net.bluemind.dav.server.proto.mkcalendar;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.dav.server.proto.IDavProtocol;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.vertx.common.Body;

public class MkCalendarProtocol implements IDavProtocol<MkCalQuery, MkCalResponse> {

	private static final Logger logger = LoggerFactory.getLogger(MkCalendarProtocol.class);

	@Override
	public void parse(final HttpServerRequest r, final DavResource davRes, final Handler<MkCalQuery> handler) {
		Body.handle(r, new Handler<Buffer>() {
			@Override
			public void handle(Buffer b) {
				logReq(r, b);
				MkCalQuery q = new MkCalQueryParser().parse(davRes, r.headers(), b);
				handler.handle(q);
			}
		});
	}

	@Override
	public void execute(LoggedCore lc, MkCalQuery query, Handler<MkCalResponse> handler) {
		MkCalResponse resp = new MkCalResponse(query.getPath());
		try {
			IContainers contApi = lc.getCore().instance(IContainers.class);
			Matcher m = ResType.VSTUFF_CONTAINER.matcher(query.getPath());
			m.find();
			String uid = m.group(2);
			ContainerDescriptor cd = ContainerDescriptor.create(uid, query.displayName, query.getResource().getUid(),
					query.kind.containerType, lc.getDomain(), false);
			contApi.create(uid, cd);
			IContainerManagement mgmtApi = lc.getCore().instance(IContainerManagement.class, uid);
			mgmtApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(lc.getUser().uid, Verb.All)));

			IUserSubscription userSubService = lc.getCore().instance(IUserSubscription.class, lc.getDomain());
			userSubService.subscribe(lc.getUser().uid, Arrays.asList(ContainerSubscription.create(uid, false)));

			logger.info("Created {} container {}, set acls and subscribe", query.kind.containerType, uid);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		handler.handle(resp);
	}

	/**
	 * <pre>
	2015-08-10 09:42:38,561 [main] n.b.d.s.t.DavClient INFO - MKCALENDAR /calendars/__uids__/CE783F05-AA04-4F90-AF3E-1A8C3C3D9976/tasks_todolist:ju1439192558561/ with template mkcalendar_vtodo.xml
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: 201 Created in 133ms.
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER Content-Length: 0
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER Strict-Transport-Security: max-age=604800
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER Server: Twisted/13.2.0 TwistedWeb/9.0.0
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER DAV: 1, access-control, calendar-access, calendar-schedule, calendar-auto-schedule, calendar-availability, inbox-availability, calendar-proxy, calendarserver-private-events, calendarserver-private-comments, calendarserver-sharing, calendarserver-sharing-no-scheduling, calendar-query-extended, calendar-default-alarms, calendar-managed-attachments, calendarserver-partstat-changes, calendar-no-timezone, calendarserver-recurrence-split, addressbook, extended-mkcol, calendarserver-principal-property-search, calendarserver-principal-search, calendarserver-home-sync
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER ETag: "5158851f3da2cbec0520a2a32104f132"
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER Date: Mon, 10 Aug 2015 07:42:38 GMT
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER Last-Modified: Mon, 10 Aug 2015 07:42:38 GMT
	2015-08-10 09:42:38,698 [main] n.b.d.s.t.DavClient INFO - S[3]: HEADER Connection: close
	 * </pre>
	 */
	@Override
	public void write(MkCalResponse response, HttpServerResponse sr) {
		// 403 (Forbidden) - This indicates at least one of two conditions:
		// 1) the server does not allow the creation of calendar collections
		// at the given location in its namespace, or 2) the parent
		// collection of the Request-URI exists but cannot accept members;

		logger.error("Sending 201");
		sr.setStatusCode(201).setStatusMessage("Created.").end();
	}

	private void logReq(HttpServerRequest r, Buffer body) {
		logger.error("{} {}", r.method(), r.path());
		for (String hn : r.headers().names()) {
			logger.error("{}: {}", hn, r.headers().get(hn));
		}
		if (body != null) {
			logger.error("parse '{}'\n{}", r.path(), body.toString());
		} else {
			logger.error("parse '{}' q:'{}'", r.path(), r.query());
		}
	}
}
