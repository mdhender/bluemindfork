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
package net.bluemind.ui.adminconsole.directory.user;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.gwt.endpoint.DeviceGwtEndpoint;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.eas.api.Account;
import net.bluemind.eas.api.gwt.endpoint.EasGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.user.api.gwt.js.JsUser;

public class DevicePanel extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.DevicesEditor";

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new DevicePanel();
			}
		});
		GWT.log("bm.ac.DevicesEditor registred");
	}

	private static DevicePanelUiBinder uiBinder = GWT.create(DevicePanelUiBinder.class);

	interface DevicePanelUiBinder extends UiBinder<FlowPanel, DevicePanel> {
	}

	public static interface Resources extends ClientBundle {

		@Source("DevicePanel.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String container();

		String header();

		String row();

		String inactive();

		String icon();

		String partnership();

		String action();

		String refreshList();

	}

	@UiField
	FlexTable deviceList;

	@UiField
	Anchor refreshList;

	private static final Resources res = GWT.create(Resources.class);

	private final Style s;
	private DirEntry user;

	private boolean easSyncUnknown;

	protected DevicePanel() {
		s = res.editStyle();
		s.ensureInjected();
		initWidget(uiBinder.createAndBindUi(this));
		deviceList.setStyleName(s.container());

		refreshList.setStyleName(s.refreshList());
		refreshList.getElement().setId("device-panel-refresh-list");
		refreshList.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				setDisplay(user, easSyncUnknown);

			}
		});
	}

	/**
	 * 
	 */
	public void addDeviceRowHeader() {
		int idx = 0;
		deviceList.setWidget(0, idx++, new Label(UserConstants.INST.partnership()));
		deviceList.setWidget(0, idx++, new Label(UserConstants.INST.identity()));
		deviceList.setWidget(0, idx++, new Label(UserConstants.INST.type()));
		deviceList.setWidget(0, idx++, new Label(UserConstants.INST.lastSync()));
		deviceList.setWidget(0, idx++, new Label());
		deviceList.setWidget(0, idx++, new Label());
		deviceList.setWidget(0, idx++, new Label());
		deviceList.setWidget(0, idx++, new Label());
		deviceList.setWidget(0, idx++, new Label());
		deviceList.getRowFormatter().setStyleName(0, s.header());
	}

	public void addDeviceRow(final ItemValue<Device> d) {

		int row = deviceList.getRowCount();
		String lastSync = "-";
		if (d.value.lastSync != null) {
			lastSync = DateTimeFormat.getFormat(PredefinedFormat.DATE_FULL).format(d.value.lastSync) + " "
					+ DateTimeFormat.getFormat(PredefinedFormat.HOUR24_MINUTE).format(d.value.lastSync);
		}
		int idx = 1;

		deviceList.getRowFormatter().setStylePrimaryName(row, s.row());

		deviceList.setWidget(row, idx++, new Label(d.value.identifier));
		deviceList.setWidget(row, idx++, new Label(d.value.type));

		final Label lSync = new Label(lastSync);

		deviceList.setWidget(row, idx++, lSync);

		Label rmSyncKeys = new Label();
		rmSyncKeys.getElement().setId("device-remove-synckey-" + d.uid);
		rmSyncKeys.setStyleName("fa fa-refresh");
		rmSyncKeys.getElement().getStyle().setCursor(Cursor.POINTER);
		rmSyncKeys.addClickHandler(getRmSyncKeysHandler(d, lSync));

		Label rmSyncKeysTxt = new Label(UserConstants.INST.deviceRemoveSyncKeys());
		rmSyncKeysTxt.getElement().setId("device-remove-synckey-txt" + d.uid);
		rmSyncKeysTxt.getElement().getStyle().setCursor(Cursor.POINTER);
		rmSyncKeysTxt.addClickHandler(getRmSyncKeysHandler(d, lSync));

		deviceList.setWidget(row, idx++, rmSyncKeys);
		deviceList.setWidget(row, idx++, rmSyncKeysTxt);

		// WIPE
		if (d.value.isWipe) {
			unwipeDevice(d, row, idx++);
		} else {
			wipeDevice(d, row, idx++);
		}
		idx++;

		Trash trash = new Trash();
		trash.setId("device-panel-trash-" + d.uid);
		trash.addStyleName(s.icon());
		trash.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				Cell cell = deviceList.getCellForEvent(event);
				int r = cell.getRowIndex();
				removeDevice(d, r);
			}
		});
		deviceList.setWidget(row, idx++, trash);

		if (easSyncUnknown) {
			CheckBox cb = new CheckBox();
			deviceList.setWidget(row, 0, cb);
			deviceList.getRowFormatter().removeStyleName(row, s.inactive());
			cb.setValue(true);
			cb.setEnabled(false);
		} else {
			if (d.value.hasPartnership) {
				setPartnership(row, d);
			} else {
				setNoPartnership(row, d);
			}
		}

		deviceList.getColumnFormatter().addStyleName(0, s.partnership());
	}

	private ClickHandler getRmSyncKeysHandler(final ItemValue<Device> d, final Label lSync) {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				removeSyncKeys(d, lSync);
			}
		};
	}

	private void wipeDevice(final ItemValue<Device> d, final int row, final int idx) {
		Label img = new Label();
		img.setStyleName("fa fa-pause");
		img.getElement().setId("device-panel-wipe-" + d.uid);
		img.getElement().getStyle().setCursor(Cursor.POINTER);

		Label imgText = new Label(UserConstants.INST.wipeDevice());
		imgText.getElement().setId("device-panel-wipe-txt" + d.uid);
		imgText.getElement().getStyle().setCursor(Cursor.POINTER);

		img.addClickHandler(getWipeClickHandler(d, row, idx));
		imgText.addClickHandler(getWipeClickHandler(d, row, idx));

		deviceList.setWidget(row, idx, img);
		deviceList.setWidget(row, idx + 1, imgText);
	}

	private ClickHandler getWipeClickHandler(final ItemValue<Device> d, final int row, final int idx) {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				UserConstants constants = UserConstants.INST;

				StringBuilder sb = new StringBuilder();
				sb.append("**** ");
				sb.append(constants.confirmWipeDeviceWarning());
				sb.append(" ****");
				sb.append("\n");
				sb.append(constants.confirmWipeDeviceWarningMsg());
				sb.append("\n\n");
				sb.append(constants.confirmWipeDevice(user.displayName, d.value.identifier));

				String prompt = Window.prompt(sb.toString(), constants.confirmWipeDevicePromptPlaceholder());
				if (prompt != null && !prompt.isEmpty()) {
					if (d.value.identifier.equals(prompt)) {

						DeviceGwtEndpoint deviceService = getDeviceService();

						deviceService.wipe(d.uid, new DefaultAsyncHandler<Void>() {

							@Override
							public void success(Void value) {
								unwipeDevice(d, row, idx);
							}
						});
					} else {
						Notification.get().reportError(constants.confirmWipeDevicePromptCheckFail());
					}
				}

			}
		};
	}

	private DeviceGwtEndpoint getDeviceService() {
		return new DeviceGwtEndpoint(Ajax.TOKEN.getSessionId(), user.entryUid);
	}

	private EasGwtEndpoint getEasService() {
		return new EasGwtEndpoint(Ajax.TOKEN.getSessionId());
	}

	private void unwipeDevice(final ItemValue<Device> d, final int row, final int idx) {
		Label img = new Label();
		img.getElement().setId("device-panel-unwipe-" + d.uid);
		img.setStyleName("fa fa-lg fa-play");
		img.getElement().getStyle().setCursor(Cursor.POINTER);

		Label imgText = new Label(UserConstants.INST.unwipeDevice());
		imgText.getElement().setId("device-panel-unwipe-txt" + d.uid);
		imgText.setTitle(UserConstants.INST.unwipeDevice());
		imgText.getElement().getStyle().setCursor(Cursor.POINTER);

		img.addClickHandler(getUnwipeClickHandler(d, row, idx));
		imgText.addClickHandler(getUnwipeClickHandler(d, row, idx));

		deviceList.setWidget(row, idx, img);
		deviceList.setWidget(row, idx + 1, imgText);
	}

	private ClickHandler getUnwipeClickHandler(final ItemValue<Device> d, final int row, final int idx) {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (Window.confirm(UserConstants.INST.confirmUnwipeDevice(user.displayName, d.value.identifier))) {

					getDeviceService().unwipe(d.uid, new DefaultAsyncHandler<Void>() {

						@Override
						public void success(Void value) {

							wipeDevice(d, row, idx);
						}
					});
				}
			}
		};
	}

	private void removeDevice(ItemValue<Device> d, final int row) {
		if (Window.confirm(UserConstants.INST.confirmDeviceRemoval(d.value.identifier))) {

			getDeviceService().delete(d.uid, new DefaultAsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					deviceList.removeRow(row);
				}
			});
		}
	}

	/**
	 * @param d
	 * @param lSync
	 */
	private void removeSyncKeys(ItemValue<Device> d, final Label lSync) {
		if (Window.confirm(UserConstants.INST.confirmDeviceRemoveSyncKeys())) {

			getEasService().insertPendingReset(Account.create(user.entryUid, d.value.identifier),
					new DefaultAsyncHandler<Void>() {

						@Override
						public void success(Void value) {
							Notification.get().reportInfo("reset !");
						}
					});

		}
	}

	/**
	 * @param row
	 * @param d
	 */
	private void setPartnership(final int row, final ItemValue<Device> d) {
		final CheckBox cb = new CheckBox();
		cb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (Window.confirm(UserConstants.INST.confirmDevicePartnershipRemoval())) {

					getDeviceService().unsetPartnership(d.uid, new DefaultAsyncHandler<Void>() {

						@Override
						public void success(Void value) {
							setNoPartnership(row, d);
						}
					});

				} else {
					// reset screen
					setDisplay(user, easSyncUnknown);
				}
			}
		});
		deviceList.setWidget(row, 0, cb);
		deviceList.getRowFormatter().removeStyleName(row, s.inactive());
		cb.setValue(true);
		if (easSyncUnknown) {
			cb.setEnabled(false);
		}
	}

	/**
	 * @param row
	 * @param d
	 */
	private void setNoPartnership(final int row, final ItemValue<Device> d) {
		final CheckBox cb = new CheckBox();
		cb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (Window.confirm(UserConstants.INST.confirmDevicePartnershipAddition())) {

					getDeviceService().setPartnership(d.uid, new DefaultAsyncHandler<Void>() {

						@Override
						public void success(Void value) {
							setPartnership(row, d);
						}

					});

				} else {
					// reset screen
					setDisplay(user, easSyncUnknown);
				}
			}
		});
		deviceList.getRowFormatter().addStyleName(row, s.inactive());
		deviceList.setWidget(row, 0, cb);
		cb.setValue(false);
	}

	/**
	 * @param msg
	 */
	private void setDeviceMessage(String msg) {
		deviceList.setWidget(deviceList.getRowCount(), 0, new Label(msg));
	}

	/**
	 * @param devices
	 */
	private void setDevices(List<ItemValue<Device>> devices) {
		deviceList.clear();
		deviceList.removeAllRows();
		if (devices.isEmpty()) {
			setDeviceMessage(UserConstants.INST.noDevice());
		} else {
			addDeviceRowHeader();
			for (ItemValue<Device> d : devices) {
				addDeviceRow(d);
			}
		}
	}

	/**
	 * @param u
	 * @param easSyncUnknown
	 */
	public void setDisplay(DirEntry u, boolean easSyncUnknown) {
		this.easSyncUnknown = easSyncUnknown;
		this.user = u;
		deviceList.clear();
		deviceList.removeAllRows();

		getDeviceService().list(new DefaultAsyncHandler<ListResult<ItemValue<Device>>>() {

			@Override
			public void success(ListResult<ItemValue<Device>> result) {
				setDevices(result.values);
			}

			@Override
			public void failure(Throwable e) {
				if (e instanceof ServerFault && ((ServerFault) e).getCode() == ErrorCode.NOT_FOUND) {
					DevicePanel.this.asWidget().setVisible(false);
				} else {
					super.failure(e);
				}
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsUser user = map.get("user").cast();
		this.user = DirEntry.create(null, null, Kind.USER, map.getString("userId"), user.getLogin(), null, false, false,
				false);
		String val = map.get(SysConfKeys.eas_sync_unknown.name()).toString();
		boolean easSyncUnknown = "true".equals(val) ? true : false;
		setDisplay(this.user, easSyncUnknown);
	}

}
