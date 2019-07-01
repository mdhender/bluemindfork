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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.tag.api.TagChanges;
import net.bluemind.tag.api.TagChanges.ItemAdd;
import net.bluemind.tag.api.TagChanges.ItemDelete;
import net.bluemind.tag.api.TagChanges.ItemModify;
import net.bluemind.tag.api.gwt.js.JsTag;
import net.bluemind.tag.api.gwt.serder.TagGwtSerDer;

public class TagChangeSet {
	private final JsArray<JsItemValue<JsTag>> tagValues;
	private final JsArray<JsItemValue<JsTag>> currentValues;

	public TagChangeSet(JsArray<JsItemValue<JsTag>> currentValues, JsArray<JsItemValue<JsTag>> tagValues) {
		this.currentValues = currentValues;
		this.tagValues = tagValues;
	}

	public TagChanges get() {
		List<ItemModify> modifiedTags = new ArrayList<>();

		List<ItemDelete> deletedTags = new ArrayList<>();
		List<ItemAdd> newTags = new ArrayList<>();

		for (int i = 0; i < currentValues.length(); i++) {
			JsItemValue<JsTag> tag = currentValues.get(i);
			JsItemValue<JsTag> oldValue = contains(tag, tagValues);
			if (oldValue != null) {

				if (!Equals(oldValue.getValue(), tag.getValue())) {
					ItemModify item = new ItemModify();
					item.uid = tag.getUid();

					item.value = new TagGwtSerDer().deserialize(new JSONObject(tag.getValue()));
					modifiedTags.add(item);

				}
			} else {
				String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
				ItemAdd item = new ItemAdd();
				item.uid = uid;
				item.value = new TagGwtSerDer().deserialize(new JSONObject(tag.getValue()));
				newTags.add(item);
			}
		}

		for (int i = 0; i < tagValues.length(); i++) {
			JsItemValue<JsTag> oldValue = tagValues.get(i);
			JsItemValue<JsTag> newValue = contains(oldValue, currentValues);
			if (newValue == null) {
				ItemDelete item = new ItemDelete();
				item.uid = oldValue.getUid();
				deletedTags.add(item);
			}
		}

		TagChanges changeset = new TagChanges();
		changeset.add = newTags;
		changeset.delete = deletedTags;
		changeset.modify = modifiedTags;

		return changeset;
	}

	private boolean Equals(JsTag jsTag, JsTag jsTag2) {

		return jsTag.getColor().equals(jsTag2.getColor()) && jsTag.getLabel().equals(jsTag2.getLabel());
	}

	private JsItemValue<JsTag> contains(JsItemValue<JsTag> tag, JsArray<JsItemValue<JsTag>> tags) {
		for (int i = 0; i < tags.length(); i++) {
			JsItemValue<JsTag> cTag = tags.get(i);
			if (cTag.getUid().equals(tag.getUid())) {
				return cTag;
			}
		}
		return null;
	}

}
