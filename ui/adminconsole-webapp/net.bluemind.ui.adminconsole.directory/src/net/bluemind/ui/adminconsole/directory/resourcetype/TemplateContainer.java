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

import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;

public class TemplateContainer extends Composite {
	private static final String WIDTH = "32.7rem";
	private static final String NEW_BUTTON = "New";
	private static final List<String> SUPPORTED_LANGUAGES = Arrays
			.asList(new String[] { "de", "en", "es", "fr", "hu", "it", "nl", "pl", "pt", "ru", "sk", "uk", "zh" });
	private TabPanel tabPanel = new TabPanel();
	private List<TemplatePanel> templatePanels = new ArrayList<>();
	private Integer previouslySelected = -1;
	private Label removeTabTitle;
	private Label addTabTitle;

	@UiConstructor
	public TemplateContainer() {
		// initialize 'x' and '-' tab titles
		this.removeTabTitle = new Label("x");
		this.removeTabTitle.getElement().getStyle().setFontWeight(FontWeight.BOLD);
		this.removeTabTitle.getElement().getStyle().setColor("red");
		this.addTabTitle = new Label("+");
		this.addTabTitle.getElement().getStyle().setFontWeight(FontWeight.BOLD);
		this.addTabTitle.getElement().getStyle().setColor("green");

		// add listeners for the 'x' and '-' buttons
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
						tabPanel.insert(new Label(), removeTabTitle, 0);
					}
				} else if (previouslySelected != 0 && tabPanel.getWidgetCount() > 2 && event.getSelectedItem() == 0) {
					// delete previously selected template
					tabPanel.remove(previouslySelected);
					templatePanels.remove(previouslySelected - 1);

					if (tabPanel.getWidgetCount() == 2) {
						// remove 'delete' button
						tabPanel.remove(0);
					}
				}
				previouslySelected = event.getSelectedItem();
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
		this.tabPanel.add(new Label(), this.addTabTitle);

		if (this.tabPanel.getWidgetCount() > 1) {
			this.tabPanel.insert(new Label(), this.removeTabTitle, 0);
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
		private RichTextArea previewArea;
		private Button previewButton;
		private boolean isPreview = false;

		public TemplatePanel(final List<String> languages, final String selectedLanguage) {
			// language combo-box
			languageBox = new ListBox();
			int index = 0;
			for (final String language : languages) {
				languageBox.addItem(language);
				if (language.equals(selectedLanguage)) {
					languageBox.setSelectedIndex(index);
				}
				index++;
			}

			// template edition area
			templateArea = new TextArea();
			templateArea.setWidth("98%");
			templateArea.getElement().getStyle().setProperty("marginTop", "2px");

			// template preview area
			previewArea = new RichTextArea();
			previewArea.setWidth("98%");
			previewArea.getElement().getStyle().setProperty("marginTop", "2px");
			previewArea.setVisible(false);

			// edition/preview toggle button
			previewButton = new Button(ResourceTypeConstants.INST.templatePreviewButtonOn(), new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					if (isPreview) {
						// hide preview
						previewArea.setVisible(false);
						templateArea.setVisible(true);
						previewButton.setText(ResourceTypeConstants.INST.templatePreviewButtonOn());
					} else {
						// show preview
						previewArea.setWidth(String.valueOf(templateArea.getElement().getOffsetWidth() - 5) + "px");
						previewArea.setHeight(String.valueOf(templateArea.getElement().getOffsetHeight() - 7) + "px");
						previewArea.setHTML(templateArea.getText());
						previewArea.setVisible(true);
						templateArea.setVisible(false);
						previewButton.setText(ResourceTypeConstants.INST.templatePreviewButtonOff());
					}
					// toggle
					isPreview = !isPreview;
				}
			});

			this.add(languageBox);
			this.add(templateArea);
			this.add(previewArea);
			this.add(previewButton);
		}

		public TemplatePanel(final List<String> languages) {
			this(languages, null);
		}
	}

}
