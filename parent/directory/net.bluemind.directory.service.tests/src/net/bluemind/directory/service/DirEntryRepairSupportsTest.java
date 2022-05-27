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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.IDirEntryRepairSupport.InternalMaintenanceOperation;
import net.bluemind.directory.service.internal.DirEntryRepairSupports;

public class DirEntryRepairSupportsTest {
	public static class IMOForTest extends InternalMaintenanceOperation {

		public IMOForTest(String beforeOp, String afterOp, int cost) {
			super(UUID.randomUUID().toString(), beforeOp, afterOp, cost);
		}

		@Override
		public void check(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.end();
		}

		@Override
		public void repair(String domainUid, DirEntry entry, RepairTaskMonitor monitor) {
			monitor.end();
		}
	}

	@Test
	public void testOrder() {
		InternalMaintenanceOperation d = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation c = new IMOForTest(d.identifier, null, 0);
		InternalMaintenanceOperation b = new IMOForTest(c.identifier, null, 0);
		InternalMaintenanceOperation a = new IMOForTest(b.identifier, null, 0);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(d, b, c, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			toSort = DirEntryRepairSupports.order(toSort);
			assertEquals(4, toSort.size());
			assertEquals(a.identifier, toSort.get(0).identifier);
			assertEquals(b.identifier, toSort.get(1).identifier);
			assertEquals(c.identifier, toSort.get(2).identifier);
			assertEquals(d.identifier, toSort.get(3).identifier);
		}

		d = new IMOForTest(null, null, 0);
		c = new IMOForTest(null, null, 0);
		b = new IMOForTest(c.identifier, null, 0);
		a = new IMOForTest(d.identifier, null, 0);

		toSort = Arrays.asList(d, b, c, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// a must be before d
			// b must be before c
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(4, toSort.size());
			boolean aFound = false;
			boolean bFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(b.identifier)) {
					bFound = true;
				} else if (op.identifier.equals(c.identifier) && !bFound) {
					fail("b operation must be before c operation");
				} else if (op.identifier.equals(d.identifier) && !aFound) {
					fail("a operation must be before d operation");
				}
			}
		}
	}

	@Test
	public void testOrder_beforeAfter() {
		InternalMaintenanceOperation b = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation d = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation c = new IMOForTest(d.identifier, b.identifier, 0);
		InternalMaintenanceOperation a = new IMOForTest(b.identifier, null, 0);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(d, b, c, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			toSort = DirEntryRepairSupports.order(toSort);
			assertEquals(4, toSort.size());
			assertEquals(a.identifier, toSort.get(0).identifier);
			assertEquals(b.identifier, toSort.get(1).identifier);
			assertEquals(c.identifier, toSort.get(2).identifier);
			assertEquals(d.identifier, toSort.get(3).identifier);
		}

		d = new IMOForTest(null, null, 0);
		c = new IMOForTest(null, d.identifier, 0);
		b = new IMOForTest(c.identifier, null, 0);
		a = new IMOForTest(d.identifier, null, 0);

		toSort = Arrays.asList(d, b, c, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// a must be before d
			// d must be before c
			// b must be before c
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(4, toSort.size());
			boolean aFound = false;
			boolean bFound = false;
			boolean cFound = false;
			boolean dFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(b.identifier)) {
					bFound = true;
				} else if (op.identifier.equals(c.identifier)) {
					cFound = true;
					if (!bFound) {
						fail("b operation must be before c operation");
					}
				} else if (op.identifier.equals(d.identifier)) {
					dFound = true;
					if (!aFound) {
						fail("a operation must be before d operation");
					} else if (cFound) {
						fail("b operation must be before c operation");
					}
				}
			}

			assertTrue(aFound && bFound && cFound && dFound);
		}
	}

	@Test
	public void testOrder_NotAllOps_beforeAfter() {
		InternalMaintenanceOperation b = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation d = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation c = new IMOForTest(d.identifier, b.identifier, 0);
		InternalMaintenanceOperation a = new IMOForTest(b.identifier, null, 0);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(d, c, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// c must be before d
			// Can be a,c,d or c,d,a or c,a,d
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(3, toSort.size());
			boolean aFound = false;
			boolean cFound = false;
			boolean dFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(c.identifier)) {
					cFound = true;
				} else if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(d.identifier)) {
					dFound = true;
					if (!cFound) {
						fail("c operation must be before d operation");
					}
				}
			}

			assertTrue(aFound && cFound && dFound);
		}

		toSort = Arrays.asList(d, b, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// a must be before b
			// Can be a,b,d or d,a,b or a,d,b
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(3, toSort.size());
			boolean aFound = false;
			boolean bFound = false;
			boolean dFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(b.identifier)) {
					bFound = true;
					if (!aFound) {
						fail("a operation must be before b operation");
					}
				} else if (op.identifier.equals(d.identifier)) {
					dFound = true;
				}
			}

			assertTrue(aFound && bFound && dFound);
		}

		toSort = Arrays.asList(c, a, b);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			toSort = DirEntryRepairSupports.order(toSort);
			assertEquals(a.identifier, toSort.get(0).identifier);
			assertEquals(b.identifier, toSort.get(1).identifier);
			assertEquals(c.identifier, toSort.get(2).identifier);
		}

		toSort = Arrays.asList(a, d);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			toSort = DirEntryRepairSupports.order(toSort);
			assertEquals(2, toSort.size());
		}
	}

	@Test
	public void testOrder_NotAllOps_beforeOnly() {
		InternalMaintenanceOperation d = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation c = new IMOForTest(d.identifier, null, 0);
		InternalMaintenanceOperation b = new IMOForTest(c.identifier, null, 0);
		InternalMaintenanceOperation a = new IMOForTest(b.identifier, null, 1000);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(d, c, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// c must be before d
			// Can be a,c,d or c,d,a or c,a,d
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(3, toSort.size());
			boolean aFound = false;
			boolean cFound = false;
			boolean dFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(c.identifier)) {
					cFound = true;
				} else if (op.identifier.equals(d.identifier)) {
					dFound = true;
					if (!cFound) {
						fail("c operation must be before d operation");
					}
				}
			}

			assertTrue(aFound && cFound && dFound);
		}

		toSort = Arrays.asList(d, b, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// a must be before b
			// Can be a,b,d or d,a,b or a,d,b
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(3, toSort.size());
			boolean aFound = false;
			boolean bFound = false;
			boolean dFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(b.identifier)) {
					bFound = true;
					if (!aFound) {
						fail("a operation must be before b operation");
					}
				} else if (op.identifier.equals(d.identifier)) {
					dFound = true;
				}
			}

			assertTrue(aFound && bFound && dFound);
		}

		toSort = Arrays.asList(b, c, d);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			toSort = DirEntryRepairSupports.order(toSort);
			assertEquals(3, toSort.size());
			assertEquals(b.identifier, toSort.get(0).identifier);
			assertEquals(c.identifier, toSort.get(1).identifier);
			assertEquals(d.identifier, toSort.get(2).identifier);
		}

		toSort = Arrays.asList(c, a, b);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			toSort = DirEntryRepairSupports.order(toSort);
			assertEquals(3, toSort.size());
			assertEquals(a.identifier, toSort.get(0).identifier);
			assertEquals(b.identifier, toSort.get(1).identifier);
			assertEquals(c.identifier, toSort.get(2).identifier);
		}
	}

	@Test
	public void testOrder_sameBefore_samePrio() {
		InternalMaintenanceOperation c = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation a = new IMOForTest(c.identifier, null, 0);
		InternalMaintenanceOperation b = new IMOForTest(c.identifier, null, 0);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(c, b, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// c must be after a and b
			// Can be a,b,c or b,a,c
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(3, toSort.size());
			boolean aFound = false;
			boolean bFound = false;
			boolean cFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
				} else if (op.identifier.equals(b.identifier)) {
					bFound = true;
				} else if (op.identifier.equals(c.identifier)) {
					cFound = true;
					if (!aFound || !bFound) {
						fail("c operation must be after a and b operations");
					}
				}
			}

			assertTrue(aFound && bFound && cFound);
		}
	}

	@Test
	public void testOrder_sameAfter_samePrio() {
		InternalMaintenanceOperation a = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation b = new IMOForTest(null, a.identifier, 0);
		InternalMaintenanceOperation c = new IMOForTest(null, a.identifier, 0);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(c, b, a);
		for (int i = 0; i < 100; ++i) {
			Collections.shuffle(toSort);
			// a must be after b and c
			// Can be a,b,c or a,c,b
			toSort = DirEntryRepairSupports.order(toSort);

			assertEquals(3, toSort.size());
			boolean aFound = false;
			boolean bFound = false;
			boolean cFound = false;
			for (InternalMaintenanceOperation op : toSort) {
				if (op.identifier.equals(a.identifier)) {
					aFound = true;
					if (bFound || cFound) {
						fail("a operation must be before b and c operations");
					}
				} else if (op.identifier.equals(b.identifier)) {
					bFound = true;
				} else if (op.identifier.equals(c.identifier)) {
					cFound = true;
				}
			}

			assertTrue(aFound && bFound && cFound);
		}
	}

	@Test
	public void testOrderCyclicDeps() {
		InternalMaintenanceOperation a = new IMOForTest(null, null, 0);
		InternalMaintenanceOperation b = new IMOForTest(null, a.identifier, 0);
		InternalMaintenanceOperation d = new IMOForTest(a.identifier, null, 0);
		InternalMaintenanceOperation c = new IMOForTest(d.identifier, b.identifier, 0);

		List<InternalMaintenanceOperation> toSort = Arrays.asList(a, b, c, d);
		// after resolution
		// " a -> b -> c -> d -> a"

		try {
			DirEntryRepairSupports.order(toSort);
			fail("should detect circular dependency");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}
	}

}
