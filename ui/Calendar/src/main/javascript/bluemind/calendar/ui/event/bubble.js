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
 * @fileoverview Bubble graphic componnent.
 */


goog.provide('bluemind.calendar.ui.event.Bubble');
goog.require('bluemind.calendar.event.template');
goog.require('goog.date');
goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.positioning');
goog.require('goog.positioning.AbsolutePosition');
goog.require('goog.positioning.AnchoredPosition');
goog.require('goog.positioning.Corner');
goog.require('goog.soy');
goog.require('goog.style');
goog.require('goog.ui.Component');
goog.require('goog.ui.Popup');
goog.require('goog.ui.PopupBase.EventType');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.Bubble = function(opt_domHelper) {
  goog.base(this, opt_domHelper);

  this.popup_ = new goog.ui.Popup();
  this.popup_.setHideOnEscape(true);
  this.popup_.setAutoHide(true);

  this.dateTimeHelper_ = bluemind.i18n.DateTimeHelper.getInstance();

};
goog.inherits(bluemind.calendar.ui.event.Bubble,
  goog.ui.Component);

/**
 * The Popup element used to position and display the bubble.
 *
 * @type {goog.ui.Popup}
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.popup_;

/**
 * date time helper
 * @type {bluemind.i18n.DateTimeHelper}
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.dateTimeHelper_;

/** @inheritDoc */
bluemind.calendar.ui.event.Bubble.prototype.createDom = function() {
  var element = goog.soy.renderAsElement(
    bluemind.calendar.event.template.bubble);
  element.style.position = 'absolute';
  element.style.visibility = 'hidden';
  this.setElementInternal(/** @type {Element} */ (element));
  this.popup_.setElement(element);
};

/** @inheritDoc */
bluemind.calendar.ui.event.Bubble.prototype.setModel = function(obj) {
  var model = this.getModel();
  if (model != null) {
    this.setVisible(false);
  }
  goog.base(this, 'setModel', obj);
};

/**
 * Unset all listener
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.unsetListeners_ = function() {
  this.getHandler().removeAll();
};


/**
 * Set  listener
 */
bluemind.calendar.ui.event.Bubble.prototype.setListeners = function() {
  this.getHandler().listen(this.popup_, goog.ui.PopupBase.EventType.HIDE,
    this.hide, false, this);

  this.getHandler().listen(this.getElementByClass(goog.getCssName('eb-close')),
    goog.events.EventType.CLICK, this.hide, false, this);

  this.getHandler().listen(goog.dom.getElement('gridContainer'),
    goog.events.EventType.SCROLL, this.hide, false, this);

  // There's a bug here.
  // this.getHandler().listen(goog.dom.getWindow(),
  //   goog.events.EventType.RESIZE, this.hide, false, this);

  this.setModelListeners();
};

/**
 * Set all listener that affect or are affected by the model
 * Should be overriden
 * @protected
 */
bluemind.calendar.ui.event.Bubble.prototype.setModelListeners = function() {
};

/**
 * Attaches the bubble to an anchor element. Computes the positioning and
 * orientation of the bubble.
 * FIXME : Should be replaced by a AnchorViewportPosition, but the
 *  AnchorViewportPositon does not tell the coputed corner where the
 *  popup will be placed. So it's not possible to correctly set the arrow.
 * @param {Element} anchor The element to which we are attaching.
 */
bluemind.calendar.ui.event.Bubble.prototype.attach = function(anchor) {
  var corner = this.computeCorner_(anchor);
  var margin = this.computeMargin_(corner, anchor);
  var position = this.computePosition_(corner, anchor);
  this.popup_.setMargin(margin);
  this.popup_.setPinnedCorner(corner);
  this.setArrowPosition_(corner, anchor);
  this.popup_.setPosition(position);
};

/**
 * Turn the bubble visibility on or off.
 *
 * @param {boolean} visible Desired visibility state.
 */
bluemind.calendar.ui.event.Bubble.prototype.setVisible = function(visible) {
  if (visible && this.getModel() == null) {
     throw Error('You must set the model object before showing the bubble');
  }
  if (visible && !this.popup_.isVisible()) {
    this.drawElement_();
    this.setListeners();
  }
  this.popup_.setVisible(visible);
  if (!this.popup_.isVisible()) {
    this.eraseElement_();
    this.unsetListeners_();
  }
};

/**
 * Alias to setVisible(false)
 */
bluemind.calendar.ui.event.Bubble.prototype.hide = function() {
  this.setVisible(false);
};

/**
 * Creates element's contents. This is called on display.
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.drawElement_ = function() {
  if (!this.isInDocument()) {
    throw Error('You must render the bubble before showing it!');
  }

  goog.dom.setTextContent(this.getElementByClass(goog.getCssName('eb-title')),
    this.buildTitle());

  goog.dom.appendChild(this.getElementByClass(goog.getCssName('eb-content')),
    this.buildContent());
};

/**
 * Gets rid of the element's contents and all assoicated listeners.
 * This is called on dispose as well as on hide.
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.eraseElement_ = function() {
  var element = this.getElement();
  if (element) {
    this.getDomHelper().removeChildren(
      this.getElementByClass(goog.getCssName('eb-content')));
    this.getDomHelper().removeChildren(
      this.getElementByClass(goog.getCssName('eb-title')));
  }
};

/**
 * Computes the class for rendering the arrow for a given bubble orientation.
 *
 * @param {goog.positioning.Corner} corner The corner.
 * @param {Element} anchor The element to which we are attaching.
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.setArrowPosition_ =
  function(corner, anchor) {

  var arrow = this.getElementByClass(goog.getCssName('eb-arrow'));
  goog.dom.classes.set(arrow, goog.getCssName('eb-arrow'));

  var arrowBorder = this.getElementByClass(goog.getCssName('eb-arrow-border'));
  goog.dom.classes.set(arrowBorder, goog.getCssName('eb-arrow-border'));
  var margin = this.popup_.getMargin().left + this.popup_.getMargin().right;
  margin -= goog.style.getSize(anchor).width / 2;
  switch (corner) {
    case goog.positioning.Corner.TOP_LEFT:
    case goog.positioning.Corner.TOP_START:
      arrow.style.top = '1px';
      arrow.style.left = - margin + 'px';
      arrow.style.bottom = '';
      arrow.style.right = '';
      arrowBorder.style.top = 0;
      arrowBorder.style.left = - margin + 'px';
      arrowBorder.style.bottom = '';
      arrowBorder.style.right = '';
      goog.dom.classes.add(arrow, goog.getCssName('eb-corner-top-left'));
      goog.dom.classes.add(arrowBorder,
        goog.getCssName('eb-corner-top-left-border'));
      break;
    case goog.positioning.Corner.TOP_RIGHT:
    case goog.positioning.Corner.TOP_END:
      arrow.style.top = '1px';
      arrow.style.right = - margin + 'px';
      arrow.style.bottom = '';
      arrow.style.left = '';
      arrowBorder.style.top = 0;
      arrowBorder.style.right = - margin + 'px';
      arrowBorder.style.bottom = '';
      arrowBorder.style.left = '';
      goog.dom.classes.add(arrow, goog.getCssName('eb-corner-top-right'));
      goog.dom.classes.add(arrowBorder,
        goog.getCssName('eb-corner-top-right-border'));
      break;
    case goog.positioning.Corner.BOTTOM_LEFT:
    case goog.positioning.Corner.BOTTOM_START:
      arrow.style.bottom = '1px';
      arrow.style.left = - margin + 'px';
      arrow.style.top = '';
      arrow.style.right = '';
      arrowBorder.style.bottom = 0;
      arrowBorder.style.left = - margin + 'px';
      arrowBorder.style.top = '';
      arrowBorder.style.right = '';
      goog.dom.classes.add(arrow, goog.getCssName('eb-corner-bottom-left'));
      goog.dom.classes.add(arrowBorder,
        goog.getCssName('eb-corner-bottom-left-border'));
      break;
    case goog.positioning.Corner.BOTTOM_RIGHT:
    case goog.positioning.Corner.BOTTOM_END:
      arrow.style.bottom = '1px';
      arrow.style.right = - margin + 'px';
      arrow.style.top = '';
      arrow.style.left = '';
      arrowBorder.style.bottom = 0;
      arrowBorder.style.right = - margin + 'px';
      arrowBorder.style.top = '';
      arrowBorder.style.left = '';
      goog.dom.classes.add(arrow, goog.getCssName('eb-corner-bottom-right'));
      goog.dom.classes.add(arrowBorder,
        goog.getCssName('eb-corner-bottom-right-border'));
      break;
    default:
      throw Error('This corner type is not supported by bubble!');
  }
};

/**
 * Computes the pinned corner for the bubble.
 *
 * @param {Element} anchor The element to which the button is attached.
 * @return {goog.positioning.Corner} The pinned corner.
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.computeCorner_ = function(anchor) {
  var viewportElement = goog.style.getClientViewportElement(
    this.getDomHelper().getOwnerDocument(anchor));
  var anchorOffset = goog.style.getPageOffset(anchor);
  var anchorSize = goog.style.getSize(anchor);
  var corner = 0;
  if (viewportElement.offsetHeight - anchorOffset.y - anchorSize.height <
      anchorOffset.y) {
    corner += goog.positioning.CornerBit.BOTTOM;
  }
  return corner;
};

/**
 * Computes the margin for the bubble.
 *
 * @param {goog.positioning.Corner} corner The pinned corner.
 * @param {Element} anchor anchor The element to which we are attaching.
 * @return {goog.math.Box} margin.
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.computeMargin_ =
  function(corner, anchor) {
  var size = (goog.style.getSize(this.getElement()).width / 2) -
    goog.style.getSize(anchor).width / 2;
  var anchorOffset = goog.style.getPageOffset(anchor);
  var margin = new goog.math.Box(0, 0, 0, 0);
  var delta = goog.style.getClientViewportElement(
    this.getDomHelper().getOwnerDocument(anchor)).offsetWidth -
    anchorOffset.x - goog.style.getSize(anchor).width - size;
  if (delta < 0) {
    size -= delta - 10;
  }
  margin.left -= size;
  return margin;
};


/**
 * Computes position for the bubble.
 *
 * @param {goog.positioning.Corner} corner The pinned corner.
 * @param {Element} anchor The element to which we are attaching.
 * @return {goog.math.Box} margin.
 * @private
 */
bluemind.calendar.ui.event.Bubble.prototype.computePosition_ =
  function(corner, anchor) {
  var position = goog.style.getPageOffset(anchor);
  if (!(corner & goog.positioning.CornerBit.BOTTOM)) {
    position.y += anchor.offsetHeight;
  }
  return new goog.positioning.AbsolutePosition(position);
};

/**
 * build the bubble title
 *
 * @return {string} title string to display inside the title.
 */
bluemind.calendar.ui.event.Bubble.prototype.buildTitle = function() {
  var ret = '';
  var end = this.getModel().getEnd().clone();
  end.add(new goog.date.Interval(goog.date.Interval.SECONDS, -1));

  if (this.getModel().isAllday()) {
    if (goog.date.isSameDay(this.getModel().getDate(), end)) {
      ret = this.dateTimeHelper_.formatDate(this.getModel().getDate());
    } else {
      ret = this.dateTimeHelper_.formatDate(this.getModel().getDate()) + ' - ' +
        this.dateTimeHelper_.formatDate(end);
    }
  } else {
    if (goog.date.isSameDay(this.getModel().getDate(), end)) {
      ret = this.dateTimeHelper_.formatDateTime(this.getModel().getDate()) +
        ' - ' + this.dateTimeHelper_.formatTime(this.getModel().getEnd());
    } else {
      ret = this.dateTimeHelper_.formatDateTime(this.getModel().getDate()) +
        ' - ' + this.dateTimeHelper_.formatDateTime(this.getModel().getEnd());
    }
  }
  return ret;
};

/**
 * build the bubble content
 *
 * @return {Element} content HTML to display inside the bubble.
 */
bluemind.calendar.ui.event.Bubble.prototype.buildContent = function() {
  return this.dom_.createElement('p');
};

/** @inheritDoc */
bluemind.calendar.ui.event.Bubble.prototype.getContentElement = function() {
  return this.getElementByClass(goog.getCssName('eb-content'));
};

