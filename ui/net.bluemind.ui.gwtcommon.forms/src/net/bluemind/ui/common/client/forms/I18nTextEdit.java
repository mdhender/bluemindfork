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
package net.bluemind.ui.common.client.forms;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;
import net.bluemind.ui.common.client.icon.famfamfam.FamFamFam;

public class I18nTextEdit extends Composite implements IsEditor<ValueBoxEditor<String>>, ICommonEditor {

	public static interface Resources extends ClientBundle {

		@Source("I18nTextEdit.css")
		Style stringEditStyle();

	}

	public static interface Style extends CssResource {

		String mandatory();

		String container();

		String textBox();

		String prependedText();

		String flag();

	}

	private static final Resources RES = GWT.create(Resources.class);
	private TrPanel tr;
	private Label title;
	private Label mandatoryLabel;
	private Style s;
	private Map<String, TextBox> texts;
	private String locale;
	private FlowPanel container;
	private TextBox fr;
	private TextBox en;

	public I18nTextEdit() {
		locale = LocaleInfo.getCurrentLocale().getLocaleName();
		if (locale.length() > 2) {
			locale = locale.substring(0, 2);
		}

		texts = new HashMap<String, TextBox>();

		s = RES.stringEditStyle();
		s.ensureInjected();

		tr = new TrPanel();
		tr.addStyleName("setting");

		title = new Label();
		tr.add(title, "label");

		container = new FlowPanel();

		FlowPanel fpFr = new FlowPanel();
		fpFr.setStyleName(s.container());

		SimplePanel spFr = new SimplePanel();
		Image imgFr = new Image(FamFamFam.INST.fr());
		imgFr.setStyleName(s.flag());
		spFr.add(imgFr);
		spFr.setStyleName(s.prependedText());

		fr = new TextBox();
		fr.setStyleName(s.textBox());
		fpFr.add(spFr);
		fpFr.add(fr);
		fr.setText(null);
		texts.put("fr", fr);
		container.add(fpFr);

		FlowPanel fpEn = new FlowPanel();
		fpEn.setStyleName(s.container());

		SimplePanel spEn = new SimplePanel();
		Image imgEn = new Image(FamFamFam.INST.en());
		imgEn.setStyleName(s.flag());
		spEn.add(imgEn);
		spEn.setStyleName(s.prependedText());

		en = new TextBox();
		en.setStyleName(s.textBox());
		fpEn.add(spEn);
		fpEn.add(en);
		en.setText(null);
		texts.put("en", en);
		container.add(fpEn);

		tr.add(container, "form");

		this.mandatoryLabel = new Label();
		tr.add(mandatoryLabel);
		initWidget(tr);
	}

	public I18nTextEdit(String titleText, boolean mandatory) {
		this();
		setTitleText(titleText);
		setMandatory(mandatory);
	}

	public void setMandatory(boolean b) {
		if (b) {
			mandatoryLabel.setText("*");
			tr.addStyleName(s.mandatory());
		} else {
			mandatoryLabel.setText("");
			tr.removeStyleName(s.mandatory());
		}
	}

	public void setFocus(Boolean focus) {
		texts.get(locale).setFocus(focus);
	}

	public void setMaxLength(int maxLength) {
		for (String k : texts.keySet()) {
			texts.get(k).setMaxLength(maxLength);
		}
	}

	@Override
	public void setTitleText(String s) {
		title.setText(s);
	}

	@Override
	public String getStringValue() {
		String s = "";
		String nl = "";
		for (String k : texts.keySet()) {
			s += nl + k + "::" + texts.get(k).getValue();
			nl = "\n";
		}
		return s;
	}

	@Override
	public void setStringValue(String v) {
		String[] labels = v.split("\n");
		for (String k : labels) {
			String[] s = k.split("::");
			if (texts.containsKey(s[0])) {
				texts.get(s[0]).setText(s[1]);
			}
		}

	}

	@Override
	public void setDescriptionText(String s) {
	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		Map<String, Widget> ret = new HashMap<String, Widget>();
		ret.put("label", title);
		ret.put("form", container);
		return ret;
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	public ValueBoxEditor<String> asEditor() {
		return null;
	}

	public void reset() {
		for (String k : texts.keySet()) {
			texts.get(k).setValue(null);
		}
	}

	@Override
	public void setReadOnly(boolean readOnly) {
	}

	@Override
	public void addFormChangeListener(IFormChangeListener listener) {
	}

	@Override
	public void setId(String id) {
		fr.getElement().setId(id + "-fr");
		en.getElement().setId(id + "-en");
	}
}
