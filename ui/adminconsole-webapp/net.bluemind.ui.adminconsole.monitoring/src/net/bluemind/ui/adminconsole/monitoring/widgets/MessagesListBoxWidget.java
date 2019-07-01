/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.monitoring.widgets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.ui.adminconsole.monitoring.l10n.WidgetsConstants;
import net.bluemind.ui.adminconsole.monitoring.models.Filters;
import net.bluemind.ui.adminconsole.monitoring.models.ServerInformationMessageEntry;

public class MessagesListBoxWidget extends DataGrid<ServerInformationMessageEntry> {

	public interface MessagesListBoxWidgetResources extends ClientBundle {
	      @Source({"messagesListBoxWidget.css"})
	      MessagesListBoxWidgetStyle style();
	}
	
	public interface MessagesListBoxWidgetStyle extends CssResource {
	      	String serviceOk();
	      	String serviceWarn();
	      	String serviceKo();
		
	}
	public static final MessagesListBoxWidgetResources gwtCssDataGridResources = GWT.create(MessagesListBoxWidgetResources.class);

    static {
        gwtCssDataGridResources.style().ensureInjected();
    }
    
	public WidgetsConstants text = GWT.create(WidgetsConstants.class);

	public ListDataProvider<ServerInformationMessageEntry> fullData;
	public List<ServerInformationMessageEntry> dataList;

	public TextColumn<ServerInformationMessageEntry> statusColumn;
	public TextColumn<ServerInformationMessageEntry> pluginColumn;
	public TextColumn<ServerInformationMessageEntry> serviceColumn;
	public TextColumn<ServerInformationMessageEntry> endpointColumn;
	public TextColumn<ServerInformationMessageEntry> serverColumn;
	public TextColumn<ServerInformationMessageEntry> messageColumn;

	public MessagesListBoxWidget() {
		super();

		this.fullData = new ListDataProvider<ServerInformationMessageEntry>(
				new ArrayList<ServerInformationMessageEntry>());
		this.dataList = new ArrayList<ServerInformationMessageEntry>();
		this.fullData.addDataDisplay(this);
		this.initTable();

		this.setRowCount(20);

		setRowStyles((row, index) -> {
			switch (row.serverInfo.status) {
			case OK:
				return gwtCssDataGridResources.style().serviceOk();
			case WARNING:
				return gwtCssDataGridResources.style().serviceWarn();
			case KO:
				return gwtCssDataGridResources.style().serviceKo();
			default:
				return gwtCssDataGridResources.style().serviceWarn();
			}
		});
	}

	public void fullUpdate(List<ServerInformationMessageEntry> list) {
		this.dataList.clear();
		this.dataList.addAll(list);

		this.unfilterView();
	}

	public void unfilterView() {
		this.fullData.getList().clear();
		this.fullData.getList().addAll(this.dataList);
	}

	public void filterView(Filters filters) {

		List<ServerInformationMessageEntry> list = new ArrayList<ServerInformationMessageEntry>();

		for (ServerInformationMessageEntry info : this.dataList) {
			if (filters.isInFilter(info)) {
				list.add(info);
			}
		}

		this.fullData.getList().clear();
		this.fullData.getList().addAll(list);

	}

	private void initTable() {
		createColumns();
		initSortable();
		initColumns();
		initSelectionModel();
		initStyle();
	}

	private void initColumns() {
		initColumn(pluginColumn, this.text.pluginColumnLabel(), 5.0, Unit.PCT);
		initColumn(serviceColumn, this.text.serviceColumnLabel(), 5.0, Unit.PCT);
		initColumn(endpointColumn, this.text.endpointColumnLabel(), 5.0, Unit.PCT);
		initColumn(serverColumn, this.text.serverColumnLabel(), 5.0, Unit.PCT);
		initColumn(statusColumn, this.text.statusColumnLabel(), 7.5, Unit.PCT);
		initColumn(messageColumn, this.text.messageColumnLabel(), 40.0, Unit.PCT);
	}

	private void createColumns() {
		this.statusColumn = new TextColumn<ServerInformationMessageEntry>() {

			@Override
			public String getValue(ServerInformationMessageEntry object) {
				return object.serverInfo.status.toString();
			}
		};
		this.pluginColumn = new TextColumn<ServerInformationMessageEntry>() {

			@Override
			public String getValue(ServerInformationMessageEntry object) {
				return object.serverInfo.plugin;
			}
		};
		this.serviceColumn = new TextColumn<ServerInformationMessageEntry>() {

			@Override
			public String getValue(ServerInformationMessageEntry object) {
				return object.serverInfo.service;
			}
		};
		this.endpointColumn = new TextColumn<ServerInformationMessageEntry>() {

			@Override
			public String getValue(ServerInformationMessageEntry object) {
				return object.serverInfo.endpoint;
			}

		};
		this.serverColumn = new TextColumn<ServerInformationMessageEntry>() {

			@Override
			public String getValue(ServerInformationMessageEntry object) {
				return object.serverInfo.server.name;
			}
		};
		this.messageColumn = new TextColumn<ServerInformationMessageEntry>() {

			@Override
			public String getValue(ServerInformationMessageEntry object) {
				if (object.idMessage == -1) {
					return "<< Aucun message >>";
				}
				return object.serverInfo.messages.get(object.idMessage);
			}

		};
	}

	private void initStyle() {
		this.setEmptyTableWidget(new HTMLPanel("h1", this.text.noFetchedMessage()));
		this.getElement().getStyle().setCursor(Cursor.POINTER);
	}

	private void initSelectionModel() {
		final NoSelectionModel<ServerInformationMessageEntry> ssm = new NoSelectionModel<ServerInformationMessageEntry>();
		ssm.addSelectionChangeHandler(new Handler() {

			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				ServerInformationMessageEntry selectedInformation = ssm.getLastSelectedObject();
				// Actions.get().showWithParams2("checkInformation",
				// selectedInformation.param);

				DialogBox popup = new DialogBox(true);

				popup.addStyleName("dialog");
				popup.add(new ServerInformationWidget(selectedInformation.serverInfo));
				popup.setGlassEnabled(true);
				popup.setGlassStyleName("modalOverlay");
				popup.center();

			}
		});

		this.setSelectionModel(ssm);
	}

	private void initSortable() {
		ListHandler<ServerInformationMessageEntry> columnSortHandler = new ListHandler<ServerInformationMessageEntry>(
				this.fullData.getList());
		ColumnSortList columnSortList = this.getColumnSortList();

		this.statusColumn.setSortable(true);
		this.pluginColumn.setSortable(true);
		this.serviceColumn.setSortable(true);
		this.endpointColumn.setSortable(true);
		this.serverColumn.setSortable(true);

		columnSortHandler.setComparator(this.statusColumn, new Comparator<ServerInformationMessageEntry>() {

			@Override
			public int compare(ServerInformationMessageEntry o1, ServerInformationMessageEntry o2) {

				if (o1.serverInfo.status.getValue() < o2.serverInfo.status.getValue()) {
					return -1;
				} else if (o1.serverInfo.status.getValue() == o2.serverInfo.status.getValue()) {
					return 0;
				}
				return 1;
			}
		});
		columnSortHandler.setComparator(this.pluginColumn, new Comparator<ServerInformationMessageEntry>() {

			@Override
			public int compare(ServerInformationMessageEntry o1, ServerInformationMessageEntry o2) {

				if (o1.serverInfo.plugin.compareTo(o2.serverInfo.plugin) < 0) {
					return -1;
				} else if (o1.serverInfo.plugin.compareTo(o2.serverInfo.plugin) == 0) {
					return 0;
				}

				return 1;
			}

		});
		columnSortHandler.setComparator(this.serviceColumn, new Comparator<ServerInformationMessageEntry>() {

			@Override
			public int compare(ServerInformationMessageEntry o1, ServerInformationMessageEntry o2) {

				if (o1.serverInfo.service.compareTo(o2.serverInfo.service) < 0) {
					return -1;
				} else if (o1.serverInfo.service.compareTo(o2.serverInfo.service) == 0) {
					return 0;
				}

				return 1;
			}

		});
		columnSortHandler.setComparator(this.endpointColumn, new Comparator<ServerInformationMessageEntry>() {

			@Override
			public int compare(ServerInformationMessageEntry o1, ServerInformationMessageEntry o2) {

				if (o1.serverInfo.endpoint.compareTo(o2.serverInfo.endpoint) < 0) {
					return -1;
				} else if (o1.serverInfo.endpoint.compareTo(o2.serverInfo.endpoint) == 0) {
					return 0;
				}

				return 1;
			}

		});
		columnSortHandler.setComparator(this.serverColumn, new Comparator<ServerInformationMessageEntry>() {

			@Override
			public int compare(ServerInformationMessageEntry o1, ServerInformationMessageEntry o2) {

				if (o1.serverInfo.server.name.compareTo(o2.serverInfo.server.name) < 0) {
					return -1;
				} else if (o1.serverInfo.server.name.compareTo(o2.serverInfo.server.name) == 0) {
					return 0;
				}

				return 1;
			}

		});

		this.addColumnSortHandler(columnSortHandler);

		columnSortList.push(this.pluginColumn);
		columnSortList.push(this.serviceColumn);
		columnSortList.push(this.serverColumn);
		columnSortList.push(this.statusColumn);

	}

	private void initColumn(TextColumn<ServerInformationMessageEntry> column, String label, double size, Unit unit) {
		this.addColumn(column, label);
		this.setColumnWidth(column, size, unit);
	}

}