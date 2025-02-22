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

import java.util.List;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.GwtSerDerUtils;
import net.bluemind.core.container.model.ContainerUpdatesResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagChanges;
import net.bluemind.tag.api.gwt.endpoint.TagsGwtEndpoint;
import net.bluemind.tag.api.gwt.js.JsTag;
import net.bluemind.tag.api.gwt.serder.TagGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainTagsModelHandler extends TagsModelHandler {

	public static final String TYPE = "bm.tag.DomainTagsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new DomainTagsModelHandler();
			}
		});
	}

	@Override
	protected void loadImpl(final TagsModel tagsModel, final AsyncHandler<Void> handler) {
		GWT.log("loading tags for domain " + tagsModel.getDomainUid());
		TagsGwtEndpoint tagService = new TagsGwtEndpoint(Ajax.TOKEN.getSessionId(), "tags_" + tagsModel.getDomainUid());
		tagService.all(new DefaultAsyncHandler<List<ItemValue<Tag>>>(handler) {

			@Override
			public void success(List<ItemValue<Tag>> tagValues) {
				JsArray<JsItemValue<JsTag>> tags = new GwtSerDerUtils.ListSerDer<>(
						new ItemValueGwtSerDer<>(new TagGwtSerDer())).serialize(tagValues).isArray()
								.getJavaScriptObject().cast();

				tagsModel.setTags(tags);
				tagsModel.setCurrentTags(tags);
				handler.success(null);
			}
		});

	}

	@Override
	protected void saveImpl(TagChanges changeset, TagsModel tagsModel, final AsyncHandler<Void> handler) {
		TagsGwtEndpoint tagService = new TagsGwtEndpoint(Ajax.TOKEN.getSessionId(), "tags_" + tagsModel.getDomainUid());
		tagService.updates(changeset, new DefaultAsyncHandler<ContainerUpdatesResult>(handler) {

			@Override
			public void success(ContainerUpdatesResult value) {
				handler.success(null);
			}
		});
	}

}
