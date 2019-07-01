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
package net.bluemind.ui.common.client.forms.autocomplete;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;

import net.bluemind.core.api.ListResult;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder.ReloadCb;

/**
 * Extend this widget for entity auto-completes
 * 
 * 
 */
public abstract class EntityEdit<T, TQ> extends Composite {

	public static interface Resources extends ClientBundle {

		@Source("EntityEdit.css")
		Style entityEditStyle();

	}

	public static interface Style extends CssResource {

		String labelMandatory();

		String inputMandatory();

		String textInput();

		String selectedEntity();

		String deleteSelected();

		String inputTitle();

		String valuesPanel();

	}

	private static final Resources RES = GWT.create(Resources.class);

	private SuggestBox text;
	private final Label title;
	private final Style style;
	private final FlowPanel fp;
	private final Label mandatoryLabel;
	private final FlowPanel values;
	private final boolean multival;

	private final Set<T> selected;

	private final IEntityFinder<T, TQ> finder;

	private final String comboQuery;

	private ArrayList<T> candidates;

	private ListBox lb;

	public EntityEdit(IEntityFinder<T, TQ> finder, boolean multival, boolean mandatory, String comboQuery) {
		this.multival = multival;
		this.finder = finder;
		selected = new HashSet<T>();
		fp = new FlowPanel();

		title = new Label();

		values = new FlowPanel();
		fp.add(values);

		mandatoryLabel = new Label();
		setMandatory(mandatory);
		style = RES.entityEditStyle();

		style.ensureInjected();

		this.comboQuery = comboQuery;
		if (comboQuery == null) {
			createAutocomplete();
		} else {
			createCombo();
		}

		fp.add(mandatoryLabel);

		initWidget(fp);
	}

	private void createCombo() {
		this.lb = new ListBox();
		lb.setMultipleSelect(multival);
		if (multival) {
			lb.setVisibleItemCount(2);
		}
		values.add(lb);
		this.candidates = null;
		lb.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				selected.clear();
				if (candidates == null) {
					return;
				}
				if (multival) {
					for (int i = 0; i < lb.getItemCount(); i++) {
						if (lb.isItemSelected(i)) {
							selected.add(candidates.get(i));
						}
					}
					return;
				}

				int idx = lb.getSelectedIndex();
				if (idx == -1) {
					return;
				}

				if (!isMandatory() && !multival) {
					T entity = candidates.get(idx - 1);
					selected.add(entity);
				} else if (!multival) {
					T entity = candidates.get(idx);
					selected.add(entity);
				}
			}
		});

		fillComboValues();
	}

	protected void fillComboValues() {
		finder.find(finder.queryFromString(comboQuery), new DefaultAsyncHandler<ListResult<T>>() {

			@Override
			public void success(ListResult<T> result) {
				lb.clear();
				ArrayList<T> tmp = new ArrayList<T>((int) result.total);
				selected.clear();
				if (!isMandatory() && !multival) {
					lb.addItem("---");
				}
				for (T entity : result.values) {
					tmp.add(entity);
					lb.addItem(finder.toString(entity));
				}

				if (result.values.size() > 0) {
					if (isMandatory()) {
						select(tmp.get(0));
					}
					candidates = tmp;
				}
			}
		});
	}

	private void createAutocomplete() {
		SuggestOracle oracle = new EntitySuggestOracle<>(finder);
		this.text = new SuggestBox(oracle);
		text.addStyleName(style.textInput());
		this.text.addSelectionHandler(new EntitySelectionHandler<T, TQ>(this));
		values.addStyleName(style.valuesPanel());
		values.add(text);
	}

	public void select(T suggested) {
		boolean added = selected.add(suggested);
		if (text != null) {
			if (added) {
				SelectedEntity<T, TQ> selEntity = new SelectedEntity<>(suggested, this);
				values.insert(selEntity, values.getWidgetCount() - 1);
			}
			text.setValue("", false);
			if (!multival) {
				text.setVisible(false);
			}
		}
	}

	public void deselect(SelectedEntity<T, TQ> selEntity) {
		T entity = selEntity.getEntity();
		selected.remove(entity);
		values.remove(selEntity);
		if (!multival) {
			text.setVisible(true);
		}
	}

	public String getTitleText() {
		return title.getText();
	}

	public void setTitleText(String title) {
		this.title.setText(title);
	}

	Style getStyle() {
		return style;
	}

	public void setMandatory(boolean b) {
		if (b) {
			mandatoryLabel.setText("*");
			title.addStyleName(style.labelMandatory());
		} else {
			mandatoryLabel.setText("");
		}
	}

	public boolean isMandatory() {
		return mandatoryLabel.getText().contains("*");
	}

	public boolean isMultival() {
		return multival;
	}

	public IEntityFinder<T, TQ> getFinder() {
		return finder;
	}

	/**
	 * Gets the selected entities
	 * 
	 * @return
	 */
	public Set<T> getValues() {
		if (lb != null && selected.isEmpty()) {

		}
		return selected;
	}

	/**
	 * Sets selected entities
	 * 
	 * @param entities
	 */
	public void setValues(final Collection<T> entities) {
		if (text != null) {
			// autocomplete mode
			values.clear();
			values.add(text);
			text.setVisible(multival || entities.size() != 1);
		} else if (lb != null) {
			// combo box mode
			if (candidates == null) {
				// candidates is filled by an async call, so "loop" until
				// candidates is not null
				Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
					@Override
					public boolean execute() {
						setValues(entities);
						return false;
					}
				}, 50);
				return;
			} else {
				if ((entities == null || entities.isEmpty()) && !isMandatory()) {
					lb.setSelectedIndex(0);
				} else {
					for (T entity : entities) {
						int idx = candidates.indexOf(entity);
						if (!isMandatory() && !multival) {
							idx++;
						}
						if (!multival) {
							lb.setSelectedIndex(idx);
						} else {
							lb.setItemSelected(idx, true);
						}
					}
					if (multival) {
						selected.clear();
						selected.addAll(entities);
					}
				}
			}
		}

		selected.clear();
		for (T e : entities) {
			select(e);
		}
	}

	/**
	 * Disabling the widget clears the stored values
	 * 
	 * @param b
	 */
	public void setEnabled(boolean b) {
		setValues(Collections.<T> emptyList());
		if (text != null) {
			text.getValueBox().setEnabled(b);
		}
	}

	public void reload(Collection<T> ids, ReloadCb<T> cb) {
		cb.finished(Collections.<T> emptyList());
	}

}
