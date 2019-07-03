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
 * @fileoverview View class for application header (Logo + logo).
 */

goog.provide("net.bluemind.contact.search.SearchView");

goog.require("goog.dom");
goog.require("goog.dom.classlist");
goog.require("goog.events.KeyCodes");
goog.require("goog.events.KeyHandler");
goog.require("goog.events.KeyHandler.EventType");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.ButtonRenderer");

/**
 * View class for application search form.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.contact.search.SearchView = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  var terms = [];
  // terms.push(new bluemind.model.search.SearchTerm('_all'));
  // var child = new bluemind.ui.SearchField(terms, null, opt_domHelper);
  /** @meaning general.search */
  var MSG_SEARCH = goog.getMsg('Search...')
  var child = new goog.ui.LabelInput(MSG_SEARCH);
  child.setId('search');
  this.addChild(child, true);

  child = new goog.ui.Button(goog.dom.createDom('div', [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'),
      goog.getCssName('fa-search'), goog.getCssName('search') ]), goog.ui.style.app.ButtonRenderer.getInstance());

  child.setId('button');
  this.addChild(child, true);
};

goog.inherits(net.bluemind.contact.search.SearchView, goog.ui.Component);

/** @override */
net.bluemind.contact.search.SearchView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();
  goog.dom.classlist.add(el, goog.getCssName('search'));
};

/** @override */
net.bluemind.contact.search.SearchView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (this.getModel()) {
    // this.getChild('search').setQuery(this.getModel());
    this.getChild('search').setValue(this.getModel());
  }
  var handler = new goog.events.KeyHandler(this.getChild('search').getElement());
  this.registerDisposable(handler);
  this.getHandler().listen(handler, goog.events.KeyHandler.EventType.KEY, function(e) {
    if (e.keyCode == goog.events.KeyCodes.ENTER) {
      this.dispatchEvent(goog.ui.Component.EventType.ACTION);
    }
  })
};

/** @override */
net.bluemind.contact.search.SearchView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  if (this.isInDocument()) {
    // this.getChild('search').setQuery(this.getModel());
    this.getChild('search').setValue(this.getModel());
  }
};
