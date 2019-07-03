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

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.tag.api.Tag;
import net.bluemind.ui.common.client.forms.Color;
import net.bluemind.ui.common.client.forms.ColorBox;
import net.bluemind.ui.common.client.icon.Trash;

public class TagItem extends Composite {

	private final String uid;
	private final TextBox label;
	private final ColorBox colorBox;
	private final Trash trash;

	public TagItem(TagValue tag) {
		uid = tag.uid;
		FlexTable ft = new FlexTable();
		initWidget(ft);

		label = new TextBox();
		label.setWidth("400px");
		label.setText(tag.label);

		ft.setWidget(0, 0, label);

		trash = new Trash();
		ft.setWidget(0, 2, trash);
		ft.getFlexCellFormatter().setRowSpan(0, 2, 2);

		colorBox = new ColorBox();
		ft.setWidget(1, 0, colorBox);
		colorBox.setValue(new Color(tag.color));
		ft.getFlexCellFormatter().setRowSpan(1, 0, 2);
	}

	public Trash getTrash() {
		return trash;
	}

	public TagValue getValue() {
		return new TagValue(uid, label.getText(), colorBox.getValue().getRGB());
	}

	public static class TagValue {
		public final String uid;
		public final String label;
		public final String color;

		public TagValue(String uid, String label, String color) {
			if (null == uid) {
				uid = "";
			}
			this.uid = uid;
			this.label = label;
			this.color = color;
		}

		public static TagValue empty() {
			return new TagValue(null, "", "000000");
		}

		public Tag toDomainTag() {
			Tag tag = new Tag();
			tag.color = color;
			tag.label = label;
			return tag;
		}
	}
}
