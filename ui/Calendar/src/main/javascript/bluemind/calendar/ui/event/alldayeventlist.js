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
 * @fileoverview Allday event list bubble componnent.
 */

goog.provide('bluemind.calendar.ui.event.AllDayEventList');
goog.require('bluemind.calendar.ui.event.Bubble');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.AllDayEventList = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(bluemind.calendar.ui.event.AllDayEventList,
  bluemind.calendar.ui.event.Bubble);

/** @inheritDoc */
bluemind.calendar.ui.event.AllDayEventList.prototype.createDom = function() {
  var element = goog.soy.renderAsElement(
    bluemind.calendar.event.template.bubble);
  element.style.position = 'absolute';
  element.style.display = 'none';
  this.setElementInternal(/** @type {Element} */ (element));
  this.popup_.setElement(element);
};

/** @inheritDoc */
bluemind.calendar.ui.event.AllDayEventList.prototype.setModel = function(obj) {
  goog.base(this, 'setModel', obj);
};

/** @inheritDoc */
bluemind.calendar.ui.event.AllDayEventList.prototype.setVisible = function(visible) {
  if (visible) {
    var eh = bluemind.calendar.model.EventHome.getInstance();
    var id = this.getModel().id.split('_');
    var start = bluemind.view.getView().getStart().clone();
    start.add(new goog.date.Interval(goog.date.Interval.DAYS, parseInt(id[1])));
    var end = start.clone();
    end.add(new goog.date.Interval(goog.date.Interval.DAYS, 1));
    var calendars = bluemind.manager.visibleCalendars_.getKeys();
    var v = bluemind.view.getView().getName();
    var container = this.getContentElement();
    goog.dom.removeChildren(container);
    goog.dom.setTextContent(
      this.getElementByClass(goog.getCssName('eb-title')),
      this.dateTimeHelper_.formatDate(start));

    eh.getEventsByPeriod(start, end, calendars).addCallback(function(evts) {

      evts.sort(function(evt1, evt2) {
        var d1 = evt1.getDate().clone();
        var d2 = evt2.getDate().clone();
        if (evt1.isAllday() || evt1.alldaySize > 1) {
          d1.setHours(0);
        }
        if (evt2.isAllday() || evt2.alldaySize > 1) {
          d2.setHours(0);
        }
        var diff = d1.getTime() - d2.getTime();
        if (diff != 0) return diff;
        var diff = evt2.getDuration() - evt1.getDuration();
        if (diff != 0) return diff;
        var diff = evt1.getKlass() - evt2.getKlass();
        if (diff != 0) return diff;
        return goog.getUid(evt1) - goog.getUid(evt2);
      });

      goog.array.forEach(evts, function(e) {
        if (bluemind.manager.isEventVisible(e)) {
          if (v == 'month' || v == 'days' && e.isAllday()) {
            e.createAlldayPopup(start, end)
            goog.dom.appendChild(container, e.getElement());
          }
        }
      });
      this.popup_.setVisible(true);
      this.setListeners();
    }, this);
  } else {
    this.popup_.setVisible(false);
    this.unsetListeners_();
  }
};

