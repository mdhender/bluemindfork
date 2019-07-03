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

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

public class EntitySuggestion<T, TQ> implements Suggestion {

	private T entity;
	private IEntityFinder<T, TQ> finder;
	private String query;

	public EntitySuggestion(T t, IEntityFinder<T, TQ> finder, String query) {
		this.query = query;
		this.entity = t;
		this.finder = finder;
	}

	@Override
	public String getDisplayString() {
		String disp = finder.toString(entity);
		RegExp r = RegExp.compile(RegExp.quote(query), "gi");

		StringBuffer sb = new StringBuffer();
		int index = 0;
		for (MatchResult matcher = r.exec(disp); matcher != null; matcher = r.exec(disp)) {
			sb.append(disp.substring(index, matcher.getIndex()));
			sb.append("<b>" + matcher.getGroup(0) + "</b>");
			index = matcher.getIndex() + matcher.getGroup(0).length();
		}

		sb.append(disp.substring(index));
		String displayString = sb.toString();
		return displayString;
	}

	@Override
	public String getReplacementString() {
		// FIXME return "[eid:" + entity.getEntityId() + ";tid:" +
		// entity.getId() + "]";
		return null;
	}

	public T getEntity() {
		return entity;
	}

}
