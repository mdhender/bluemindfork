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
package net.bluemind.ui.gwttag.client;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;

public class TagsEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.tag.TagsEditor";
	private TagManagement tags;

	protected TagsEditor() {
		this(false);
	}

	public TagsEditor(boolean domain) {

		tags = new TagManagement(domain);
		initWidget(tags);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new TagsEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject m) {
		TagsModel model = m.cast();
		tags.loadModel(model);
	}

	@Override
	public void saveModel(JavaScriptObject m) {
		tags.saveModel(m);
	}

}
