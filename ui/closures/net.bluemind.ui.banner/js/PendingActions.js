/**

 * BEGIN LICENSE
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

/**
 * @fileoverview
 * 
 * Bluemind application banner.
 */

goog.provide('net.bluemind.ui.PendingActions');

goog.require('net.bluemind.ui.template');
goog.require('goog.dom');
goog.require('goog.dom.classlist');
goog.require('goog.ui.Component');

/**
 * Simple widget to display pending actions
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.ui.PendingActions = function(url, opt_domHelper) {	
  goog.base(this, opt_domHelper);
  this.url = url;
  this.setModel("~");
};
goog.inherits(net.bluemind.ui.PendingActions, goog.ui.Control);

/** @override */
net.bluemind.ui.PendingActions.prototype.createDom = function() {
  this.element_ = this.dom_.createElement('span');
  var el = this.getElement();
  this.draw();

};

net.bluemind.ui.PendingActions.prototype.draw = function() {
  this.getElement().innerHTML = net.bluemind.ui.template.pendingActions(this.getModel());
}

/** @override */
net.bluemind.ui.PendingActions.prototype.handleMouseUp = function (){
	if (this.url != null){
		var loc = goog.global['location'];
		loc.href = this.url;
	}
}

/** @override */
net.bluemind.ui.PendingActions.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  if (this.isInDocument()) {
    this.draw();
  }
}