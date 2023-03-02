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
package net.bluemind.ui.adminconsole.system.domains.openid;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;

import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class OpenIdGrid extends DataGrid<OpenIdRegistration> {

	public static DateTimeFormat sdf = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM);

	private static final DomainConstants txt = DomainConstants.INST;

	private ListDataProvider<OpenIdRegistration> ldp;

	private List<OpenIdRegistration> entities;

	public OpenIdGrid() {
		entities = new ArrayList<>();
		createColums();
		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		this.ldp = new ListDataProvider<OpenIdRegistration>();
		ldp.addDataDisplay(this);

		AsyncHandler columnSortHandler = new AsyncHandler(this);
		addColumnSortHandler(columnSortHandler);
	}

	private void createColums() {

		TextColumn<OpenIdRegistration> systemIdentifier = new TextColumn<OpenIdRegistration>() {
			@Override
			public String getValue(OpenIdRegistration reg) {
				return reg.systemIdentifier;
			}
		};
		addColumn(systemIdentifier, txt.systemIdentifier(), txt.systemIdentifier());
		setColumnWidth(systemIdentifier, 20, Unit.PCT);
		systemIdentifier.setSortable(true);

		Column<OpenIdRegistration, String> endpoint = new Column<OpenIdRegistration, String>(
				new FixedSizeEditInputCell()) {
			@Override
			public String getValue(OpenIdRegistration reg) {
				return reg.endpoint;
			}
		};
		addColumn(endpoint, txt.endpoint(), txt.endpoint());
		setColumnWidth(endpoint, 300, Unit.PX);

		endpoint.setFieldUpdater(new FieldUpdater<OpenIdRegistration, String>() {

			@Override
			public void update(int index, OpenIdRegistration object, String value) {
				OpenIdGrid.this.entities.get(index).endpoint = value;
			}

		});

		Column<OpenIdRegistration, String> applicationId = new Column<OpenIdRegistration, String>(
				new FixedSizeEditInputCell()) {
			@Override
			public String getValue(OpenIdRegistration reg) {
				return reg.applicationId;
			}
		};
		addColumn(applicationId, txt.applicationId(), txt.applicationId());
		setColumnWidth(applicationId, 300, Unit.PX);

		applicationId.setFieldUpdater(new FieldUpdater<OpenIdRegistration, String>() {

			@Override
			public void update(int index, OpenIdRegistration object, String value) {
				OpenIdGrid.this.entities.get(index).applicationId = value;
			}

		});

		Column<OpenIdRegistration, String> clientSecret = new Column<OpenIdRegistration, String>(
				new FixedSizeEditInputCell()) {
			@Override
			public String getValue(OpenIdRegistration reg) {
				return reg.applicationSecret;
			}
		};
		addColumn(clientSecret, txt.applicationSecret(), txt.applicationSecret());
		setColumnWidth(clientSecret, 300, Unit.PX);

		clientSecret.setFieldUpdater(new FieldUpdater<OpenIdRegistration, String>() {

			@Override
			public void update(int index, OpenIdRegistration object, String value) {
				OpenIdGrid.this.entities.get(index).applicationSecret = value;
			}

		});

		Column<OpenIdRegistration, String> tokenEndpoint = new Column<OpenIdRegistration, String>(
				new FixedSizeEditInputCell()) {
			@Override
			public String getValue(OpenIdRegistration reg) {
				return reg.tokenEndpoint;
			}
		};
		addColumn(tokenEndpoint, txt.tokenEndpoint(), txt.tokenEndpoint());
		setColumnWidth(tokenEndpoint, 300, Unit.PX);

		tokenEndpoint.setFieldUpdater(new FieldUpdater<OpenIdRegistration, String>() {

			@Override
			public void update(int index, OpenIdRegistration object, String value) {
				OpenIdGrid.this.entities.get(index).tokenEndpoint = value;
			}

		});

	}

	public void setValues(List<OpenIdRegistration> entities) {
		this.entities = entities;
		ldp.setList(entities);
		ldp.refresh();
	}

	public List<OpenIdRegistration> getValues() {
		return this.entities;
	}

	public void refresh() {
		ldp.refresh();
	}

	static class FixedSizeEditInputCell extends TextInputCell {
		private static Template template;

		interface Template extends SafeHtmlTemplates {
			// {0}, {1}, {2} relate to value, size, style
			@Template("<input type=\"text\" value=\"{0}\" tabindex=\"-1\" size=\"{1}\" maxlength=\"{1}\" style=\"{2}\"></input>")
			SafeHtml input(String value, String size, String style);
		}

		public FixedSizeEditInputCell() {
			template = GWT.create(Template.class);
		}

		@Override
		public void render(Context context, String value, SafeHtmlBuilder sb) {
			// Get the view data.
			Object key = context.getKey();
			ViewData viewData = getViewData(key);
			if (viewData != null && viewData.getCurrentValue().equals(value)) {
				clearViewData(key);
				viewData = null;
			}

			String s = (viewData != null) ? viewData.getCurrentValue() : value;
			if (s != null) {
				sb.append(template.input(s, "300", "width: 460px"));
			} else {
				sb.appendHtmlConstant("<input type=\"text\" tabindex=\"-1\"></input>");
			}
		}
	}
}
