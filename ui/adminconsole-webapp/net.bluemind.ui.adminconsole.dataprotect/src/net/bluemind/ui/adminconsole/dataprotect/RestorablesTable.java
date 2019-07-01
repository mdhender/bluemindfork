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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.GenerationContent;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.api.RestorableKind;
import net.bluemind.dataprotect.api.RestoreOperation;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;
import net.bluemind.ui.common.client.forms.Ajax;

public class RestorablesTable extends DataGrid<ClientRestorable> {

	private SelectionModel<ClientRestorable> selectionModel;
	private ListDataProvider<ClientRestorable> ldp;
	private ProvidesKey<ClientRestorable> keyProvider;
	private Map<RestorableKind, List<RestoreOperation>> caps;
	private String locale;
	private int generationId;
	private HashMap<String, Domain> domIdx;
	private GenerationContent content;

	private static final DPTexts txt = DPTexts.INST;

	public RestorablesTable() {
		this.locale = LocaleInfo.getCurrentLocale().getLocaleName();
		if (locale.length() > 2) {
			locale = locale.substring(0, 2);
		}

		this.keyProvider = new ProvidesKey<ClientRestorable>() {

			@Override
			public Object getKey(ClientRestorable item) {
				return item.entryUid + "@" + item.domainUid;
			}
		};
		this.selectionModel = new NoSelectionModel<ClientRestorable>(keyProvider);
		setSelectionModel(this.selectionModel);

		Column<ClientRestorable, TippedResource> typeColumn = new Column<ClientRestorable, TippedResource>(
				new TooltipedImageCell()) {

			@Override
			public TippedResource getValue(ClientRestorable j) {
				switch (j.kind) {
				case DOMAIN:
					return new TippedResource("fa-home", txt.typeDomain());
				case MAILSHARE:
					return new TippedResource("fa-inbox", txt.typeMailshare());
				case OU:
					return new TippedResource("fa-sitemap", txt.typeOU());
				default:
				case USER:
					return new TippedResource("fa-user", txt.typeUser());
				}
			}
		};
		typeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		addColumn(typeColumn, txt.colType(), txt.colType());
		setColumnWidth(typeColumn, 48, Unit.PX);

		TextColumn<ClientRestorable> nextRun = new TextColumn<ClientRestorable>() {
			@Override
			public String getValue(ClientRestorable d) {
				return d.displayName;
			}
		};
		addColumn(nextRun, txt.colEntity(), txt.colEntity());
		setColumnWidth(nextRun, 200, Unit.PX);

		if (Ajax.TOKEN.isDomainGlobal()) {
			TextColumn<ClientRestorable> domain = new TextColumn<ClientRestorable>() {
				@Override
				public String getValue(ClientRestorable d) {
					if (d.kind == RestorableKind.DOMAIN) {
						return "";
					} else {
						Domain dom = domIdx.get(d.domainUid);
						return dom != null ? dom.name : "";
					}
				}
			};
			addColumn(domain, txt.colDomain(), txt.colDomain());
			setColumnWidth(domain, 200, Unit.PX);
		}

		Column<ClientRestorable, List<ActionHandler<ClientRestorable>>> actions = new Column<ClientRestorable, List<ActionHandler<ClientRestorable>>>(
				new ActionRestoreCell<ClientRestorable>()) {
			@Override
			public List<ActionHandler<ClientRestorable>> getValue(ClientRestorable d) {
				List<ActionHandler<ClientRestorable>> trans = new LinkedList<ActionHandler<ClientRestorable>>();
				if (d.deleted && d.kind == RestorableKind.USER) {
					RestoreOperation restore = new RestoreOperation();
					restore.identifier = "complete.restore." + d.kind.name().toLowerCase();
					restore.kind = d.kind;
					restore.translations = new HashMap<>();
					restore.translations.put("en", "Restore");
					restore.translations.put("fr", "Restaurer");
					ActionHandler<ClientRestorable> ahr = new RestoreActionHandler("Restore", content, d, generationId,
							restore);
					trans.add(ahr);
				} else {
					List<RestoreOperation> rops = caps.get(d.kind);

					if (rops != null) {
						for (RestoreOperation rop : rops) {
							String tr = rop.translations.get(locale);
							if (tr == null) {
								tr = rop.identifier;
							}
							ActionHandler<ClientRestorable> ahr = new RestoreActionHandler(tr, content, d, generationId,
									rop);
							trans.add(ahr);
						}
					}
				}
				return trans;
			}
		};

		addColumn(actions, txt.colActions(), txt.colActions());
		setColumnWidth(actions, 100, Unit.PX);

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		ldp = new ListDataProvider<ClientRestorable>();
		ldp.addDataDisplay(this);
	}

	public void refresh() {
		ldp.refresh();
	}

	public void selectAll(boolean b) {
	}

	public List<ClientRestorable> getValues() {
		return ldp.getList();
	}

	public void setValues(List<Restorable> values) {
		List<ClientRestorable> mappedValues = new ArrayList<>(values.size());
		int index = 0;
		mapValue(index, values, mappedValues);
	}

	private void mapValue(int index, List<Restorable> values, List<ClientRestorable> mappedValues) {
		if (index == values.size()) {
			ldp.setList(mappedValues);
			ldp.refresh();
		} else {
			Restorable rest = values.get(index);
			DirectoryGwtEndpoint dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), rest.domainUid);
			dir.findByEntryUid(rest.entryUid, new DefaultAsyncHandler<DirEntry>() {

				@Override
				public void success(DirEntry value) {
					mappedValues.add(new ClientRestorable(rest, value == null));
					mapValue(index + 1, values, mappedValues);
				}
			});

		}
	}

	public void setCapabilities(List<RestoreOperation> capabilities) {
		this.caps = new HashMap<>();
		for (RestoreOperation ro : capabilities) {
			List<RestoreOperation> l = caps.get(ro.kind);
			if (l == null) {
				l = new LinkedList<>();
				caps.put(ro.kind, l);
			}
			l.add(ro);
		}
	}

	public void setGeneration(int generationId) {
		this.generationId = generationId;
	}

	public void setContent(GenerationContent content) {
		this.content = content;
		List<ItemValue<Domain>> doms = content.domains;
		this.domIdx = new HashMap<String, Domain>();
		for (ItemValue<Domain> d : doms) {
			domIdx.put(d.uid, d.value);
		}
	}

}
