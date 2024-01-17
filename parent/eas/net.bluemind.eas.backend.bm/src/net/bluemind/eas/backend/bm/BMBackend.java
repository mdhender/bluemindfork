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
package net.bluemind.eas.backend.bm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IContentsExporter;
import net.bluemind.eas.backend.IContentsImporter;
import net.bluemind.eas.backend.IHierarchyExporter;
import net.bluemind.eas.backend.IHierarchyImporter;
import net.bluemind.eas.backend.bm.calendar.CalendarBackend;
import net.bluemind.eas.backend.bm.calendar.CalendarsNotificationHandler;
import net.bluemind.eas.backend.bm.contacts.ContactNotificationHandler;
import net.bluemind.eas.backend.bm.contacts.ContactsBackend;
import net.bluemind.eas.backend.bm.mail.FolderNotificationHandler;
import net.bluemind.eas.backend.bm.mail.MailBackend;
import net.bluemind.eas.backend.bm.mail.NewMailNotificationHandler;
import net.bluemind.eas.backend.bm.state.InternalState;
import net.bluemind.eas.backend.bm.task.TaskBackend;
import net.bluemind.eas.backend.bm.task.TaskNotificationHandler;
import net.bluemind.eas.backend.bm.user.UserBackend;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.hornetq.client.Consumer;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.network.topology.Topology;
import net.bluemind.vertx.common.http.BasicAuthHandler;

public class BMBackend implements IBackend {

	private final IHierarchyImporter hImporter;
	private final IContentsImporter cImporter;
	private final IHierarchyExporter exporter;
	private final IContentsExporter contentsExporter;
	private final ISyncStorage store;
	private final UserBackend userBackend;

	private Consumer mailPushConsumer;
	private Consumer imapNotificationsConsumer;
	private Consumer calPushConsumer;
	private Consumer contactPushConsumer;
	private Consumer taskPushConsumer;
	private static final Logger logger = LoggerFactory.getLogger(BMBackend.class);

	public BMBackend(ISyncStorage store) {

		FolderBackend folderBackend = new FolderBackend(store);
		MailBackend mailBackend = new MailBackend(store);
		CalendarBackend calendarBackend = new CalendarBackend(store);
		ContactsBackend contactsBackend = new ContactsBackend(store);
		TaskBackend taskBackend = new TaskBackend(store);
		this.store = store;
		this.userBackend = new UserBackend();

		hImporter = new HierarchyImporter(folderBackend);
		exporter = new HierarchyExporter(folderBackend);
		cImporter = new ContentsImporter(mailBackend, calendarBackend, contactsBackend, taskBackend);
		contentsExporter = new ContentsExporter(mailBackend, calendarBackend, contactsBackend, taskBackend);

		MQ.init(new MQ.IMQConnectHandler() {

			@Override
			public void connected() {
				setupFolderPush();
				setupMailPush();
				setupCalPush();
				setupContactPush();
				setupTaskPush();
			}
		});
		;
	}

	private void setupMailPush() {
		if (mailPushConsumer != null) {
			return;
		}
		mailPushConsumer = MQ.registerConsumer(Topic.MAPI_ITEM_NOTIFICATIONS, new NewMailNotificationHandler(store));

	}

	private void setupFolderPush() {
		if (imapNotificationsConsumer != null) {
			return;
		}
		imapNotificationsConsumer = MQ.registerConsumer(Topic.MAPI_HIERARCHY_NOTIFICATIONS,
				new FolderNotificationHandler());
	}

	private void setupCalPush() {
		if (calPushConsumer != null) {
			return;
		}
		calPushConsumer = MQ.registerConsumer(Topic.CALENDAR_NOTIFICATIONS, new CalendarsNotificationHandler(store));
	}

	private void setupContactPush() {
		if (contactPushConsumer != null) {
			return;
		}
		contactPushConsumer = MQ.registerConsumer(Topic.CONTACT_NOTIFICATIONS, new ContactNotificationHandler(store));
	}

	private void setupTaskPush() {
		if (taskPushConsumer != null) {
			return;
		}
		taskPushConsumer = MQ.registerConsumer(Topic.TASK_NOTIFICATIONS, new TaskNotificationHandler(store));
	}

	@Override
	public IHierarchyImporter getHierarchyImporter(BackendSession bs) {
		return hImporter;
	}

	@Override
	public IHierarchyExporter getHierarchyExporter(BackendSession bs) {
		return exporter;
	}

	@Override
	public IContentsImporter getContentsImporter(BackendSession bs) {
		return cImporter;
	}

	@Override
	public IContentsExporter getContentsExporter(BackendSession bs) {
		return contentsExporter;
	}

	@Override
	public MSUser getUser(String loginAtDomain, String password) throws ActiveSyncException {
		return userBackend.getUser(loginAtDomain, password);
	}

	@Override
	public String getPictureBase64(BackendSession bs, int photoId) {
		return userBackend.getPictureBase64(bs, photoId);
	}

	@Override
	public void acknowledgeRemoteWipe(BackendSession bs) {
		logger.info("Client '{}' acknowledges RemoteWipe", bs.getDeviceId().getIdentifier());
		// TODO do something (send email to admin? device owner?)
	}

	@Override
	public void initInternalState(BackendSession bs) {
		logger.debug("Set internal state");

		InternalState is = new InternalState();
		is.coreUrl = "http://" + Topology.get().core().value.address() + ":8090";
		is.sid = bs.getSid();

		bs.setInternalState(is);
	}

	@Override
	public void purgeSessions() {
		logger.info("bm-core (re)started. Purge backend sessions.");
		BasicAuthHandler.purgeSessions();
		UserBackend.purgeSession();
	}

}
