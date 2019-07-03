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
goog.provide('net.bluemind.chooser.fileexplorer.FileExplorerView');

goog.require('goog.ui.Container');
goog.require('net.bluemind.filehosting.ui.FileHostingExplorer');



/**
 * @constructor
 *
 * @extends {net.bluemind.filehosting.ui.FileHostingExplorer}
 */
net.bluemind.chooser.fileexplorer.FileExplorerView = function() {
  net.bluemind.filehosting.ui.FileHostingExplorer.call(this);
};
goog.inherits(net.bluemind.chooser.fileexplorer.FileExplorerView, net.bluemind.filehosting.ui.FileHostingExplorer);


/** @override */
net.bluemind.chooser.fileexplorer.FileExplorerView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var sibbling = this.getDomHelper().getPreviousElementSibling(this.getElement());
  this.getElement().style.top = (sibbling.offsetTop + sibbling.offsetHeight) + 'px';
};
