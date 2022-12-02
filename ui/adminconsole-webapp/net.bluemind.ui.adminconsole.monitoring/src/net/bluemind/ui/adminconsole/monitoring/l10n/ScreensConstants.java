/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.monitoring.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;

public interface ScreensConstants extends ConstantsWithLookup {

	ScreensConstants INST = GWT.create(ScreensConstants.class);

	String date();

	String id();

	String level();

	String product();

	String name();

	String host();

	String datalocation();

	String message();

	String all();

	String warning();

	String critical();

	String filterResolved();

	String days();

	String operation();

	String createdAt();

	String updatedAt();

	String status();

	String executionMode();

	String mandatory();

	String events();

	String running();

	String planned();

	String finished();
}
