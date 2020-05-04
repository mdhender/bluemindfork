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
package net.bluemind.ui.adminconsole.base.client;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainsAsync;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.NotificationPanel;
import net.bluemind.gwtconsoleapp.base.eventbus.GwtEventBus;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEvent;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEventHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHelper;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.HistToken;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.common.client.forms.Ajax;

public final class AdminScreen extends Composite {

	private RSStyle style;

	public interface RSBundle extends ClientBundle {
		@Source("AdminScreen.css")
		RSStyle getStyle();
	}

	public interface RSStyle extends CssResource {
		String sectionList();
	}

	private static AdminScreenUiBinder uiBinder = GWT.create(AdminScreenUiBinder.class);

	interface AdminScreenUiBinder extends UiBinder<DockLayoutPanel, AdminScreen> {
	}

	public static interface AdminScreenConstants extends Messages {
		String allDomain();

		String headingTitle();

		String headingTitleVersionned(String v);
	}

	private static final AdminScreenConstants constants = GWT.create(AdminScreenConstants.class);

	@UiField
	FlowPanel westPanel;

	@UiField
	DeckLayoutPanel centerPanel;

	@UiField
	ListBox domainSelector;

	@UiField
	Banner banner;

	@UiField(provided = true)
	BreadCrumb bc;

	private List<ItemValue<Domain>> domains;

	private List<SectionAnchorContainer> anchorContainers;

	private List<Section> sections;

	NotificationPanel notification;

	public static final RSBundle bundle = GWT.create(RSBundle.class);

	public void notifySectionAction(String screen) {
		for (SectionAnchorContainer menuContainer : anchorContainers) {
			menuContainer.notifySectionAction(screen);
		}
	}

	public AdminScreen(List<Section> sections, BreadCrumb bc) {
		this.style = bundle.getStyle();
		style.ensureInjected();
		this.sections = sections;
		this.bc = bc;
		initWidget(uiBinder.createAndBindUi(this));
		initWest(westPanel);
		initCenter(centerPanel);

		notification = new NotificationPanel();
		domainSelector.getElement().setId("domain-selector");

		registerNotificationHandler();

		Event.setEventListener(RootPanel.get().getElement(), new EventListener() {

			@Override
			public void onBrowserEvent(Event event) {
				if ("refresh-domains".equals(event.getType())) {
					loadDomains();
				}
			}
		});

		DOM.sinkBitlessEvent(RootPanel.get().getElement(), "refresh-domains");
		SubscriptionInfoHolder.get().init();
	}

	@UiHandler("domainSelector")
	void domainSelected(ChangeEvent e) {
		int selectedDomain = domainSelector.getSelectedIndex();
		ItemValue<Domain> d = domains.get(selectedDomain);
		DomainsHolder.get().setSelectedDomain(d);
		DefaultDomainHolder.set(d.uid);
		HistToken token = Actions.get().getHistoryToken();
		token.req.put("selectedDomainUid", domains.get(selectedDomain).uid);
		Actions.get().showWithParams2(token.screen, token.req);

	}

	@UiFactory
	SecurityContext getToken() {
		return Ajax.TOKEN;
	}

	public void initCenter(DeckLayoutPanel sp) {

	}

	private void initWest(FlowPanel sp) {
		anchorContainers = new LinkedList<SectionAnchorContainer>();
		FlexTable ft = new FlexTable();
		ft.setStyleName(style.sectionList());
		sp.setStyleName(style.sectionList());
		for (Section section : sections) {
			SectionAnchorContainer sac = new SectionAnchorContainer(section);
			anchorContainers.add(sac);
			ft.setWidget(ft.getRowCount(), 0, sac);
			sp.add(sac);
		}
		ft.setCellPadding(0);
		ft.setCellSpacing(0);
		ft.setBorderWidth(0);
		// sp.add(ft);
	}

	public void onScreenShown() {

		loadDomains();
		if (Ajax.TOKEN.getRoles().contains("GO_SUBSCRIPTION")) {
			Actions.get().show("subscription", null);
		}
	}

	private void loadDomains() {
		IDomainsAsync domains = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());

		GWT.log("user domain " + Ajax.TOKEN.getContainerUid());
		domains.all(new DefaultAsyncHandler<List<ItemValue<Domain>>>() {

			@Override
			public void success(List<ItemValue<Domain>> result) {
				GWT.log("Get domain list from core server");
				DomainsHolder.get().setDomains(result);
				constructListBox(result);
			}

		});
	}

	private void registerNotificationHandler() {
		notification = new NotificationPanel();
		GwtEventBus.bus.addHandler(NotificationEvent.TYPE, new NotificationEventHandler() {
			@Override
			public void onNotify(NotificationEvent event) {
				switch (event.notificationType) {
				case INFO:
					notification.showOk(event.message);
					break;
				case ERROR:
					notification.showError(event.message);
					break;
				case EXCEPTION:
					notification.showError(event.exception.getMessage());
				}
			}
		});
	}

	private void constructListBox(List<ItemValue<Domain>> domList) {
		Set<ItemValue<Domain>> domainSet = new TreeSet<>(new DomainComparator());
		domainSet.addAll(domList);

		this.domains = new ArrayList<ItemValue<Domain>>();
		this.domains.addAll(domainSet);

		domainSelector.clear();
		for (ItemValue<Domain> dom : domainSet) {
			String domainName = dom.value.name;
			if (domainName.endsWith(".internal") && !dom.value.aliases.isEmpty()) {
				domainName = dom.value.aliases.iterator().next();
			}
			domainSelector.addItem(DomainsHelper.getDisplayName(dom.value), dom.uid);
		}
		if (domList.size() == 1) {
			domainSelector.setEnabled(false);
		} else {
			domainSelector.setVisible(true);
		}
		DomainsHolder.get().setDomains(domList);

		if (DefaultDomainHolder.get(domains) != null) {
			ItemValue<Domain> selectedDomain = DefaultDomainHolder.get(domains);
			DomainsHolder.get().setSelectedDomain(selectedDomain);
			domainSelector.setSelectedIndex(domains.indexOf(selectedDomain));
		} else {
			DomainsHolder.get().setSelectedDomain(domains.get(0));
		}
	}

	@UiFactory
	AdminScreenConstants getTexts() {
		return constants;
	}

	@UiFactory
	AdminIcons getIcons() {
		return AdminIcons.INST;
	}
}
