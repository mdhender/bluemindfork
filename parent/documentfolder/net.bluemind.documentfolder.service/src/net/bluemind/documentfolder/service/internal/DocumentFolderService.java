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
package net.bluemind.documentfolder.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.document.api.DocumentMetadata;
import net.bluemind.document.api.IDocument;
import net.bluemind.documentfolder.api.DocumentFolder;
import net.bluemind.documentfolder.api.IDocumentFolder;

public class DocumentFolderService implements IDocumentFolder {

	private ItemStore store;
	private Container container;
	private RBACManager rbacManager;
	private BmContext context;

	public DocumentFolderService(BmContext context, Container container) {
		this.context = context;
		this.container = container;
		store = new ItemStore(context.getDataSource(), container, context.getSecurityContext());
		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	@Override
	public void create(String uid, String name) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		Item item = new Item();
		item.uid = uid;
		item.displayName = name;
		try {
			store.create(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void rename(String uid, String name) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		try {
			store.update(uid, name);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		try {
			Item item = store.get(uid);
			if (item != null) {
				IDocument documentService = context.provider().instance(IDocument.class, container.uid, item.uid);
				List<DocumentMetadata> docs = documentService.list();
				if (docs.size() > 0) {
					// FIXME throw exception or remove docs?

					// throw new ServerFault("Fail to delete folder '"
					// + item.displayName + "' Folder is not empty.");

					for (DocumentMetadata doc : docs) {
						documentService.delete(doc.uid);
					}

				}
				store.delete(item);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public DocumentFolder get(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		DocumentFolder ret = null;
		try {
			Item item = store.get(uid);
			if (item != null) {
				ret = DocumentFolder.create(item.uid, item.displayName);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		return ret;
	}

	@Override
	public ListResult<DocumentFolder> list() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		ListResult<DocumentFolder> ret = new ListResult<>();
		try {
			List<Item> items = store.all();
			ret.total = items.size();
			if (ret.total > 10000) {
				throw ServerFault.tooManyResults("Too many DocumentFolder");
			}
			ret.values = new ArrayList<>((int) ret.total);
			for (Item item : items) {
				ret.values.add(DocumentFolder.create(item.uid, item.displayName));
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		return ret;
	}

}
