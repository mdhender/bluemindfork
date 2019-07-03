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
package net.bluemind.dav.server.proto.props.webdav;

import java.util.regex.Matcher;

import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.dav.server.proto.IPropertyValue;
import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.props.HrefSet;
import net.bluemind.dav.server.proto.props.IPropertyFactory;
import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.LoggedCore;
import net.bluemind.dav.server.store.ResType;

public class ResourceId extends HrefSet {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ResourceId.class);

	public static final QName NAME = new QName(NS.WEBDAV, "resource-id");

	public ResourceId() {
		super(NAME);
	}

	public static IPropertyFactory factory() {
		return new IPropertyFactory() {
			@Override
			public IPropertyValue create() {
				return new ResourceId();
			}
		};
	}

	@Override
	public void fetch(LoggedCore lc, DavResource dr) {
		String p = dr.getPath();
		ResType rt = dr.getResType();
		switch (dr.getResType()) {
		case PRINCIPAL:
			hrefs = ImmutableList.of("urn:uuid:" + dr.getUid());
			break;
		case VSTUFF_CONTAINER:
			ContainerDescriptor events = lc.vStuffContainer(dr);
			String euid = "urn:uuid:" + events.uid;
			hrefs = ImmutableList.of(euid);
			break;
		case VCARDS_CONTAINER:
			Matcher vcMatcher = rt.matcher(p);
			vcMatcher.find();
			String buid = "urn:uuid:" + vcMatcher.group(2);
			hrefs = ImmutableList.of(buid);
			break;
		default:
			throw new RuntimeException("No unique resource-id for " + p);
		}
	}
}
