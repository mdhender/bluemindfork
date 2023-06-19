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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;

import net.bluemind.smime.cacerts.api.SmimeRevocation;
import net.bluemind.ui.adminconsole.base.ui.IBmGrid;
import net.bluemind.ui.adminconsole.system.certificate.smime.l10n.SmimeCertificateConstants;

public class SmimeRevocationsGrid extends DataGrid<SmimeRevocation> implements IBmGrid<SmimeRevocation> {

	public static final String TYPE = "bm.ac.SmimeRevocationsGrid";

	protected ListDataProvider<SmimeRevocation> ldp;
	protected static final SmimeCertificateConstants constants = SmimeCertificateConstants.INST;

	public SmimeRevocationsGrid() {

		TextColumn<SmimeRevocation> serialNumberColumn = new TextColumn<SmimeRevocation>() {

			@Override
			public String getValue(SmimeRevocation de) {
				return de.serialNumber;
			}

		};

		addColumn(serialNumberColumn, constants.rSn());
		setColumnWidth(serialNumberColumn, 120, Unit.PX);
		serialNumberColumn.setSortable(true);

		Column<SmimeRevocation, SafeHtml> issuerColumn = new Column<SmimeRevocation, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(SmimeRevocation de) {
				SafeHtmlBuilder builder = new SafeHtmlBuilder();
				if (de != null) {
					builder.appendHtmlConstant(de.issuer.replace(",", ",<br/>"));
				}
				return builder.toSafeHtml();
			}
		};

		addColumn(issuerColumn, constants.rIssuer());
		setColumnWidth(issuerColumn, 100, Unit.PX);
		issuerColumn.setSortable(true);

		TextColumn<SmimeRevocation> dateColumn = new TextColumn<SmimeRevocation>() {

			@Override
			public String getValue(SmimeRevocation de) {
				return DateTimeFormat.getShortDateFormat().format(de.revocationDate);
			}

		};

		addColumn(dateColumn, constants.rDate());
		setColumnWidth(dateColumn, 50, Unit.PX);
		dateColumn.setSortable(true);

		TextColumn<SmimeRevocation> reasonColumn = new TextColumn<SmimeRevocation>() {

			@Override
			public String getValue(SmimeRevocation de) {
				return de.revocationReason;
			}

		};

		addColumn(reasonColumn, constants.rReason());
		setColumnWidth(reasonColumn, 100, Unit.PX);
		reasonColumn.setSortable(true);

		setHeight("250px");

		setLoadingIndicator(null);

		ldp = new ListDataProvider<>();
		ldp.addDataDisplay(this);

		// add handler to sorting
		AsyncHandler columnSortHanler = new AsyncHandler(this);
		addColumnSortHandler(columnSortHanler);

	}

	@Override
	public void refresh() {
		ldp.refresh();
	}

	@Override
	public void selectAll(boolean b) {
	}

	@Override
	public List<SmimeRevocation> getValues() {
		return ldp.getList();
	}

	@Override
	public void setValues(List<SmimeRevocation> values) {
		ldp.setList(values);
		ldp.refresh();
	}

}
