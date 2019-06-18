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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public class TemplateContainer extends Composite {
	private static final String WIDTH = "32.7rem";
	private static final String NEW_BUTTON = "New";
	private static final String MINUS_BUTTON = "-";
	private static final String PLUS_BUTTON = "+";
	private static final List<String> SUPPORTED_LANGUAGES = Arrays
			.asList(new String[] { "de", "en", "es", "fr", "hu", "it", "nl", "pl", "pt", "ru", "sk", "uk", "zh" });
	private TabPanel tabPanel = new TabPanel();
	private List<TemplatePanel> templatePanels = new ArrayList<>();

	@UiConstructor
	public TemplateContainer() {
		// add listeners for the '+' and '-' buttons
		this.tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {

			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				if (event.getSelectedItem() == tabPanel.getWidgetCount() - 1) {
					// add new template
					final TemplatePanel templatePanel = new TemplatePanel(availableLanguages());
					tabPanel.insert(templatePanel, NEW_BUTTON, tabPanel.getWidgetCount() - 1);
					tabPanel.selectTab(tabPanel.getWidgetCount() - 2);
					templatePanels.add(templatePanel);

					if (tabPanel.getWidgetCount() == 2) {
						// add 'delete' button
						tabPanel.insert(new Label(), MINUS_BUTTON, 0);
					}
				} else if (tabPanel.getWidgetCount() > 2 && event.getSelectedItem() == 0) {
					// delete last template
					tabPanel.remove(tabPanel.getWidgetCount() - 2);
					templatePanels.remove(templatePanels.size() - 1);

					if (tabPanel.getWidgetCount() == 2) {
						// remove 'delete' button
						tabPanel.remove(0);
					}
				}
			}
		});

		initWidget(tabPanel);
		tabPanel.setWidth(WIDTH);
	}

	/** @return the languages that are not already used */
	private List<String> availableLanguages() {
		final List<String> availableLanguages = new ArrayList<>(SUPPORTED_LANGUAGES);
		for (final TemplatePanel templatePanel : this.templatePanels) {
			availableLanguages.remove(templatePanel.languageBox.getSelectedItemText());
		}
		return availableLanguages;
	}

	/**
	 * Initialize this component with <i>templates</i>. <b>/!\ Should be called once
	 * only!</b>
	 */
	public void setTemplates(final Map<String, String> templates) {
		for (final Entry<String, String> entry : templates.entrySet()) {
			final TemplatePanel templatePanel = new TemplatePanel(SUPPORTED_LANGUAGES, entry.getKey());
			templatePanel.templateArea.setText(entry.getValue());
			this.tabPanel.add(templatePanel, entry.getKey());
			this.templatePanels.add(templatePanel);
		}
		this.tabPanel.add(new Label(), PLUS_BUTTON);

		if (this.tabPanel.getWidgetCount() > 1) {
			this.tabPanel.insert(new Label(), MINUS_BUTTON, 0);
			this.tabPanel.selectTab(1);
		}
	}

	public Map<String, String> getTemplates() {
		final Map<String, String> templates = new HashMap<>();
		for (final TemplatePanel templatePanel : this.templatePanels) {
			final String language = templatePanel.languageBox.getSelectedItemText();
			final String template = templatePanel.templateArea.getText();
			if (!template.trim().isEmpty()) {
				templates.put(language, template);
			}
		}
		return templates;
	}

	/**
	 * A simple panel with combo-box for the languages and a text area for the
	 * template.
	 */
	private class TemplatePanel extends VerticalPanel {
		private ListBox languageBox;
		private TextArea templateArea;

		public TemplatePanel(final List<String> languages, final String selectedLanguage) {
			languageBox = new ListBox();
			int index = 0;
			for (final String language : languages) {
				languageBox.addItem(language);
				if (language.equals(selectedLanguage)) {
					languageBox.setSelectedIndex(index);
				}
				index++;
			}
			templateArea = new TextArea();
			templateArea.setWidth("98%");
			templateArea.getElement().getStyle().setProperty("marginTop", "2px");
			this.add(languageBox);
			this.add(templateArea);
		}

		public TemplatePanel(final List<String> languages) {
			this(languages, null);
		}
	}

}
