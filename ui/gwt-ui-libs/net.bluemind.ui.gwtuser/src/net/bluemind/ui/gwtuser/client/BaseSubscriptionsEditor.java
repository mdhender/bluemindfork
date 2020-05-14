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
package net.bluemind.ui.gwtuser.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementSockJsEndpoint;
import net.bluemind.core.container.api.gwt.js.JsContainerSubscription;
import net.bluemind.core.container.api.gwt.serder.ContainerSubscriptionGwtSerDer;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.SwitchButton;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.ui.gwtuser.client.l10n.SubscriptionConstants;
import net.bluemind.user.api.IUserSubscriptionAsync;
import net.bluemind.user.api.gwt.endpoint.UserSubscriptionGwtEndpoint;

public abstract class BaseSubscriptionsEditor extends CompositeGwtWidgetElement {

	private final int CAL_MAX_ITEMS = 9000; // OVER 9000
	private final int AB_MAX_ITEMS = 1000;
	private final int TASK_MAX_ITEMS = 9000; // OVER 9000

	private int maxItemsLimit = 1000;

	@UiField(provided = true)
	ContainerAutoComplete autocomplete;

	@UiField
	FlexTable table;

	@UiField
	Label noSubscription;

	private static FolderEditUiBinder uiBinder = GWT.create(FolderEditUiBinder.class);

	interface FolderEditUiBinder extends UiBinder<HTMLPanel, BaseSubscriptionsEditor> {
	}

	public static interface Resources extends ClientBundle {

		@Source("BookSubscription.css")
		Style editStyle();

	}

	private static final Resources res = GWT.create(Resources.class);

	public static interface Style extends CssResource {

		String container();

		String item();

		String trash();

		String itemCountWarning();

		String itemCountWarningDialogContent();

		String warningIcon();

	}

	private final Style s;
	private Map<String, SwitchButton> subscription;

	private HTMLPanel htmlPanel;

	protected String userId;
	protected String domainUid;
	private String modelId;

	private DirectoryGwtEndpoint directory;

	private ContainerFinder containerFinder;

	private String type;

	public BaseSubscriptionsEditor(String containerType) {
		type = containerType;
		this.modelId = containerType + "-subscription";
		containerFinder = new ContainerFinder(containerType);
		autocomplete = new ContainerAutoComplete(getAddLabel(), containerFinder);

		s = res.editStyle();
		s.ensureInjected();

		subscription = new HashMap<String, SwitchButton>();

		htmlPanel = uiBinder.createAndBindUi(this);

		table.setVisible(false);
		noSubscription.setVisible(false);

		autocomplete.setTarget(new IContainerSelectTarget() {

			@Override
			public void seletected(ContainerDescriptor desc) {
				if (!subscription.containsKey(desc.uid)) {
					desc.offlineSync = true;
					CompletableFuture.allOf(addEntry(desc, true));
				}
			}
		});

		table.setStyleName(s.container());

		if ("calendar".equals(containerType)) {
			maxItemsLimit = CAL_MAX_ITEMS;
		} else if ("addressbook".equals(containerType)) {
			maxItemsLimit = AB_MAX_ITEMS;
		} else if ("todolist".equals(containerType)) {
			maxItemsLimit = TASK_MAX_ITEMS;
		}

		initWidget(htmlPanel);
	}

	abstract protected String getAddLabel();

	public void setDomainUid(String domainUid) {
		this.domainUid = domainUid;
		directory = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		containerFinder.setDomain(domainUid);
	}

	public void setUserId(String userid) {
		this.userId = userid;
		containerFinder.setUserUid(userId);
	}

	public void addEntry(List<ContainerSubscriptionDescriptor> containers) {
		List<CompletableFuture<Count>> entries = new ArrayList<>();

		for (ContainerSubscriptionDescriptor cd : containers) {
			entries.add(addEntry(cd, cd.offlineSync));
		}

		CompletableFuture.allOf(entries.toArray(new CompletableFuture[0]));
	}

	public CompletableFuture<Count> addEntry(ContainerDescriptor f, boolean isNew) {
		return addEntry(ContainerSubscriptionDescriptor.create(f, true), isNew);
	}

	public CompletableFuture<Count> addEntry(ContainerSubscriptionDescriptor f, boolean isNew) {

		IContainerManagementPromise service = new ContainerManagementSockJsEndpoint(Ajax.TOKEN.getSessionId(),
				f.containerUid).promiseApi();

		CompletableFuture<Count> ret = service.getItemCount().thenApply(value -> {
			hasSubscription(true);

			final String key = f.containerUid;
			int row = table.getRowCount();
			int i = 0;
			Trash trash = null;

			if (canUnsubscribe(f)) {

				trash = new Trash();
				trash.setId("book-management-entry-trash-" + key);
				trash.addClickHandler(new ClickHandler() {
					@Override
					public void onClick(ClickEvent event) {
						Cell c = table.getCellForEvent(event);
						table.removeRow(c.getRowIndex());
						subscription.remove(key);
						hasSubscription(table.getRowCount() > 0);
					}
				});

			}
			String label = getLabel(f, null);
			final Label displayName = new Label(label);
			if (directory != null && f.owner != null) {
				directory.findByEntryUid(f.owner, new DefaultAsyncHandler<DirEntry>() {

					@Override
					public void success(DirEntry value) {
						if (value != null) {
							displayName.setText(getLabel(f, value.displayName));
						}
					}
				});
			}
			table.setWidget(row, i++, displayName);

			Label warningIcon = new Label();
			warningIcon.setStyleName("fa fa-lg fa-exclamation-circle");
			warningIcon.addStyleName(s.itemCountWarning());
			warningIcon.setTitle(SubscriptionConstants.INST.tooFat(value.total));
			warningIcon.setVisible(false);
			table.getCellFormatter().addStyleName(row, i, s.warningIcon());
			table.setWidget(row, i++, warningIcon);

			SwitchButton sb = new SwitchButton(key, f.offlineSync, SubscriptionConstants.INST.on(),
					SubscriptionConstants.INST.off());

			sb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

				@Override
				public void onValueChange(ValueChangeEvent<Boolean> event) {
					if (event.getValue() && value.total > maxItemsLimit) {
						warningIcon.setVisible(true);
						fatContainerWarning(value.total, sb, warningIcon);
					} else {
						warningIcon.setVisible(false);
					}
				}

			});

			if (f.offlineSync && value.total > maxItemsLimit) {
				// danger danger, high voltage
				warningIcon.setVisible(true);
				if (isNew) {
					fatContainerWarning(value.total, sb, warningIcon);
				}
			}

			table.setWidget(row, i++, sb);

			if (trash != null) {
				table.setWidget(row, i, trash);
				table.getCellFormatter().addStyleName(row, i++, s.trash());
			} else {
				table.setWidget(row, i, new Label());
			}

			subscription.put(key, sb);
			table.getRowFormatter().setStyleName(row, s.item());

			return value;
		});

		return ret;

	}

	protected boolean canUnsubscribe(ContainerSubscriptionDescriptor f) {
		return !(f.owner != null && f.owner.equals(userId) && f.defaultContainer);
	}

	/**
	 * @param items
	 * @param sb
	 */
	private void fatContainerWarning(int items, SwitchButton sb, Label warningIcon) {
		DialogBox os = new DialogBox();

		FlowPanel buttons = new FlowPanel();
		Button ok = new Button(SubscriptionConstants.INST.yes());
		ok.addStyleName("button");
		ok.addStyleName("primary");
		ok.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				os.hide();
			}
		});

		Button cancel = new Button(SubscriptionConstants.INST.no());
		cancel.addStyleName("button");
		cancel.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				os.hide();
				sb.setValue(false);
				warningIcon.setVisible(false);
			}
		});

		buttons.add(ok);
		buttons.add(cancel);
		buttons.getElement().getStyle().setPadding(5, Unit.PX);

		DockLayoutPanel dlp = new DockLayoutPanel(Unit.PX);
		dlp.setHeight("150px");
		dlp.setWidth("500px");

		Label warn = new Label(SubscriptionConstants.INST.tooFatWarningTitle());
		warn.addStyleName("modal-dialog-title");
		dlp.addNorth(warn, 40);

		Label content = new Label(SubscriptionConstants.INST.tooFatWarningContent(items));
		content.addStyleName(s.itemCountWarningDialogContent());
		dlp.addSouth(buttons, 40);

		dlp.add(content);

		os.addStyleName("dialog");
		os.getElement().setAttribute("style", "padding:0");
		os.setWidget(dlp);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(false);
		os.setGlassStyleName("modalOverlay");
		os.setModal(true);
		os.center();
		os.show();
	}

	abstract protected String getLabel(ContainerSubscriptionDescriptor f, String ownerDisplayName);

	private void hasSubscription(boolean hasSubscription) {
		table.setVisible(hasSubscription);
		noSubscription.setVisible(!hasSubscription);
	}

	public void setValue() {
		initValue();
	}

	private void initValue() {
		table.removeAllRows();

		IUserSubscriptionAsync service = new UserSubscriptionGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		service.listSubscriptions(userId, type, new DefaultAsyncHandler<List<ContainerSubscriptionDescriptor>>() {

			@Override
			public void success(List<ContainerSubscriptionDescriptor> value) {
				Collections.sort(value, new Comparator<ContainerSubscriptionDescriptor>() {

					@Override
					public int compare(ContainerSubscriptionDescriptor o1, ContainerSubscriptionDescriptor o2) {
						if (userId.equals(o1.owner) && userId.equals(o2.owner)) {
							return o1.name.compareTo(o2.name);
						} else if (userId.equals(o1.owner) && !userId.equals(o2.owner)) {
							return -1;
						} else if (!userId.equals(o1.owner) && userId.equals(o2.owner)) {
							return 1;
						} else {
							return o1.name.compareTo(o2.name);
						}

					}
				});

				addEntry(value);
			}
		});

	}

	@Override
	public void loadModel(JavaScriptObject model) {

		JsMapStringJsObject m = model.cast();

		if (m.get("domainUid") != null) {
			setDomainUid(m.getString("domainUid"));
		}

		if (m.get("userId") != null) {
			setUserId(m.getString("userId"));
		}
		setValue();
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		List<ContainerSubscription> l = new ArrayList<ContainerSubscription>();
		for (SwitchButton value : subscription.values()) {
			ContainerSubscription cs = ContainerSubscription.create(value.getName(), value.getValue());
			l.add(cs);
		}

		JsArray<JsContainerSubscription> csList = new GwtSerDerUtils.ListSerDer<>(new ContainerSubscriptionGwtSerDer())
				.serialize(l).isArray().getJavaScriptObject().cast();

		JsMapStringJsObject m = model.cast();
		m.put(modelId, csList);
	}

}
