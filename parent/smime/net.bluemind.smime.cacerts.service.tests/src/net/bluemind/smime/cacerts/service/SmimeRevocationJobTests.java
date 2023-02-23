/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.scheduledjob.api.IJob;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeRevocation;

public class SmimeRevocationJobTests extends AbstractServiceTests {

	private String smimeRevocationJob = new SmimeRevocationJob().getJobId();

	private ItemValue<SmimeCacert> createCacert(String uidpart) throws Exception {
		SmimeCacert cert = defaultSmimeCacert("data/trust-ca.crt.cer");
		String uid = uidpart + System.nanoTime();

		ISmimeCACert serviceCert = getServiceCacert(defaultSecurityContext, container.uid);
		serviceCert.create(uid, cert);

		ItemValue<SmimeCacert> smimeCert = serviceCert.getComplete(uid);
		assertNotNull(smimeCert);
		assertNotNull(smimeCert.item());

		return smimeCert;
	}

	private SmimeRevocation createRevocation(String serialNumber, Date revocationDate, String revocationReason,
			String url, Date lastUpdate, Date nextUpdate, ItemValue<SmimeCacert> cacert) {
		SmimeRevocation revocation = SmimeRevocation.create(serialNumber, revocationDate, revocationReason, url,
				lastUpdate, nextUpdate, "issuer_" + cacert.uid, cacert.uid);
		IInCoreSmimeRevocation incoreService = getIncoreService(defaultSecurityContext, domainUid);
		incoreService.create(revocation, cacert);
		return incoreService.get(serialNumber, cacert);
	}

	@Test
	public void testSmimeRevocationJob() throws Exception {
		LocalDate localDate = LocalDate.now();
		Date revokedDate = Date.from(localDate.minusDays(10).atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date lastUpdate = Date.from(localDate.minusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
		Date nextUpdate = Date.from(localDate.minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant());

		ItemValue<SmimeCacert> cacert = createCacert("uid1");
		assertNotNull(cacert);

		SmimeRevocation r1 = createRevocation("A1", revokedDate, "you're revoked", "crls_location", lastUpdate,
				nextUpdate, cacert);
		assertNotNull(r1);
		SmimeRevocation r2 = createRevocation("A2", revokedDate, "you're revoked", "crls_location2", lastUpdate,
				nextUpdate, cacert);
		assertNotNull(r2);

		IJob serviceAdmin0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IJob.class);

		try {
			JobExecutionQuery query = new JobExecutionQuery();
			serviceAdmin0.start(smimeRevocationJob, domainUid);
			waitFor(serviceAdmin0, smimeRevocationJob);

			query.jobId = smimeRevocationJob;
			ListResult<JobExecution> searchExecution = serviceAdmin0.searchExecution(query);
			assertEquals(1L, searchExecution.total);
			assertEquals(JobExitStatus.SUCCESS, searchExecution.values.get(0).status);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	private void waitFor(IJob serviceAdmin0, String jobId) throws ServerFault {
		JobExecutionQuery query = new JobExecutionQuery();
		query.active = true;
		query.jobId = jobId;
		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} while (!serviceAdmin0.searchExecution(query).values.isEmpty());
	}

	@Override
	protected ISmimeRevocation getServiceRevocation(SecurityContext context, String domainUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeRevocation.class, domainUid);
	}

	@Override
	protected ISmimeCACert getServiceCacert(SecurityContext context, String containerUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(ISmimeCACert.class, containerUid);
	}

	private IInCoreSmimeRevocation getIncoreService(SecurityContext context, String domainUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IInCoreSmimeRevocation.class, domainUid);
	}
}
