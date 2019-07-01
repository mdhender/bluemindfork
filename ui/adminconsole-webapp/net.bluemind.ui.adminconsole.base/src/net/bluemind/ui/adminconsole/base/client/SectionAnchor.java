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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Anchor;

public class SectionAnchor extends Anchor {

	private String sectionId;
	private SAStyle style;

	public interface SABundle extends ClientBundle {

		@Source("SectionAnchor.css")
		SAStyle getStyle();

	}

	public interface SAStyle extends CssResource {

		String active();

		String sAnchor();

		String disabled();

	}

	public static final SABundle sab = GWT.create(SABundle.class);

	public SectionAnchor(String sectionName) {
		super(sectionName);
		this.style = sab.getStyle();
		style.ensureInjected();
		addStyleName(style.sAnchor());
	}

	public void setHighlighted(boolean h) {
		if (h) {
			addStyleName(style.active());
		} else {
			removeStyleName(style.active());
		}
	}

	public String getSectionId() {
		return sectionId;
	}

	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	public void setDisabled() {
		addStyleName(style.disabled());
	}
}
