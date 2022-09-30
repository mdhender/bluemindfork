/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package net.bluemind.utils;

public enum ByteSizeUnit {
	BYTES {
		@Override
		public long toBytes(long size) {
			return size;
		}

		@Override
		public long toKB(long size) {
			return size / (C1 / C0);
		}

		@Override
		public long toMB(long size) {
			return size / (C2 / C0);
		}

		@Override
		public long toGB(long size) {
			return size / (C3 / C0);
		}

		@Override
		public long toTB(long size) {
			return size / (C4 / C0);
		}

		@Override
		public long toPB(long size) {
			return size / (C5 / C0);
		}

		@Override
		public String getSuffix() {
			return "b";
		}

		@Override
		public long fromBytes(long size) {
			return size;
		}

		@Override
		public long fromKB(long size) {
			return x(size, C1 / C0, MAX / (C1 / C0));
		}

		@Override
		public long fromMB(long size) {
			return x(size, C2 / C0, MAX / (C2 / C0));
		}

		@Override
		public long fromGB(long size) {
			return x(size, C3 / C0, MAX / (C3 / C0));
		}

		@Override
		public long fromTB(long size) {
			return x(size, C4 / C0, MAX / (C4 / C0));
		}

		@Override
		public long fromPB(long size) {
			return x(size, C5 / C0, MAX / (C5 / C0));
		}
	},
	KB {
		@Override
		public long toBytes(long size) {
			return x(size, C1 / C0, MAX / (C1 / C0));
		}

		@Override
		public long toKB(long size) {
			return size;
		}

		@Override
		public long toMB(long size) {
			return size / (C2 / C1);
		}

		@Override
		public long toGB(long size) {
			return size / (C3 / C1);
		}

		@Override
		public long toTB(long size) {
			return size / (C4 / C1);
		}

		@Override
		public long toPB(long size) {
			return size / (C5 / C1);
		}

		@Override
		public String getSuffix() {
			return "kb";
		}

		@Override
		public long fromBytes(long size) {
			return size / (C1 / C0);
		}

		@Override
		public long fromKB(long size) {
			return size;
		}

		@Override
		public long fromMB(long size) {
			return x(size, C2 / C1, MAX / (C2 / C1));
		}

		@Override
		public long fromGB(long size) {
			return x(size, C3 / C1, MAX / (C3 / C1));
		}

		@Override
		public long fromTB(long size) {
			return x(size, C4 / C1, MAX / (C4 / C1));
		}

		@Override
		public long fromPB(long size) {
			return x(size, C5 / C1, MAX / (C5 / C1));
		}
	},
	MB {
		@Override
		public long toBytes(long size) {
			return x(size, C2 / C0, MAX / (C2 / C0));
		}

		@Override
		public long toKB(long size) {
			return x(size, C2 / C1, MAX / (C2 / C1));
		}

		@Override
		public long toMB(long size) {
			return size;
		}

		@Override
		public long toGB(long size) {
			return size / (C3 / C2);
		}

		@Override
		public long toTB(long size) {
			return size / (C4 / C2);
		}

		@Override
		public long toPB(long size) {
			return size / (C5 / C2);
		}

		@Override
		public String getSuffix() {
			return "mb";
		}

		@Override
		public long fromBytes(long size) {
			return size / (C2 / C0);
		}

		@Override
		public long fromKB(long size) {
			return size / (C1 / C0);
		}

		@Override
		public long fromMB(long size) {
			return size;
		}

		@Override
		public long fromGB(long size) {
			return x(size, C3 / C2, MAX / (C3 / C2));
		}

		@Override
		public long fromTB(long size) {
			return x(size, C4 / C2, MAX / (C4 / C2));
		}

		@Override
		public long fromPB(long size) {
			return x(size, C5 / C2, MAX / (C5 / C2));
		}
	},
	GB {
		@Override
		public long toBytes(long size) {
			return x(size, C3 / C0, MAX / (C3 / C0));
		}

		@Override
		public long toKB(long size) {
			return x(size, C3 / C1, MAX / (C3 / C1));
		}

		@Override
		public long toMB(long size) {
			return x(size, C3 / C2, MAX / (C3 / C2));
		}

		@Override
		public long toGB(long size) {
			return size;
		}

		@Override
		public long toTB(long size) {
			return size / (C4 / C3);
		}

		@Override
		public long toPB(long size) {
			return size / (C5 / C3);
		}

		@Override
		public String getSuffix() {
			return "gb";
		}

		@Override
		public long fromBytes(long size) {
			return size / (C3 / C0);
		}

		@Override
		public long fromKB(long size) {
			return size / (C2 / C0);
		}

		@Override
		public long fromMB(long size) {
			return size / (C1 / C0);
		}

		@Override
		public long fromGB(long size) {
			return size;
		}

		@Override
		public long fromTB(long size) {
			return x(size, C3 / C2, MAX / (C3 / C2));
		}

		@Override
		public long fromPB(long size) {
			return x(size, C4 / C2, MAX / (C4 / C2));
		}
	},
	TB {
		@Override
		public long toBytes(long size) {
			return x(size, C4 / C0, MAX / (C4 / C0));
		}

		@Override
		public long toKB(long size) {
			return x(size, C4 / C1, MAX / (C4 / C1));
		}

		@Override
		public long toMB(long size) {
			return x(size, C4 / C2, MAX / (C4 / C2));
		}

		@Override
		public long toGB(long size) {
			return x(size, C4 / C3, MAX / (C4 / C3));
		}

		@Override
		public long toTB(long size) {
			return size;
		}

		@Override
		public long toPB(long size) {
			return size / (C5 / C4);
		}

		@Override
		public String getSuffix() {
			return "tb";
		}

		@Override
		public long fromBytes(long size) {
			return size / (C4 / C0);
		}

		@Override
		public long fromKB(long size) {
			return size / (C3 / C0);
		}

		@Override
		public long fromMB(long size) {
			return size / (C2 / C0);
		}

		@Override
		public long fromGB(long size) {
			return size / (C1 / C0);
		}

		@Override
		public long fromTB(long size) {
			return size;
		}

		@Override
		public long fromPB(long size) {
			return x(size, C4 / C3, MAX / (C4 / C3));
		}
	},
	PB {
		@Override
		public long toBytes(long size) {
			return x(size, C5 / C0, MAX / (C5 / C0));
		}

		@Override
		public long toKB(long size) {
			return x(size, C5 / C1, MAX / (C5 / C1));
		}

		@Override
		public long toMB(long size) {
			return x(size, C5 / C2, MAX / (C5 / C2));
		}

		@Override
		public long toGB(long size) {
			return x(size, C5 / C3, MAX / (C5 / C3));
		}

		@Override
		public long toTB(long size) {
			return x(size, C5 / C4, MAX / (C5 / C4));
		}

		@Override
		public long toPB(long size) {
			return size;
		}

		@Override
		public String getSuffix() {
			return "pb";
		}

		@Override
		public long fromBytes(long size) {
			return size / (C5 / C0);
		}

		@Override
		public long fromKB(long size) {
			return size / (C4 / C0);
		}

		@Override
		public long fromMB(long size) {
			return size / (C3 / C0);
		}

		@Override
		public long fromGB(long size) {
			return size / (C2 / C0);
		}

		@Override
		public long fromTB(long size) {
			return size / (C1 / C0);
		}

		@Override
		public long fromPB(long size) {
			return size;
		}
	};

	static final long C0 = 1L;
	static final long C1 = C0 * 1024L;
	static final long C2 = C1 * 1024L;
	static final long C3 = C2 * 1024L;
	static final long C4 = C3 * 1024L;
	static final long C5 = C4 * 1024L;

	static final long MAX = Long.MAX_VALUE;

	/**
	 * Scale d by m, checking for overflow. This has a short name to make above code
	 * more readable.
	 */
	static long x(long d, long m, long over) {
		if (d > over)
			return Long.MAX_VALUE;
		if (d < -over)
			return Long.MIN_VALUE;
		return d * m;
	}

	public abstract long toBytes(long size);

	public abstract long toKB(long size);

	public abstract long toMB(long size);

	public abstract long toGB(long size);

	public abstract long toTB(long size);

	public abstract long toPB(long size);

	public abstract long fromBytes(long size);

	public abstract long fromKB(long size);

	public abstract long fromMB(long size);

	public abstract long fromGB(long size);

	public abstract long fromTB(long size);

	public abstract long fromPB(long size);

	public abstract String getSuffix();

}
