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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsSockJsEndpoint;
import net.bluemind.ui.adminconsole.base.orgunit.OUFinder;
import net.bluemind.ui.adminconsole.base.orgunit.OUUtils;
import net.bluemind.ui.common.client.forms.AbstractTextEdit;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.ITextEditor;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestOracle;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestion;
import net.bluemind.ui.common.client.icon.Trash;

public class DelegationEdit extends AbstractTextEdit<String> {

	private OUFinder ouFinder;

	private String orgUnitUid;
	private String domainUid;

	protected FlowPanel container;
	protected TextBox textBox;
	private Trash trash;

	@Override
	public ITextEditor<String> createTextBox() {
		s = res.editStyle();
		s.ensureInjected();
		container = new FlowPanel();

		ouFinder = new OUFinder();

		SuggestOracle oracle = new EntitySuggestOracle<>(ouFinder);
		SuggestBox suggest = new SuggestBox(oracle);
		textBox = new TextBox();
		container.add(textBox);
		container.add(suggest);
		trash = new Trash();
		trash.addClickHandler(e -> {
			asEditor().setValue(null);
		});
		trash.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
		container.add(trash);
		suggest.setVisible(false);
		textBox.addFocusHandler(e -> {
			textBox.setVisible(false);
			suggest.setVisible(true);
			suggest.setFocus(true);
		});

		suggest.getValueBox().addBlurHandler(e -> {
			suggest.setVisible(false);
			textBox.setVisible(true);
		});

		suggest.addSelectionHandler(e -> {
			Suggestion selectedItem = e.getSelectedItem();
			@SuppressWarnings("unchecked")
			EntitySuggestion<OrgUnitPath, OrgUnitQuery> sug = (EntitySuggestion<OrgUnitPath, OrgUnitQuery>) selectedItem;
			textBox.setText(toPath(sug.getEntity()));
			orgUnitUid = sug.getEntity().uid;
			GWT.log("value " + sug.getEntity().name);
			suggest.setFocus(false);
		});

		return new ITextEditor<String>() {

			@Override
			public Widget asWidget() {
				return container;
			}

			@Override
			public ValueBoxEditor<String> asEditor() {

				return new ValueBoxEditor<String>(textBox) {

					@Override
					public void setValue(String value) {
						orgUnitUid = value;
						loadValue();
					}

					@Override
					public String getValue() {
						return orgUnitUid;
					}
				};
			}

			@Override
			public void setEnabled(boolean b) {
				textBox.setEnabled(b);
			}

		};
	}

	public void setId(String id) {
		textBox.getElement().setId(id);

	}

	public void setDomain(String domain) {
		this.domainUid = domain;
		this.ouFinder.setDomain(domain);
	}

	private void loadValue() {
		if (domainUid != null && orgUnitUid != null) {
			IOrgUnitsPromise ous = new OrgUnitsSockJsEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();
			ous.getPath(orgUnitUid).thenAccept(path -> {
				textBox.setValue(toPath(path));
			});
		} else {
			textBox.setValue("");
		}
	}

	private static String toPath(OrgUnitPath path) {
		return OUUtils.toPath(path);
	}

	public void setKind(Kind kind) {
		ouFinder.setKind(kind);
	}

	public void setMaxLength(int len) {
		textBox.setMaxLength(len);
	}

	@Override
	public void setStringValue(String v) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDescriptionText(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPropertyName(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getPropertyName() {
		// TODO Auto-generated method stub
		return null;
	}
}
