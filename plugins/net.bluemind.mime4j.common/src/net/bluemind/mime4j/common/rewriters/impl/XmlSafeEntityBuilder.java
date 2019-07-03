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
/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package net.bluemind.mime4j.common.rewriters.impl;

import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.message.BodyFactory;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.mime4j.common.DefaultEntityBuilder;

/**
 * A <code>ContentHandler</code> that rewrites parts encoding to quoted
 * printable (texts) & base64 (binary bodies) so the message is safe to include
 * in XML.
 */
public class XmlSafeEntityBuilder extends DefaultEntityBuilder {

	private static final Logger logger = LoggerFactory.getLogger(XmlSafeEntityBuilder.class);

	public XmlSafeEntityBuilder(Entity entity, BodyFactory bodyFactory) {
		super(entity, bodyFactory);
		logger.debug("created.");
	}

	@Override
	public void postBody(Entity entity, BodyDescriptor bd, Body b) {

		// BM change: change content encodings
		Header header = entity.getHeader();
		if (header != null) {
			if (bd.getMimeType().startsWith("text/")) {
				header.setField(Fields.contentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE));
			} else {
				header.setField(Fields.contentTransferEncoding(MimeUtil.ENC_BASE64));
			}
		}
	}
}
