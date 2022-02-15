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
package net.bluemind.ui.adminconsole.dataprotect;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;

import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.GenerationContent;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.gwt.endpoint.DataProtectGwtEndpoint;
import net.bluemind.dataprotect.api.gwt.serder.GenerationContentGwtSerDer;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;
import net.bluemind.ui.adminconsole.progress.ui.ProgressScreen;

public class GenerationBrowser extends Composite implements IGwtScreenRoot {

	interface GenerationBrowserUi extends UiBinder<HTMLPanel, GenerationBrowser> {

	}

	private static final GenerationBrowserUi binder = GWT.create(GenerationBrowserUi.class);

	public static final String TYPE = "bm.ac.DPGenerationBrowser";

	private HTMLPanel panel;

	@UiField
	Label genInfos;

	@UiField
	PushButton back;

	@UiField
	ToggleButton domainsToggle;

	@UiField
	ToggleButton usersToggle;

	@UiField
	ToggleButton ouToggle;

	@UiField
	ToggleButton mailsharesToggle;

	@UiField
	TextBox searchQuery;

	@UiField
	PushButton find;

	@UiField
	RestorablesTable restorables;

	private int activeFilters;

	private int generationId;

	private String version;

	private GenerationContent content;

	private ScreenRoot instance;

	private static String cachedGenContent;

	public GenerationBrowser(ScreenRoot instance) {
		this.instance = instance;
		this.panel = binder.createAndBindUi(this);
		initWidget(panel);
	}

	protected void onScreenShown(ScreenShowRequest ssr) {
		String mode = (String) ssr.get("mode");
		String genContentString = null;
		if ("restore".equals(mode)) {
			genContentString = getProgressOutput("genContentString");
		} else {
			String taskId = (String) ssr.get("resultId");
			genContentString = getProgressOutput(taskId);
		}

		GenerationContentGwtSerDer gcSerDes = new GenerationContentGwtSerDer();
		JSONValue value = JSONParser.parseStrict(genContentString);
		GenerationContent fromReq = null;
		try {
			fromReq = gcSerDes.deserialize(value);
		} catch (Throwable t) {
			GWT.log(t.getMessage(), t);
		}

		if (fromReq == null) {
			GWT.log("No generation specified");
			return;
		}
		generationId = fromReq.generationId;
		GWT.log("[" + generationId + "] Parsed: " + fromReq + ", domains: " + fromReq.domains.size());
		content = fromReq;

		DataProtectGwtEndpoint dpEndpoint = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpEndpoint.getAvailableGenerations(new DefaultAsyncHandler<List<DataProtectGeneration>>() {

			@Override
			public void success(List<DataProtectGeneration> value) {
				for (DataProtectGeneration dataProtectGeneration : value) {
					if (generationId == dataProtectGeneration.id) {
						version = dataProtectGeneration.blueMind.displayName;
						setInfosLabel();
						break;
					}
				}

			}
		});
	}

	private String getProgressOutput(String key) {
		if (ProgressScreen.hasResult(key)) {
			cachedGenContent = ProgressScreen.getResult(key);
			return cachedGenContent;
		} else {
			return cachedGenContent;
		}
	}

	private void filterToggled(boolean activated) {
		if (activated) {
			activeFilters++;
		} else {
			activeFilters--;
		}
		find();
	}

	@UiHandler("domainsToggle")
	void domainsToggled(ClickEvent ce) {
		filterToggled(domainsToggle.isDown());
	}

	@UiHandler("usersToggle")
	void usersToggled(ClickEvent ce) {
		filterToggled(usersToggle.isDown());
	}

	@UiHandler("ouToggle")
	void ouToggled(ClickEvent ce) {
		filterToggled(ouToggle.isDown());
	}

	@UiHandler("mailsharesToggle")
	void mailsharesToggled(ClickEvent ce) {
		filterToggled(mailsharesToggle.isDown());
	}

	@UiHandler("back")
	void backClicked(ClickEvent ce) {
		Actions.get().show("dpNavigator", new ScreenShowRequest());
	}

	@UiHandler("find")
	void findClicked(ClickEvent ce) {
		find();
	}

	@UiHandler("searchQuery")
	void enterPressed(KeyPressEvent kpe) {
		if (kpe.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
			find();
		}
	}

	private void find() {
		String q = searchQuery.getText().trim().toLowerCase();
		GWT.log("find: '" + q + "', activeFilters: " + activeFilters);
		restorables.setCapabilities(content.capabilities);
		restorables.setGeneration(generationId);
		restorables.setContent(content);

		LinkedList<Restorable> matches = new LinkedList<Restorable>();
		int limit = 500;

		if (activeFilters == 0 || usersToggle.isDown() || mailsharesToggle.isDown() || ouToggle.isDown()) {
			for (ItemValue<DirEntry> de : content.entries) {
				if (matches.size() >= limit) {
					break;
				}
				DirEntry entry = de.value;
				Kind det = entry.kind;

				if (det != Kind.USER && det != Kind.MAILSHARE && det != Kind.ORG_UNIT) {
					continue;
				}
				String path = entry.path;
				GWT.log("On entry [" + entry.entryUid + "] of type " + det.name() + " with path " + path);

				int idx = path.indexOf('/');
				String domainUid = path.substring(0, idx);

				if (det == Kind.ORG_UNIT && (activeFilters == 0 || ouToggle.isDown())) {
					if (matches(entry, q)) {
						matches.add(Restorable.create(domainUid, entry));
					}
				}
				if (det == Kind.USER && (activeFilters == 0 || usersToggle.isDown())) {
					if (matches(entry, q)) {
						matches.add(Restorable.create(domainUid, entry));
					}
				}
				if (det == Kind.MAILSHARE && (activeFilters == 0 || mailsharesToggle.isDown())) {
					if (matches(entry, q)) {
						matches.add(Restorable.create(domainUid, entry));
					}
				}
			}
		}
		if (!DomainsHolder.get().getSelectedDomain().uid.equals("global.virt")) {
			String domain = DomainsHolder.get().getSelectedDomain().uid;
			Iterator<Restorable> iter = matches.iterator();
			while (iter.hasNext()) {
				Restorable rest = iter.next();
				switch (rest.kind) {
				case DOMAIN:
					if (!rest.entryUid.equals(domain)) {
						iter.remove();
					}
					break;
				default:
					if (!rest.domainUid.equals(domain)) {
						iter.remove();
					}
					break;
				}
			}
		}
		GWT.log("Search matched " + matches.size() + " entries.");
		restorables.setValues(matches);
	}

	private boolean matches(DirEntry de, String q) {
		if (q.isEmpty()) {
			return true;
		}
		return de.displayName.toLowerCase().contains(q);
	}

	private boolean matches(Domain d, String q) {
		if (q.isEmpty()) {
			return true;
		}
		return d.name.toLowerCase().contains(q);
	}

	public void setInfosLabel() {
		int users = 0;
		int mailshares = 0;
		int ous = 0;
		for (ItemValue<DirEntry> de : content.entries) {
			Kind det = de.value.kind;
			switch (det) {
			case ADDRESSBOOK:
				break;
			case CALENDAR:
				break;
			case DOMAIN:
				break;
			case GROUP:
				break;
			case MAILSHARE:
				mailshares++;
				break;
			case RESOURCE:
				break;
			case USER:
				users++;
				break;
			case ORG_UNIT:
				ous++;
				break;
			default:
				break;

			}
		}
		genInfos.setText(texts().genInfos(version, content.domains.size(), users, mailshares, ous));
		find();
	}

	@UiFactory
	DPTexts texts() {
		return DPTexts.INST;
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new GenerationBrowser(screenRoot);
			}
		});

	}

	@Override
	public void attach(Element e) {
		DOM.appendChild(e, getElement());
		JsMapStringString request = instance.getState();
		JsArrayString keus = request.keys();
		ScreenShowRequest asSSR = new ScreenShowRequest();
		for (int i = 0; i < keus.length(); i++) {
			String k = keus.get(i);
			String v = request.get(k);
			asSSR.put(k, v);
			GWT.log(" * '" + k + "' => '" + v + "'");
		}
		onScreenShown(asSSR);
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doLoad(ScreenRoot instance) {
		// TODO Auto-generated method stub

	}

}
