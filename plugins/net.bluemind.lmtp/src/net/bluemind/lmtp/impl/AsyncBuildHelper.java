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
package net.bluemind.lmtp.impl;

import java.util.Iterator;

public class AsyncBuildHelper<T> implements Callback {

	public static interface IBuildOperation<T> {
		void beforeAsync(T t, Callback forAsync);

		void afterAsync(T t);
	}

	private Iterator<T> iterator;
	private IBuildOperation<T> operation;
	private T current;
	private Callback done;

	public AsyncBuildHelper(Iterator<T> it, IBuildOperation<T> bo, Callback done) {
		this.iterator = it;
		this.operation = bo;
		this.done = done;
	}

	@Override
	public void onResult() {
		operation.afterAsync(current);
		build();
	}

	public void build() {
		if (iterator.hasNext()) {
			current = iterator.next();
			operation.beforeAsync(current, this);
		} else {
			done.onResult();
		}
	}
}
