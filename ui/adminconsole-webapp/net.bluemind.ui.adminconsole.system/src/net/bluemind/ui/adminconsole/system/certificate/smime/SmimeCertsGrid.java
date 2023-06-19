/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.ui.adminconsole.system.certificate.smime;

import java.util.List;

import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import net.bluemind.smime.cacerts.api.SmimeCacertInfos;
import net.bluemind.ui.adminconsole.base.ui.IBmGrid;
import net.bluemind.ui.adminconsole.system.certificate.smime.l10n.SmimeCertificateConstants;

public class SmimeCertsGrid extends DataGrid<SmimeCacertInfos> implements IBmGrid<SmimeCacertInfos> {

	Column<SmimeCacertInfos, SafeHtml> subjectColumn;
	Column<SmimeCacertInfos, SafeHtml> issuerColumn;
	Column<SmimeCacertInfos, SafeHtml> nbRevocationsColumn;

	public interface DCGStyle extends CssResource {

		public String suspended();

	}

	public static final String TYPE = "bm.ac.SmimeCertsGrid";

	protected SelectionModel<SmimeCacertInfos> selectionModel;
	protected ListDataProvider<SmimeCacertInfos> ldp;
	protected static final SmimeCertificateConstants constants = SmimeCertificateConstants.INST;

	public SmimeCertsGrid() {

		this.getElement().getStyle().setCursor(Cursor.POINTER);

		this.selectionModel = new SingleSelectionModel<>(item -> (item == null) ? null : item.cacertUid);
		setSelectionModel(this.selectionModel);

		subjectColumn = new Column<SmimeCacertInfos, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(SmimeCacertInfos de) {
				SafeHtmlBuilder builder = new SafeHtmlBuilder();
				if (de != null) {
					builder.appendHtmlConstant("<div>" + de.cacertSubject.replace(",", ",<br/>") + "</div>");
				}
				return builder.toSafeHtml();
			}
		};

		setColumnWidth(subjectColumn, 100, Unit.PX);
		subjectColumn.setSortable(true);

		issuerColumn = new Column<SmimeCacertInfos, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(SmimeCacertInfos de) {
				SafeHtmlBuilder builder = new SafeHtmlBuilder();
				if (de != null) {
					builder.appendHtmlConstant("<div>" + de.cacertIssuer.replace(",", ",<br/>") + "</div>");
				}
				return builder.toSafeHtml();
			}
		};

		setColumnWidth(issuerColumn, 100, Unit.PX);
		issuerColumn.setSortable(true);

		nbRevocationsColumn = new Column<SmimeCacertInfos, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(SmimeCacertInfos de) {
				SafeHtmlBuilder builder = new SafeHtmlBuilder();
				if (de != null) {
					if (!de.revocations.isEmpty()) {
						builder.appendHtmlConstant("<div title=\"" + constants.showRevocations() + "\">"
								+ de.revocations.size() + "</div>");
					} else {
						builder.appendHtmlConstant("<div>" + de.revocations.size() + "</div>");
					}
				}
				return builder.toSafeHtml();
			}
		};

		setColumnWidth(nbRevocationsColumn, 50, Unit.PX);

		setHeight("300px");

		addColumn(subjectColumn, constants.caSubject());
		addColumn(issuerColumn, constants.caIssuer());
		addColumn(nbRevocationsColumn, constants.revocations());

		setLoadingIndicator(null);

		ldp = new ListDataProvider<>();
		ldp.addDataDisplay(this);

		// add handler to sorting
		AsyncHandler columnSortHanler = new AsyncHandler(this);
		addColumnSortHandler(columnSortHanler);

		addCellPreviewHandler(event -> {
			if (BrowserEvents.CLICK.equalsIgnoreCase(event.getNativeEvent().getType())) {
				new SmimeRevocationsDialog(event.getValue()).openDialog();
			}
		});
	}

	@Override
	public void refresh() {
		ldp.refresh();
	}

	@Override
	public void selectAll(boolean b) {
	}

	@Override
	public List<SmimeCacertInfos> getValues() {
		return ldp.getList();
	}

	@Override
	public void setValues(List<SmimeCacertInfos> values) {
		ldp.setList(values);
		ldp.refresh();
	}

}
