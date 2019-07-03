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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.common.client.forms.Ajax;

public class SectionScreen extends Composite implements IGwtScreenRoot {

	private SimplePanel panel = new SimplePanel();
	private SSStyle style;
	private Section section;

	public interface SSBundle extends ClientBundle {

		@Source("SectionScreen.css")
		SSStyle getStyle();
	}

	public interface SSStyle extends CssResource {

		String image();

		String vPanel();

		String sTitle();

		String links();

		String first();

		String last();

		String bg();

		String disabled();

	}

	public static final SSBundle bundle = GWT.create(SSBundle.class);

	private ScreenRoot screenRoot;

	private SectionScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		initWidget(panel);
		this.style = bundle.getStyle();
		style.ensureInjected();
		this.addStyleName(style.bg());
	}

	public void construct() {
		FlowPanel vp = new FlowPanel();
		panel.add(vp);

		for (int j = 0; j < section.getSections().length(); j++) {

			final Section sub = section.getSections().get(j);

			// FIXME rpcs ?
			// if (sub.hasAttribute("rpcs")) {
			// boolean rpcsAllowed = true;
			// String[] rpcs = sub.getAttribute("rpcs").split(",");
			// for (String rpc : rpcs) {
			// if (!Ajax.TOKEN.getRoles().contains(rpc)) {
			// rpcsAllowed = false;
			// break;
			// }
			// }
			//
			// if (!rpcsAllowed) {
			// continue;
			// }
			// }

			VerticalPanel sPanel = new VerticalPanel();
			sPanel.addStyleName(style.vPanel());

			String icoStyle = sub.getIconStyle();
			if (icoStyle != null) {
				Label label = new Label();
				label.setStyleName(icoStyle + " fa");
				sPanel.add(label);
			} else {
				GWT.log("subsection without icon in menus.xml : " + sub.getId() + " : " + sub.getName());
			}

			HTML subName = new HTML("<h2>" + sub.getName() + "</h2>");
			subName.addStyleName(style.sTitle());
			sPanel.add(subName);
			JsArray<Screen> screens = sub.getScreens();
			BulletList bl = new BulletList();
			bl.addStyleName(style.links());

			int added = 0;

			for (int k = 0; k < screens.length(); k++) {
				final Screen screen = screens.get(k);

				boolean inRole = isInRole(screen);
				// FIXME check roles
				// if (screen.getRole()) {
				// String[] rpcs = screen.getAttribute("rpcs").split(",");
				// for (String rpc : rpcs) {
				// if (!Ajax.TOKEN.getRoles().contains(rpc)) {
				// rpcDisabled = true;
				// break;
				// }
				// }
				// }

				String scName = screen.getName();
				Anchor a = new Anchor(scName);
				a.getElement().setId("section-screen-" + screen.getId());
				String sName = null;
				if (added == 0) {
					sName = style.first();
				} else if (added == screens.length() - 1) {
					sName = style.last();
				}
				bl.add(a, sName);
				if (!inRole) {
					a.addStyleName(style.disabled());
				} else {
					a.addClickHandler(new ClickHandler() {

						@Override
						public void onClick(ClickEvent event) {
							Actions.get().showWithParams2(screen.getId(), null);
						}
					});
				}

				added++;
			}
			sPanel.add(bl);
			vp.add(sPanel);
		}
	}

	private boolean isInRole(Screen screen) {
		if (screen.getRoles() == null) {
			return true;
		}

		if (Arrays.stream(screen.getRoles()).map(r -> Ajax.TOKEN.getRoles().contains(r)).filter(f -> f).count() > 0) {
			return true;
		} else if (screen.getOURoles() != null && Arrays.stream(screen.getOURoles()).map(r -> {
			boolean hasRole = Ajax.TOKEN.getRolesByOrgUnits().values().stream().map(ouRoles -> ouRoles.contains(r))
					.filter(f -> f).count() > 0;
			return Ajax.TOKEN.getRolesByOrgUnits().values().stream().map(ouRoles -> ouRoles.contains(r)).filter(f -> f)
					.count() > 0;
		}).filter(f -> f).count() > 0) {
			return true;
		} else {
			return false;
		}
	}

	public static void registerType() {
		GwtScreenRoot.register("bm.ac.SectionScreen", new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new SectionScreen(screenRoot);
			}
		});
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	public void detach() {
		onDetach();
	}

	public native static ScreenRoot create(Section section)
	/*-{
    var ret = {
      'id' : section.id,
      'type' : 'bm.ac.SectionScreen',
      'model_' : section
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
		this.section = instance.getModel().cast();
		construct();
	}

}
