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

import java.util.List;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Style.TextAlign;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;

import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.ui.adminconsole.monitoring.l10n.WidgetsConstants;
import net.bluemind.ui.adminconsole.monitoring.util.HtmlTag;
import net.bluemind.ui.adminconsole.monitoring.util.UIFormatter;

/**
 * 
 * Widget used to display a single-server information.
 * 
 * @author vincent
 *
 */
public class ServerInformationWidget extends Composite {

	public ServerInformation info;

	public VerticalPanel mainPanel;
	public ListBox messages;
	public CellTable<FetchedData> techTable;
	public WidgetsConstants text;

	public ServerInformationWidget(ServerInformation info) {
		this.mainPanel = new VerticalPanel();
		this.info = info;
		this.initWidget(mainPanel);
		this.init();
	}

	public void init() {
		this.text = GWT.create(WidgetsConstants.class);
		this.mainPanel.setWidth(UIFormatter.WIDGET_WIDTH);

		this.createTitle();
		this.createStatusMessage();
		this.createMessages();
		this.createDataTable(this.techTable, this.text.techTableLabel());

		this.mainPanel.getElement().getStyle().setMargin(2.0, Unit.EM);
	}

	private void createStatusMessage() {

		HTMLPanel title = new HTMLPanel("h3", "");

		switch (this.info.status) {
		case OK:
			title.getElement().setInnerHTML(this.text.okInfo());
			break;
		case WARNING:
			title.getElement().setInnerHTML(this.text.warningInfo());
			break;
		case KO:
			title.getElement().setInnerHTML(this.text.koInfo());
			break;
		default:
			title.getElement().setInnerHTML(this.text.unknownInfo());
		}

		title.getElement().getStyle().setTextAlign(TextAlign.CENTER);
		this.mainPanel.add(title);
	}

	private void createTitle() {

		this.mainPanel.add(UIFormatter.newTitle(HtmlTag.H1.toString(),
				info.plugin + "/" + info.service + "/" + info.endpoint + " " + this.text.on() + " " + info.server.name,
				this.info.status));

	}

	private void createMessages() {

		this.messages = new ListBox();
		this.messages.setVisibleItemCount(UIFormatter.LIST_VISIBLE_ITEM_COUNT);
		this.messages.setWidth(UIFormatter.WIDGET_WIDTH);

		this.mainPanel.add(new HTMLPanel(HtmlTag.H2.toString(), text.infoMessages()));

		if (this.info.messages != null) {
			for (String msg : this.info.messages) {
				this.messages.addItem(msg);
			}
			this.mainPanel.add(this.messages);
		}

	}

	private void createDataTable(CellTable<FetchedData> table, String title) {
		table = new CellTable<FetchedData>();
		table.setWidth(UIFormatter.WIDGET_WIDTH);

		TextColumn<FetchedData> titleColumn = new TextColumn<FetchedData>() {

			@Override
			public String getValue(FetchedData object) {
				return object.title;
			}
		};
		TextColumn<FetchedData> dataColumn = new TextColumn<FetchedData>() {

			@Override
			public String getValue(FetchedData object) {
				return object.data;
			}
		};
		ListDataProvider<FetchedData> dataProvider = new ListDataProvider<FetchedData>();

		table.addColumn(titleColumn, text.dataId());
		table.addColumn(dataColumn, text.fetchedData());
		dataProvider.addDataDisplay(table);

		List<FetchedData> list = dataProvider.getList();

		this.mainPanel.add(new HTMLPanel(HtmlTag.H2.toString(), title));

		if (this.info.dataList != null && this.info.dataList.size() > 0) {
			for (FetchedData data : this.info.dataList) {
				list.add(data);
			}

			if (list.size() > 0) {
				this.mainPanel.add(table);
			} else {
				this.mainPanel.add(new Label(text.noFetchedData()));
			}
		} else {
			this.mainPanel.add(new Label(text.noFetchedData()));
		}
	}
}
