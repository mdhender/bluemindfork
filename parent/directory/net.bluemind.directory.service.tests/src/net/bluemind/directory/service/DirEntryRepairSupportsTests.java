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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.directory.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.directory.service.internal.DirEntryRepairSupports;

public class DirEntryRepairSupportsTests {

	public static final String TEST_USER_OP = "testUserOp";

	public static class FakeTestRepairSupport implements IDirEntryRepairSupport {

		public static class FakeFactory implements IDirEntryRepairSupport.Factory {

			@Override
			public IDirEntryRepairSupport create(BmContext context) {
				return new FakeTestRepairSupport(context);
			}

		}

		@SuppressWarnings("unused")
		private BmContext context;

		public FakeTestRepairSupport(BmContext context) {
			this.context = context;
		}

		@Override
		public Set<MaintenanceOperation> availableOperations(Kind kind) {
			switch (kind) {
			case USER:
				MaintenanceOperation op = new MaintenanceOperation();
				op.identifier = TEST_USER_OP;
				op.description = "gg";
				return ImmutableSet.of(op);
			default:
				return ImmutableSet.of();
			}

		}

		@Override
		public Set<InternalMaintenanceOperation> ops(Kind kind) {
			switch (kind) {
			case USER:
				InternalMaintenanceOperation op = new InternalMaintenanceOperation(TEST_USER_OP, null, null, 1) {

					@Override
					public void check(String domainUid, DirEntry entry, DiagnosticReport report,
							IServerTaskMonitor monitor) {

					}

					@Override
					public void repair(String domainUid, DirEntry entry, DiagnosticReport report,
							IServerTaskMonitor monitor) {

					}

				};
				return ImmutableSet.of(op);
			default:
				return ImmutableSet.of();
			}

		}

	}

	private BmTestContext testContext;

	@Test
	public void testAvailableOperations() {
		DirEntryRepairSupports drs = new DirEntryRepairSupports(testContext);
		Set<MaintenanceOperation> ops = drs.availableOperations(Kind.USER);
		assertTrue(ops.stream().anyMatch(op -> op.identifier.equals(TEST_USER_OP)));

		ops = drs.availableOperations(Kind.GROUP);
		assertFalse(ops.stream().anyMatch(op -> op.identifier.equals(TEST_USER_OP)));
	}

	@Test
	public void testOps() {
		DirEntryRepairSupports drs = new DirEntryRepairSupports(testContext);

		// empty filter
		List<InternalMaintenanceOperation> ops = drs.ops(ImmutableSet.of(), Kind.USER);
		assertTrue(ops.stream().anyMatch(op -> op.identifier.equals(TEST_USER_OP)));

		// null filter
		ops = drs.ops(null, Kind.USER);
		assertTrue(ops.stream().anyMatch(op -> op.identifier.equals(TEST_USER_OP)));

		// TEST_USER_OP not in filterIn set
		ops = drs.ops(ImmutableSet.of("zz"), Kind.USER);
		assertFalse(ops.stream().anyMatch(op -> op.identifier.equals(TEST_USER_OP)));
	}

	@Before
	public void before() throws Exception {
		this.testContext = new BmTestContext(SecurityContext.SYSTEM);
	}
}
