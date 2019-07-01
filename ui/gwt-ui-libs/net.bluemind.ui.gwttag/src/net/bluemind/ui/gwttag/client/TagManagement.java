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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.tag.api.ITagsAsync;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.gwt.endpoint.TagsGwtEndpoint;
import net.bluemind.tag.api.gwt.js.JsTag;
import net.bluemind.tag.api.gwt.serder.TagGwtSerDer;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.ColorBox;
import net.bluemind.ui.common.client.forms.tag.UUID;
import net.bluemind.ui.common.client.icon.Trash;
import net.bluemind.ui.gwttag.client.l10n.TagManagementConstants;

public class TagManagement extends CompositeGwtWidgetElement {

	@UiField
	FlexTable table;

	@UiField
	TextBox label;

	@UiField
	ColorBox color;

	@UiField
	Button addTag;

	private static TagManagementUiBinder uiBinder = GWT.create(TagManagementUiBinder.class);

	interface TagManagementUiBinder extends UiBinder<HTMLPanel, TagManagement> {
	}

	public static interface Resources extends ClientBundle {

		@Source("TagManagement.css")
		Style editStyle();

	}

	private static final Resources res = GWT.create(Resources.class);

	public static interface Style extends CssResource {

		String container();

		String icon();

	}

	private final Style s;
	private HTMLPanel form;
	private List<ItemValue<Tag>> tags = new ArrayList<>();

	private boolean domain;

	public TagManagement(boolean domain) {
		super();
		this.domain = domain;
		s = res.editStyle();
		s.ensureInjected();
		form = uiBinder.createAndBindUi(this);
		initWidget(form);
		table.setStyleName(s.container());
		table.getColumnFormatter().addStyleName(1, s.icon());

		addTag.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				if (label.getText().isEmpty()) {
					Window.alert(TagManagementConstants.INST.emptyLabel());

				} else {
					addTag();
				}
			}
		});

		label.addKeyDownHandler(new KeyDownHandler() {
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					if (label.getText().isEmpty()) {
						Window.alert(TagManagementConstants.INST.emptyLabel());
					} else {
						addTag();
					}
				}
			}

		});

	}

	private void addTag() {
		Tag t = new Tag();
		t.label = label.getText();
		t.color = color.getValue().getRGB();
		final ItemValue<Tag> value = new ItemValue<>();
		value.uid = UUID.uuid();
		value.value = t;
		addEntry(value, false);
		resetForm();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		doLoad(model);

		String domainUid = null;
		try {
			final TagsModel tagsModel = model.cast();
			domainUid = tagsModel.getDomainUid();
			if (null == domainUid || domainUid.trim().length() == 0) {
				throw new NullPointerException();
			}
		} catch (Exception e) {
			domainUid = Ajax.TOKEN.getContainerUid();
		}

		if (domainUid.equals("global.virt")) {
			return;
		}

		ITagsAsync domainTags = new TagsGwtEndpoint(Ajax.TOKEN.getSessionId(), "tags_" + domainUid);

		final String containerId = domainUid;
		domainTags.all(new AsyncHandler<List<ItemValue<Tag>>>() {

			@Override
			public void success(List<ItemValue<Tag>> value) {
				for (ItemValue<Tag> tag : value) {
					addEntry(tag, true);
				}
			}

			@Override
			public void failure(Throwable e) {
				GWT.log("Cannot load tags of container " + "tags_" + containerId);
			}
		});
	}

	protected void doLoad(JavaScriptObject model) {

		resetForm();
		tags.clear();
		table.removeAllRows();

		final TagsModel tagsModel = model.cast();

		JsArray<JsItemValue<JsTag>> tags = tagsModel.getTags();
		for (int i = 0; i < tags.length(); i++) {
			JsItemValue<JsTag> tag = tags.get(i);
			addEntry(new ItemValueGwtSerDer<>(new TagGwtSerDer()).deserialize(new JSONObject(tag)), false);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {

		JsArray<JsItemValue<JsTag>> currentTags = JsArray.createArray().cast();

		for (ItemValue<Tag> tag : tags) {
			JsItemValue<JsTag> itemValue = new ItemValueGwtSerDer<>(new TagGwtSerDer()).serialize(tag).isObject()
					.getJavaScriptObject().cast();
			currentTags.push(itemValue);
		}

		TagsModel tagsModel = model.cast();
		tagsModel.setCurrentTags(currentTags);
	}

	public void addEntry(final ItemValue<Tag> t, boolean readOnly) {
		if (isAlreadyPresent(t)) {
			return;
		}
		if (!readOnly) {
			tags.add(t);
		}
		final String key = t.uid;
		final int row = table.getRowCount();
		int i = 0;
		Trash trash = null;
		final TagInlineEditor edit = new TagInlineEditor(t.value, readOnly);
		edit.addValueChangeHandler(new ValueChangeHandler<Tag>() {

			@Override
			public void onValueChange(ValueChangeEvent<Tag> event) {
				if (edit.getValue().label.isEmpty()) {
					// FIXME
					// cCtrl.showError(TagManagementConstants.INST
					// .emptyLabel());
				} else {
					t.value = edit.getValue();
				}
			}
		});
		table.setWidget(row, i++, edit);

		if (readOnly) {
			Label readOnlyIcon = new Label();
			readOnlyIcon.setStyleName("fa fa-chain fa-lg");
			table.setWidget(row, i++, trash);
		} else {
			trash = new Trash();
			trash.setId("tag-management-trash-" + key);
			trash.addClickHandler(new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					if (Window.confirm(TagManagementConstants.INST.confirmDelete(t.value.label))) {
						tags.remove(t);
						Cell c = table.getCellForEvent(event);
						table.removeRow(c.getRowIndex());
					}
				}
			});
			table.setWidget(row, i++, trash);

		}

	}

	private boolean isAlreadyPresent(ItemValue<Tag> t) {
		for (ItemValue<Tag> tag : tags) {
			if (tag.uid.equals(t.uid)) {
				return true;
			}
		}
		return false;
	}

	private void resetForm() {
		label.setValue("");
		color.setValue(null);
	}

	public Widget asWidget() {
		return form;
	}

}
