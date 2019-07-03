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
package net.bluemind.ui.common.client.forms.acl;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.ui.common.client.forms.IFormChangeListener;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestOracle;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class AclAutoCompleteEntity extends Composite implements ICommonEditor {

	private static final AclConstants constants = GWT.create(AclConstants.class);

	private SuggestBox input;
	private final IEntityFinder<DirEntry, DirEntryQuery> finder;
	private IEntitySelectTarget target;

	public AclAutoCompleteEntity(IEntityFinder<DirEntry, DirEntryQuery> finder, boolean mandatory, String comboQuery) {

		this.finder = finder;

		SuggestOracle oracle = new EntitySuggestOracle<>(finder);
		input = new SuggestBox(oracle);
		input.addSelectionHandler(new EntitySelectionHandler(this));

		input.getElement().setAttribute("placeholder", constants.aclPlaceHolder());

		initWidget(input);
	}

	public void setTarget(IEntitySelectTarget ae) {
		target = ae;
	}

	public void select(DirEntry suggested) {
		if (input != null) {
			AclSelectedEntity entity = new AclSelectedEntity(suggested, this);
			target.seletected(entity.getEntity());
			input.setValue("", false);
		}
	}

	public IEntityFinder<DirEntry, DirEntryQuery> getFinder() {
		return finder;
	}

	@Override
	public void setTitleText(String titleText) {
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public void setStringValue(String v) {
	}

	@Override
	public void setDescriptionText(String s) {
	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		return null;
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	public void setEnable(boolean e) {
		if (e) {
			input.getElement().removeAttribute("disabled");
		} else {
			input.getElement().setAttribute("disabled", "disabled");
		}
	}

	@Override
	public void setReadOnly(boolean readOnly) {
	}

	@Override
	public void addFormChangeListener(IFormChangeListener listener) {
	}

	@Override
	public void setId(String id) {
		input.getElement().setId(id);
	}

	public void setDomain(String domain) {
		finder.setDomain(domain);
	}
}
