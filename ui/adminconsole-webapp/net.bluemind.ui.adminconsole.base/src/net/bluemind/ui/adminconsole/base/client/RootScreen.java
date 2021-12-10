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

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.common.client.forms.Ajax;

public class RootScreen extends Composite implements IGwtScreenRoot {

	private SimplePanel panel;
	private RSStyle style;
	private List<Section> sections;

	public interface RSBundle extends ClientBundle {

		@Source("RootScreen.css")
		RSStyle getStyle();

	}

	public interface RSStyle extends CssResource {

		String bg();

		String imageContainer();

		String sAnchor();

		String screensList();

		String sectionBlock();

		String subSectionBlock();

		String subSectionContainer();

		String disabled();
	}

	public static final RSBundle bundle = GWT.create(RSBundle.class);

	public RootScreen() {
		this.panel = new SimplePanel();
		initWidget(panel);
		this.style = bundle.getStyle();
		style.ensureInjected();

		this.addStyleName(style.bg());

	}

	private void construct() {
		FlexTable ft = new FlexTable();
		panel.add(ft);
		ft.setStyleName(style.sectionBlock());

		for (int i = 0; i < sections.size(); i++) {
			int row = i / 2;
			int col = i % 2;

			Section section = sections.get(i);
			VerticalPanel vp = new VerticalPanel();
			FlexTable container = new FlexTable();

			Anchor sectionAnchor = createSectionAnchor(section);
			sectionAnchor.getElement().setId("root-screen-" + section.getId());

			boolean disabled = false;// FIXME disabled
										// section.hasAttribute("disabled");
			sectionAnchor.addStyleName(style.sAnchor());
			vp.add(sectionAnchor);

			// add topLvl screens
			BulletList bl = new BulletList();
			bl.addStyleName(style.screensList());
			for (int j = 0; j < section.getSections().length(); j++) {

				Section subSection = section.getSections().get(j);

				for (int k = 0; k < subSection.getScreens().length(); k++) {
					Screen screen = subSection.getScreens().get(k);

					boolean rpcDisabled = false;

					if (screen.isTopLevel()) {

						rpcDisabled = !isInRole(screen);

						Anchor a = new Anchor(screen.getName());
						final String id = screen.getId();
						a.getElement().setId(id);

						// a.ensureDebugId(id);

						if (disabled || rpcDisabled) {
							a.setStyleName(style.disabled());
						} else {
							a.addClickHandler(new ClickHandler() {

								@Override
								public void onClick(ClickEvent event) {
									Actions.get().showWithParams2(id, null);
								}
							});
						}
						bl.add(a);
					}
				}
			}

			vp.add(bl);

			container.setWidget(0, 0, vp);
			container.getFlexCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
			String iconStyle = section.getIconStyle();
			if (iconStyle != null) {
				Label label = new Label();
				label.setStyleName(iconStyle + " fa fa-4x");
				container.setWidget(0, 1, label);
				container.getCellFormatter().setStyleName(0, 1, style.imageContainer());
			}
			container.setStyleName(style.subSectionContainer());

			ft.setWidget(row, col, container);

			ft.getFlexCellFormatter().addStyleName(row, col, style.subSectionBlock());
			ft.getFlexCellFormatter().setVerticalAlignment(row, col, HasVerticalAlignment.ALIGN_TOP);
		}

	}

	private boolean isInRole(Screen screen) {
		if (screen.getRoles() == null) {
			return true;
		}

		if (Arrays.stream(screen.getRoles()).map(r -> Ajax.TOKEN.getRoles().contains(r)).filter(f -> f).count() > 0) {
			return true;
		} else if (screen.getOURoles() != null && Arrays.stream(screen.getOURoles()).map(r -> {
			return Ajax.TOKEN.getRolesByOrgUnits().values().stream().filter(ouRoles -> ouRoles.contains(r)).count() > 0;
		}).filter(f -> f).count() > 0) {
			return true;
		}

		return false;
	}

	private SectionAnchor createSectionAnchor(Section section) {
		String sectionName = section.getName();
		SectionAnchor a = new SectionAnchor(sectionName);
		final String sectId = section.getId();
		a.setSectionId(sectId);
		a.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().showWithParams2(sectId, null);
			}
		});

		return a;
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	public static void registerType() {
		GwtScreenRoot.register("bm.ac.RootScreen", new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new RootScreen();
			}
		});

		GWT.log("bm.ac.RootScreen registred");
	}

	public static native ScreenElement model()
	/*-{
		var ret = {
			'id' : null,
			'type' : 'bm.ac.RootScreen'
		};
		return ret;
	}-*/;

	@Override
	public void loadModel(JavaScriptObject model) {

	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

	@Override
	public void doLoad(ScreenRoot instance) {
		this.sections = AdminConsoleMenus.get().getRootAsList();
		construct();
	}

}
