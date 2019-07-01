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
 * @fileoverview pending events notification componnent.
 */

goog.provide('bluemind.calendar.ui.pending.Notification');

goog.require('bluemind.calendar.Controller');
goog.require('bluemind.calendar.model.EventHome');
goog.require('bluemind.calendar.model.EventStorageEventType');
goog.require('bluemind.calendar.pending.template');
goog.require('bluemind.calendar.template.i18n');
goog.require('goog.dom');
goog.require('goog.events.EventType');
goog.require('goog.soy');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.pending.Notification = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(bluemind.calendar.ui.pending.Notification, goog.ui.Component);

/** @inheritDoc */
bluemind.calendar.ui.pending.Notification.prototype.createDom = function() {
  this.element_ = goog.soy.renderAsElement(
    bluemind.calendar.pending.template.notification);
  this.setElementInternal(/** @type {Element} */ (this.element_));
};

/** @inheritDoc */
bluemind.calendar.ui.pending.Notification.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(bluemind.calendar.model.EventHome.getInstance(),
    bluemind.calendar.model.EventStorageEventType.EVENT_UPDATE,
    this.update);

  this.getHandler().listen(this.element_, goog.events.EventType.CLICK,
    bluemind.calendar.Controller.getInstance().showPendingEvents);

};

/**
 * @type {Element} element_ pending dom element.
 * @private
 */
bluemind.calendar.ui.pending.Notification.prototype.element_;

/**
 * Update widget
 */
bluemind.calendar.ui.pending.Notification.prototype.update =
  function() {
  var writable = new Array();
  goog.array.forEach(bluemind.manager.getVisibleCalendars().getKeys(), function(c) {
    if (goog.array.contains(bluemind.writableCalendars, c + '')) {
      writable.push(c);
    }
  });
  bluemind.calendar.model.EventHome.getInstance()
    .countPendingEvents(writable).addCallback(function(nb) {
    this.element_.innerHTML = nb;
    if (nb == 0) {
      goog.dom.classes.remove(this.element_, goog.getCssName('highlight'));
    } else {
      goog.dom.classes.add(this.element_, goog.getCssName('highlight'));
    }
    var data = {nb: nb};
    var windowTitle = goog.soy.renderAsElement(
      bluemind.calendar.template.i18n.appName, data);
    goog.dom.getWindow().document.title = goog.dom.getTextContent(windowTitle);

    var tpContent = goog.soy.renderAsElement(
      bluemind.calendar.template.i18n.pendingEvent, data);
    goog.dom.setProperties(this.element_,
      {'title': goog.dom.getTextContent(tpContent)});

    // quick tb bandal hack
    var bandal = goog.dom.getElement('bandal-tb');
    if (bandal) {
      if (nb == 0) {
        bandal.innerHTML = '';
      } else {
        bandal.innerHTML = goog.dom.getTextContent(tpContent);
        this.getHandler().listen(bandal, goog.events.EventType.CLICK,
          bluemind.calendar.Controller.getInstance().showPendingEvents);
      }
    }
  }, this);
};
