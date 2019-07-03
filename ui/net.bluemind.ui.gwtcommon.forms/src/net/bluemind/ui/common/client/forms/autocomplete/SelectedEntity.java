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
package net.bluemind.ui.common.client.forms.autocomplete;

import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class SelectedEntity<T, TQ> extends Composite {

	private EntityEdit<T, TQ> edit;
	private T entity;

	public SelectedEntity(T entity, EntityEdit<T, TQ> eEdit) {
		this.edit = eEdit;
		this.entity = entity;

		FlowPanel fp = new FlowPanel();
		Label del = new Label("X");
		del.getElement().getStyle().setFontWeight(FontWeight.BOLD);
		Label spacer = new Label("");
		spacer.setWidth("5px");
		fp.add(del);
		fp.add(spacer);
		del.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				edit.deselect(SelectedEntity.this);
			}
		});

		IEntityFinder<T, TQ> f = edit.getFinder();
		Label l = new Label();
		l.setText(f.toString(entity).trim());
		fp.add(l);
		Label spacer2 = new Label("");
		spacer2.setWidth("5px");
		fp.add(spacer2);
		initWidget(fp);
	}

	public T getEntity() {
		return entity;
	}
}
