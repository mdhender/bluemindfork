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
package net.bluemind.ui.adminconsole.directory.calendar;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JsArray;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot.SizeHint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.directory.BaseQCreateScreen;
import net.bluemind.ui.adminconsole.directory.calendar.l10n.CalendarConstants;

public class QCreateCalendarScreen extends BaseQCreateScreen {

	private static final String TYPE = "bm.ac.QCreateCalendarScreen";

	public QCreateCalendarScreen(ScreenRoot screen) {
		super(screen);
		title.setInnerText(CalendarConstants.INST.newCalendar());
		icon.setStyleName("fa fa-2x fa-calendar");
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new QCreateCalendarScreen(screenRoot);
			}
		});
	}

	@Override
	protected void doCancel() {
		Actions.get().showWithParams2("directory", null);
	}

	@Override
	protected void doEditCreated() {
		String domainUid = rootScreen.getModel().<JsMapStringJsObject> cast().getString("domainUid");
		String calendarUid = rootScreen.getModel().<JsMapStringJsObject> cast().getString("calendarUid");
		Map<String, String> params = new HashMap<>();
		params.put("domainUid", domainUid);
		params.put("entryUid", calendarUid);
		Actions.get().showWithParams2("editCalendar", params);
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("qcCalendar", TYPE).cast();
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(400, 360));
		screenRoot.getHandlers()
				.push(ModelHandler.create(null, QCreateCalendarModelHandler.TYPE).<ModelHandler> cast());

		JsArray<ScreenElement> comp = JsArray.createArray().cast();
		comp.push(ScreenElement.create(null, NewCalendar.TYPE));

		screenRoot.setContent(ContainerElement.create("qcCalendarContainer", comp));
		return screenRoot;
	}

}
