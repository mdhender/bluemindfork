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
package net.bluemind.ui.adminconsole.system.domains.edit.indexing;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.tag.api.TagDescriptor;
import net.bluemind.ui.adminconsole.system.domains.assignments.AssignmentWidget;

public class EditDomainIndexingEditor extends AssignmentWidget {

	public static final String TYPE = "bm.ac.EditDomainIndexingEditor";
	private static EditDomainIndexingEditorUiBinder uiBinder = GWT.create(EditDomainIndexingEditorUiBinder.class);

	interface EditDomainIndexingEditorUiBinder extends UiBinder<HTMLPanel, EditDomainIndexingEditor> {
	}

	@UiField
	ListBox indexing;

	@Override
	protected List<TagListBoxMapping> getMapping() {
		List<AssignmentWidget.TagListBoxMapping> mapping = Arrays.asList(new AssignmentWidget.TagListBoxMapping[] {
				new AssignmentWidget.TagListBoxMapping(TagDescriptor.bm_es.getTag(), indexing) });
		return mapping;
	}

	protected EditDomainIndexingEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditDomainIndexingEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		super.saveModel(model);
	}

}
