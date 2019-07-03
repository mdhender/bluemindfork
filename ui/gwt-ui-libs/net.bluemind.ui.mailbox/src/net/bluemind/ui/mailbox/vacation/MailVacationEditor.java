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
package net.bluemind.ui.mailbox.vacation;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.mailbox.api.gwt.js.JsMailFilterVacation;
import net.bluemind.mailbox.api.gwt.serder.MailFilterVacationGwtSerDer;
import net.bluemind.ui.mailbox.filter.MailSettingsModel;

public class MailVacationEditor extends CompositeGwtWidgetElement {

	public static String TYPE = "bm.mail.MailVacationEditor";
	private VacationEdit vacationEdit;

	public MailVacationEditor() {
		FlowPanel panel = new FlowPanel();
		Label title = new Label(VacationConstants.INST.vacationTitle());
		title.setStyleName("sectionTitle");
		panel.add(title);
		vacationEdit = new VacationEdit();
		panel.add(vacationEdit);

		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);
		MailFilter mf = model.getMailFilter();

		if (mf == null) {
			asWidget().setVisible(false);
		} else {
			asWidget().setVisible(true);
			if (mf.vacation == null) {
				vacationEdit.setValue(new MailFilter.Vacation());
			} else {
				vacationEdit.setValue(mf.vacation);
			}
		}

	}

	@Override
	public void saveModel(JavaScriptObject m) {
		MailSettingsModel model = MailSettingsModel.get(m);
		MailFilter mf = model.getMailFilter();

		if (mf != null) {

			Vacation value = vacationEdit.getValue();
			if (value.enabled && (value.subject == null || vacationEdit.getValue().subject.trim().isEmpty())) {
				throw new ServerFault(VacationConstants.INST.emptySubject());
			}

			if (value.enabled && value.start == null) {
				throw new ServerFault(VacationConstants.INST.emptyDtStart());
			}

			model.getJsMailFilter().setVacation(new MailFilterVacationGwtSerDer().serialize(value).isObject()
					.getJavaScriptObject().<JsMailFilterVacation> cast());

		}
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.mail.MailVacationEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement we) {
						return new MailVacationEditor();
					}
				});
	}

}
