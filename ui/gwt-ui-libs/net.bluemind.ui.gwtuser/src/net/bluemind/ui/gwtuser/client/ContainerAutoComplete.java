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
package net.bluemind.ui.gwtuser.client;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestOracle;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestion;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder;

public class ContainerAutoComplete extends Composite {

	private SuggestBox input;
	private final IEntityFinder<ContainerDescriptor, ContainerQuery> finder;
	private IContainerSelectTarget target;

	public ContainerAutoComplete(String title, IEntityFinder<ContainerDescriptor, ContainerQuery> finder) {
		this.finder = finder;

		SuggestOracle oracle = new EntitySuggestOracle<ContainerDescriptor, ContainerQuery>(finder);
		input = new SuggestBox(oracle);
		input.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {

			@Override
			public void onSelection(SelectionEvent<Suggestion> event) {
				@SuppressWarnings("unchecked")
				EntitySuggestion<ContainerDescriptor, ContainerQuery> es = (EntitySuggestion<ContainerDescriptor, ContainerQuery>) event
						.getSelectedItem();
				select(es.getEntity());
			}
		});

		input.getElement().setAttribute("placeholder", title);

		initWidget(input);
	}

	public void setTarget(IContainerSelectTarget cse) {
		target = cse;
	}

	public void select(ContainerDescriptor suggested) {
		if (input != null) {
			target.seletected(suggested);
			input.setValue("", false);
		}
	}

	public IEntityFinder<ContainerDescriptor, ContainerQuery> getFinder() {
		return finder;
	}

}
