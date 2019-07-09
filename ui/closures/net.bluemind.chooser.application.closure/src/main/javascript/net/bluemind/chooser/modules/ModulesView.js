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
goog.provide('net.bluemind.chooser.modules.ModulesView');

goog.require('goog.ui.TabBar');
goog.require('goog.ui.TabBar.Location');



/**
 * @constructor git a
 * @extends {goog.ui.TabBar}
 */
net.bluemind.chooser.modules.ModulesView = function() {
  goog.ui.TabBar.call(this, goog.ui.TabBar.Location.TOP);

};
goog.inherits(net.bluemind.chooser.modules.ModulesView, goog.ui.TabBar);
