/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.tag.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.api.TagRef;

public class TagsSanitizer {

	private final BmContext context;
	private static final Logger logger = LoggerFactory.getLogger(TagsSanitizer.class);

	public TagsSanitizer(BmContext context) {
		this.context = context;
	}

	public boolean sanitize(List<TagRef> tags) {
		if (tags == null || tags.isEmpty()) {
			return false;
		}

		Map<String, List<TagRef>> tagsByContainer = tags.stream().collect(Collectors.groupingBy(tr -> tr.containerUid));

		return tagsByContainer.entrySet().stream().map(e -> {
			return sanitize(e.getKey(), e.getValue(), tags);
		}).filter(m -> m).count() > 0;
	}

	private boolean sanitize(String containerUid, List<TagRef> tagRefs, List<TagRef> orig) {
		return tagRefs.stream().map(tr -> {
			ItemValue<Tag> referencedTag = null;
			try {
				referencedTag = context.su().provider().instance(ITags.class, tr.containerUid).getComplete(tr.itemUid);
			} catch (Exception e) {
				logger.debug("Cannot lookup referenced tag {}:{}, {}", tr.containerUid, tr.itemUid, e.getMessage());
			}
			if (referencedTag != null) {
				boolean modified = !referencedTag.value.color.equals(tr.color);
				modified = modified || !referencedTag.value.label.equals(tr.label);
				tr.color = referencedTag.value.color;
				tr.label = referencedTag.value.label;
				return modified;
			} else {
				tr.containerUid = null;
				orig.remove(tr);
				return true;
			}
		}).filter(m -> m).count() > 0;
	}

}
