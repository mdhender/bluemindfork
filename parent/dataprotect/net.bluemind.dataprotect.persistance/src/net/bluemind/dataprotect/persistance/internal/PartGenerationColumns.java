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
package net.bluemind.dataprotect.persistance.internal;

import net.bluemind.core.jdbc.Columns;

public class PartGenerationColumns {
	public static Columns COLUMNS = Columns.create() //
			.col("id") //
			.col("backup_id") //
			.col("starttime") //
			.col("endtime") //
			.col("size_mb") //
			.col("valid", "t_generation_status")//
			.col("tag")//
			.col("server_adr")//
			.col("datatype");

}
