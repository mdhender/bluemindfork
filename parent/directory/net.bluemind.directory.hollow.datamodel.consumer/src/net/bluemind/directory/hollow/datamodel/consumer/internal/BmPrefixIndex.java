/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.directory.hollow.datamodel.consumer.internal;

import com.netflix.hollow.core.index.HollowPrefixIndex;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

public class BmPrefixIndex extends HollowPrefixIndex {

	public BmPrefixIndex(HollowReadStateEngine readStateEngine, String type, String fieldPath) {
		this(readStateEngine, type, fieldPath, 4);
	}

	public BmPrefixIndex(HollowReadStateEngine readStateEngine, String type, String fieldPath,
			int estimatedMaxStringDuplicates) {
		super(readStateEngine, type, fieldPath, estimatedMaxStringDuplicates);
	}

	@Override
	protected long estimateNumNodes(long totalWords, long averageWordLen) {
		// ProducerTests deleteGroup fails with a too low estimate
		return 10 * super.estimateNumNodes(totalWords, averageWordLen);
	}

}
