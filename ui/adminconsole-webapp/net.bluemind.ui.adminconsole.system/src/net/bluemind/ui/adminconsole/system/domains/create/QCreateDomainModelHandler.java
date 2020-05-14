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
package net.bluemind.ui.adminconsole.system.domains.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.RootPanel;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainsPromise;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.group.api.IGroupPromise;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.gwt.endpoint.GroupGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.IServerPromise;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.server.api.gwt.js.JsServer;
import net.bluemind.server.api.gwt.serder.ServerGwtSerDer;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.IDomainTemplatePromise;
import net.bluemind.system.api.gwt.endpoint.DomainTemplateGwtEndpoint;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.IUserPromise;
import net.bluemind.user.api.User;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public class QCreateDomainModelHandler implements IGwtModelHandler {

	protected static final String TAG_MAIL = "mail/";
	public static final String TYPE = "bm.ac.QCreateDomainModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateDomainModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateDomainModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		IServerPromise serverService = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default").promiseApi();
		serverService.allComplete().thenAccept((servers) -> {
			JsArray<JsItemValue<JsServer>> list = JsArray.createArray().cast();

			for (ItemValue<Server> server : servers) {

				if (server.value.tags.stream().filter(tag -> tag.startsWith(TAG_MAIL)).count() > 0) {

					JsItemValue<JsServer> jsServer = new ItemValueGwtSerDer<>(new ServerGwtSerDer()).serialize(server)
							.isObject().getJavaScriptObject().cast();
					list.push(jsServer);
				}
			}
			map.put(HostKeys.servers.name(), list);
			handler.success(null);
		}).exceptionally(e -> {
			handler.failure(e);
			return null;
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final AsyncHandler<Void> wrappedHandler = new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				RootPanel.get().getElement()
						.dispatchEvent(Document.get().createHtmlEvent("refresh-domains", true, true));
				handler.success(null);
			}

			@Override
			public void failure(Throwable e) {
				handler.failure(e);
			}

		};

		final JsMapStringJsObject map = model.cast();
		QCreateDomainModel dmodel = map.getObject("domainModel");
		Domain domain = new Domain();
		domain.name = dmodel.name;
		domain.label = dmodel.name;
		domain.aliases = new HashSet<>(Arrays.asList(dmodel.domainAlias));

		if (dmodel.createAdmin && !checkAdminUser(wrappedHandler, dmodel)) {
			return;
		}

		IDomainsPromise service = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();

		service.create(dmodel.domainUid, domain).thenCompose((v) -> {
			return CompletableFuture.allOf(JsHelper.asList(dmodel.selectedServer.getValue().getTags()).stream()
					.map(tag -> tagServer(dmodel.selectedServer.getUid(), dmodel.domainUid, tag))
					.toArray(c -> new CompletableFuture[c]));
		}).thenCompose((v) -> autoAssignDefaultServices(dmodel.domainUid)).thenCompose((v) -> {
			if (dmodel.createAdmin) {
				return addAdminUser(dmodel);
			} else {
				return CompletableFuture.completedFuture(null);
			}
		}).thenAccept((v) -> {
			wrappedHandler.success(null);
		}).exceptionally(e -> {
			wrappedHandler.failure(e);
			return null;
		});

	}

	private static final Set<String> banned = skipTags();

	private static Set<String> skipTags() {
		Set<String> ret = new HashSet<>();
		ret.addAll(Arrays.asList("mail/imap", "bm/pgsql-data", "mail/smtp-edge"));
		return ret;
	}

	private CompletableFuture<Void> autoAssignDefaultServices(String domainUid) {

		IDomainTemplatePromise dt = new DomainTemplateGwtEndpoint(Ajax.TOKEN.getSessionId()).promiseApi();
		IServerPromise servers = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default").promiseApi();
		CompletableFuture<List<ItemValue<Server>>> allServersProm = servers.allComplete();
		CompletableFuture<Set<String>> toSetProm = dt.getTemplate().thenApply(template -> {
			Set<String> toApply = new LinkedHashSet<>();
			toApply.add("bm/core");
			for (DomainTemplate.Kind kind : template.kinds) {
				for (DomainTemplate.Tag tag : kind.tags) {
					if (tag.autoAssign && !banned.contains(tag.value)) {
						toApply.add(tag.value);
					}
				}
			}
			return toApply;
		});
		return toSetProm.thenCombine(allServersProm, (Set<String> tagsToApply, List<ItemValue<Server>> allServers) -> {
			CompletableFuture<Void> root = CompletableFuture.completedFuture(null);
			for (ItemValue<Server> srv : allServers) {
				for (String srvTag : srv.value.tags) {
					if (tagsToApply.contains(srvTag)) {
						root = root.thenCompose(v -> tagServer(srv.uid, domainUid, srvTag));
					}
				}
			}

			return root;
		}).thenApply(v -> (Void) null);

	}

	private CompletableFuture<Void> tagServer(String serveruid, String domainuid, String tag) {
		IServerPromise servers = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default").promiseApi();
		Notification.get().reportInfo("tagging server " + serveruid + " @domain " + domainuid + " with tag: " + tag);
		return servers.assign(serveruid, domainuid, tag);
	}

	protected CompletableFuture<Void> addAdminUser(QCreateDomainModel model) {

		IUserPromise users = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), model.domainUid).promiseApi();

		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		final User user = new User();
		user.login = model.adminLogin;
		user.password = model.adminPassword;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name = Name.create(model.adminLogin, null, null, null, null, null);
		user.dataLocation = model.selectedServer.getUid();
		IGroupPromise groups = new GroupGwtEndpoint(Ajax.TOKEN.getSessionId(), model.domainUid).promiseApi();
		return users.create(uid, user).thenCompose((v) -> {
			return groups.byName("admin");
		}).thenCompose((v) -> {
			List<Member> member = new ArrayList<>();
			member.add(Member.user(uid));
			return groups.add(v.uid, member);
		}).thenAccept((v) -> {
			Notification.get().reportInfo("Successfully created admin user " + user.login);
		});
	}

	private boolean checkAdminUser(AsyncHandler<Void> wrappedHandler, QCreateDomainModel model) {
		if (model.adminLogin == null || model.adminLogin.isEmpty()) {
			wrappedHandler.failure(new RuntimeException(DomainConstants.INST.invalidAdminLogin()));
			return false;
		}

		if (model.adminPassword == null || model.adminPassword.isEmpty()) {
			wrappedHandler.failure(new RuntimeException(DomainConstants.INST.invalidAdminPassword()));
			return false;
		}

		return true;
	}

}
