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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.core.index.HollowPrefixIndex;
import com.netflix.hollow.core.read.engine.HollowReadStateEngine;

public class BmPrefixIndex extends HollowPrefixIndex {

	private static final Logger logger = LoggerFactory.getLogger(BmPrefixIndex.class);

	public BmPrefixIndex(HollowReadStateEngine readStateEngine, String type, String fieldPath) {
		this(readStateEngine, type, fieldPath, 4);
	}

	public BmPrefixIndex(HollowReadStateEngine readStateEngine, String type, String fieldPath,
			int estimatedMaxStringDuplicates) {
		super(readStateEngine, type, fieldPath, estimatedMaxStringDuplicates);
	}

	@Override
	protected long estimateNumNodes(long totalWords, long averageWordLen) {
		// averageWordLen can be Integer.MAX_VALUE with some hollow snapshots
		long parentEstimate = totalWords * Math.min(1024, averageWordLen);
		if (averageWordLen > 1024) {
			logger.warn("averageWordLen > 1024 ({}). Estimate will be {} instead of {} to prevent huge allocation",
					averageWordLen, 10 * parentEstimate, totalWords * averageWordLen);
		}
		// ProducerTests deleteGroup fails with a too low estimate
		return 10 * parentEstimate;
	}

}
