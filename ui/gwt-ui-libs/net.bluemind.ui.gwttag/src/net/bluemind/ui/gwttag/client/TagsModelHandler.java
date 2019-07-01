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

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.tag.api.TagChanges;

public abstract class TagsModelHandler implements IGwtModelHandler {

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final TagsModel tagsModel = model.cast();
		loadImpl(tagsModel, handler);
	}

	protected abstract void loadImpl(TagsModel tagsModel, AsyncHandler<Void> handler);

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final TagsModel tagsModel = model.cast();

		TagChanges changeset = new TagChangeSet(tagsModel.getCurrentTags(), tagsModel.getTags()).get();
		saveImpl(changeset, tagsModel, handler);
	}

	protected abstract void saveImpl(TagChanges changeset, TagsModel tagsModel, AsyncHandler<Void> handler);

}
