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
package net.bluemind.ui.gwtuser.client;

import java.util.Collection;
import java.util.List;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder;

public class ContainerFinder implements IEntityFinder<ContainerDescriptor, ContainerQuery> {

	private String containerType;
	private String domainUid;
	private String userUid;

	public ContainerFinder(String containerType) {
		this.containerType = containerType;
		this.domainUid = Ajax.TOKEN.getContainerUid();
		this.userUid = Ajax.TOKEN.getSubject();
	}

	@Override
	public String getType(ContainerDescriptor result) {
		return null;
	}

	@Override
	public String toString(ContainerDescriptor ci) {
		if (ci.ownerDisplayname != null) {
			return ci.name + " ( " + ci.ownerDisplayname + " )";
		} else {
			return ci.name;
		}
	}

	@Override
	public void find(ContainerQuery cq, final AsyncHandler<ListResult<ContainerDescriptor>> cb) {
		if (Ajax.TOKEN.getContainerUid().equals(domainUid) && Ajax.TOKEN.getSubject().equals(userUid)) {
			new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId()).all(cq, new AsyncHandler<List<ContainerDescriptor>>() {

				@Override
				public void success(List<ContainerDescriptor> value) {

					ListResult<ContainerDescriptor> ret = new ListResult<>();
					ret.total = value.size();
					ret.values = value;
					cb.success(ret);

				}

				@Override
				public void failure(Throwable e) {
					cb.failure(e);
				}
			});
		} else {
			new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId()).allForUser(domainUid, userUid, cq,
					new AsyncHandler<List<ContainerDescriptor>>() {

						@Override
						public void success(List<ContainerDescriptor> value) {

							ListResult<ContainerDescriptor> ret = new ListResult<>();
							ret.total = value.size();
							ret.values = value;
							cb.success(ret);

						}

						@Override
						public void failure(Throwable e) {
							cb.failure(e);
						}
					});
		}
	}

	@Override
	public ContainerQuery queryFromString(String queryString) {
		ContainerQuery fq = new ContainerQuery();
		fq.name = queryString;
		fq.type = containerType;
		return fq;
	}

	@Override
	public void reload(Collection<ContainerDescriptor> ids,
			net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder.ReloadCb<ContainerDescriptor> cb) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDomain(String domainUid) {
		this.domainUid = domainUid;
	}

	public void setUserUid(String userId) {
		this.userUid = userId;
	}

}
