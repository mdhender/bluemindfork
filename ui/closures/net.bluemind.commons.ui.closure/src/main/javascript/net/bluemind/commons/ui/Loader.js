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


goog.provide("net.bluemind.commons.ui.Loader");

goog.require("goog.dom");

net.bluemind.commons.ui.Loader = function() {
}

net.bluemind.commons.ui.Loader.prototype.kitt_;

net.bluemind.commons.ui.Loader.prototype.start = function(){
  this.kitt_ = new goog.dom.createElement('div');
  goog.dom.setProperties(this.kitt_, {
    'class' : 'kitt'
  });
  goog.dom.appendChild(this.kitt_, new goog.dom.createElement('div'));
  document.body.insertBefore(this.kitt_, document.body.firstChild);
}


net.bluemind.commons.ui.Loader.prototype.stop = function() {
  goog.dom.removeNode(this.kitt_);
}
