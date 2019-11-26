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
package net.bluemind.addressbook.persistence;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Parameter;

public class ParametersColumns {

	protected static List<Parameter> stringAsParameters(String stringParameters) {
		if (stringParameters != null) {
			String[] entries = stringParameters.split(";");
			Parameter[] params = new Parameter[entries.length];
			for (int i = 0; i < entries.length; i++) {
				String p = entries[i];
				String[] v = p.split("=");
				if (v.length == 2) {
					params[i] = Parameter.create(v[0], v[1]);
				}

				if (v.length == 1) {
					params[i] = Parameter.create(v[0], "true");
				}
			}

			return Arrays.asList(params);
		} else {
			return Collections.emptyList();
		}
	}

	protected static List<String> asStrings(Array array) throws SQLException {
		String[] values = (String[]) array.getArray();

		return Arrays.asList(values);
	}

	/*
	 * protected static Parameters asParameters(Array array) throws SQLException
	 * {
	 * 
	 * String[] values = (String[]) array.getArray(); Parameter[] params = new
	 * Parameter[values.length]; int i = 0; for (String v : values) { if (v !=
	 * null) { String[] vv = v.split("="); params[i] = Parameter.create(vv[0],
	 * vv[1]); } i++; }
	 * 
	 * return Parameters.create(params); }
	 */
	protected static String parametersAsString(List<VCard.Parameter> parameters) {
		if (parameters == null || parameters.size() == 0) {
			return null;
		}

		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < parameters.size(); i++) {
			Parameter p = parameters.get(i);
			if (i != 0) {
				ret.append(";");
			}
			ret.append(p.label + "=" + p.value);
		}
		return ret.toString();

	}
}
