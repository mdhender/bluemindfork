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
package net.bluemind.ui.adminconsole.base.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.base.Actions;

public class SectionAnchorContainer extends FlexTable {

	private String id;
	private SectionAnchor anchor;
	private Label icon;
	private Label iconActive;
	private static final String faStyle = " fa fa-lg ";

	private SACStyle style;
	private boolean enabled;

	public interface SABundle extends ClientBundle {
		@Source("SectionAnchorContainer.css")
		SACStyle getStyle();
	}

	public interface SACStyle extends CssResource {
		String anchorContainer();

		String anchor();

		String menuiconInactive();

		String menuiconActive();

		String active();
	}

	public static final SABundle sab = GWT.create(SABundle.class);

	public SectionAnchorContainer(Section section) {
		GWT.log("Creating section " + section.getIconStyle());

		checkRoles(section);
		this.style = sab.getStyle();
		style.ensureInjected();

		id = section.getId();

		anchor = createSectionAnchor(section);
		icon = new Label();
		icon.setStyleName(section.getIconStyle() + faStyle + this.style.menuiconInactive());
		iconActive = new Label();
		iconActive.setStyleName(section.getIconStyle() + faStyle + this.style.menuiconActive());
		setWidget(0, 0, icon);
		setWidget(0, 1, anchor);
		getCellFormatter().addStyleName(0, 1, style.anchor());
		setStyleName(style.anchorContainer());

	}

	private void checkRoles(Section section) {
		if (section.getRoles() == null) {
			enabled = true;
		}

		boolean ok = false;
		for (int i = 0; i < section.getRoles().length(); i++) {
			String role = section.getRoles().get(i);
			ok |= net.bluemind.ui.common.client.forms.Ajax.TOKEN.getRoles().contains(role);
		}
		enabled = ok;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setActive(boolean active) {
		if (active) {
			addStyleName(style.active());
			setWidget(0, 0, iconActive);
		} else {
			removeStyleName(style.active());
			setWidget(0, 0, icon);
		}
	}

	private SectionAnchor createSectionAnchor(Section section) {
		String sectionName = section.getName();
		SectionAnchor a = new SectionAnchor(sectionName);
		final String sectId = section.getId();
		a.setSectionId(sectId);

		// FIXME disabled ??
		if (enabled) {
			// a.setDisabled();
		} else {
			a.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					Actions.get().showWithParams2(sectId, null);
				}
			});
		}
		return a;
	}

	public void notifySectionAction(String screen) {
		setActive(screen != null ? screen.equals(id) : false);
	}
}
