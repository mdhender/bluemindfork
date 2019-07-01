/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.base.orgunit;

import java.util.Arrays;
import java.util.HashSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.ui.adminconsole.base.orgunit.l10n.OrgUnitConstants;
import net.bluemind.ui.common.client.OverlayScreen;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestOracle;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestion;

public class AddOrgUnitRoles extends Composite {

	interface AddOrgUnitRolesUiBinder extends UiBinder<DockLayoutPanel, AddOrgUnitRoles> {
	}

	private static AddOrgUnitRolesUiBinder uiBinder = GWT.create(AddOrgUnitRolesUiBinder.class);

	@UiField
	DoneCancelActionBar actionBar;

	@UiField
	HTMLPanel formPanel;

	private OverlayScreen os;

	private OrgUnitPath path;

	private SuggestBox suggestbox;

	public AddOrgUnitRoles(String domainUid) {
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.getElement().setAttribute("style", "z-index:1000");
		initWidget(dlp);

		OUFinder ouFinder = new OUFinder();
		ouFinder.setDomain(domainUid);
		ouFinder.setKinds(new HashSet<>(Arrays.asList(DirEntry.Kind.values())));
		SuggestOracle oracle = new EntitySuggestOracle<>(ouFinder);
		suggestbox = new SuggestBox(oracle);
		suggestbox.getElement().setAttribute("placeholder", OrgUnitConstants.INST.ou());
		formPanel.add(suggestbox);

		suggestbox.addSelectionHandler(e -> {
			EntitySuggestion<OrgUnitPath, OrgUnitQuery> sug = (EntitySuggestion<OrgUnitPath, OrgUnitQuery>) e
					.getSelectedItem();
			path = sug.getEntity();
			suggestbox.setText(OUUtils.toPath(path));
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				hide();
			}
		});

	}

	public OrgUnitPath getValue() {
		return path;
	}

	public void addDoneAction(ScheduledCommand sc) {
		actionBar.setDoneAction(sc);
	}

	public SizeHint getSizeHint() {
		return new SizeHint(500, 150);

	}

	public void setOverlay(OverlayScreen os) {
		this.os = os;
	}

	public void hide() {
		os.hide();
	}

	public void setFocus() {
		suggestbox.setFocus(true);
	}
}
