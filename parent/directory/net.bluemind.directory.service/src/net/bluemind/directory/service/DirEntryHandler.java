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
package net.bluemind.directory.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirEntry;

public abstract class DirEntryHandler {
	public static byte[] EMPTY_PNG;

	static {
		System.setProperty("java.awt.headless", "true");
		BufferedImage singlePixelImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
		Color transparent = new Color(0, 0, 0, 0);
		singlePixelImage.setRGB(0, 0, transparent.getRGB());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(singlePixelImage, "png", out);
			EMPTY_PNG = out.toByteArray();
		} catch (IOException e) {
			EMPTY_PNG = null;
		}
	}

	public abstract DirEntry.Kind kind();

	public byte[] getIcon(BmContext context, String domainUid, String uid) throws ServerFault {
		return EMPTY_PNG;
	}

	public void create(BmContext context, String domainUid, DirEntry entry) throws ServerFault {
		directory(context, domainUid).create(entry.entryUid, entry);
	}

	public void update(BmContext context, String domainUid, DirEntry entry) throws ServerFault {
		directory(context, domainUid).update(entry.entryUid, entry);
	}

	public void delete(BmContext context, String domainUid, String uid) throws ServerFault {
		directory(context, domainUid).delete(uid);
	}

	public void updateAccountType(BmContext context, String domainUid, String uid, AccountType accountType)
			throws ServerFault {
		directory(context, domainUid).updateAccountType(uid, accountType);
	}

	public abstract TaskRef entryDeleted(BmContext context, String domainUid, String entryUid) throws ServerFault;

	private IInCoreDirectory directory(BmContext context, String domainUid) throws ServerFault {
		return context.su().provider().instance(IInCoreDirectory.class, domainUid);
	}

}
