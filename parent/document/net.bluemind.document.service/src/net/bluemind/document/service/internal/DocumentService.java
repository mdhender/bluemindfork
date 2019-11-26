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
package net.bluemind.document.service.internal;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.document.api.Document;
import net.bluemind.document.api.DocumentMetadata;
import net.bluemind.document.api.IDocument;
import net.bluemind.document.persistence.DocumentMetadataStore;
import net.bluemind.document.service.Activator;
import net.bluemind.document.storage.IDocumentStore;

public class DocumentService implements IDocument {

	private DocumentMetadataStore store;
	private IDocumentStore fsstore;
	private Item item;
	private RBACManager rbacManager;

	public DocumentService(BmContext context, Container container, Item item) {

		store = new DocumentMetadataStore(context.getDataSource());

		fsstore = Activator.getDocumentStore();
		this.item = item;
		rbacManager = RBACManager.forContext(context).forContainer(container);
	}

	@Override
	public void create(String uid, Document doc) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		store.create(item, doc.metadata);
		fsstore.store(uid, doc.content);

	}

	@Override
	public void update(String uid, Document doc) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		store.update(item, doc.metadata);
		fsstore.store(uid, doc.content);

	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		store.delete(uid);
		fsstore.delete(uid);
	}

	@Override
	public Document fetch(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		Document d = null;

		byte[] content = fsstore.get(uid);
		if (content != null) {
			d = new Document();
			d.content = content;
			DocumentMetadata dm = store.get(uid);
			d.metadata = dm;
		}

		return d;
	}

	@Override
	public DocumentMetadata fetchMetadata(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name());

		return store.get(uid);
	}

	@Override
	public List<DocumentMetadata> list() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		return store.getAll(item);
	}

}
