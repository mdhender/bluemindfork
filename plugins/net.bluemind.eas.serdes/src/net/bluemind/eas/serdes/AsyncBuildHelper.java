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
package net.bluemind.eas.serdes;

import java.util.Iterator;

import net.bluemind.eas.dto.base.Callback;

public class AsyncBuildHelper<T, BUILDER> implements Callback<BUILDER> {

	public static interface IBuildOperation<T, B> {
		void beforeAsync(B b, T t, Callback<B> forAsync);

		void afterAsync(B b, T t);
	}

	private Iterator<T> iterator;
	private IBuildOperation<T, BUILDER> operation;
	private T current;
	private Callback<BUILDER> done;

	public AsyncBuildHelper(Iterator<T> it, IBuildOperation<T, BUILDER> bo, Callback<BUILDER> done) {
		this.iterator = it;
		this.operation = bo;
		this.done = done;
	}

	@Override
	public void onResult(BUILDER data) {
		operation.afterAsync(data, current);
		build(data);
	}

	public void build(BUILDER builder) {
		if (iterator.hasNext()) {
			current = iterator.next();
			operation.beforeAsync(builder, current, this);
		} else {
			done.onResult(builder);
		}
	}

}