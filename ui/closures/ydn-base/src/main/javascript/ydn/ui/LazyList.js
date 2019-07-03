/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2013 Sergi Mansilla
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the 'Software'), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/**
 * @fileoverview List panel for large items.
 *
 * @link https://github.com/sergi/virtual-list
 */


goog.provide('ydn.ui.LazyList');
goog.require('goog.async.Delay');
goog.require('goog.ui.Component');
goog.require('ydn.ui.ILazyListRenderer');
goog.require('ydn.ui.LazyListModel');
goog.require('ydn.ui.LazyListRenderer');
goog.require('goog.userAgent.product');


/**
 *
 * @param {ydn.ui.ILazyListModel} model
 * @param {ydn.ui.ILazyListRenderer} renderer
 * @param {goog.dom.TagName=} opt_tag
 * @param {goog.dom.DomHelper=} opt_dom
 * @constructor
 * @extends {goog.ui.Component}
 * @struct
 */
ydn.ui.LazyList = function(model, renderer, opt_tag, opt_dom) {
  ydn.ui.LazyList.base(this, 'constructor', opt_dom);
  /**
   * @type {goog.dom.TagName}
   * @private
   */
  this.tag_ = opt_tag || goog.dom.TagName.DIV;
  /**
   * @type {ydn.ui.ILazyListRenderer}
   * @private
   */
  this.renderer_ = renderer;
  this.setModel(model);

  /**
   * @protected
   * @type {number}
   */
  this.screenItemsLen = 0;

  /**
   * @protected
   * @type {number}
   */
  this.cachedItemsLen = 0;

  /**
   * @type {goog.async.Delay}
   * @private
   */
  this.delay_cleanup_ = new goog.async.Delay(this.cleanUpItems_, 100, this);

};
goog.inherits(ydn.ui.LazyList, goog.ui.Component);


/**
 * @return {ydn.ui.ILazyListModel}
 */
ydn.ui.LazyList.prototype.getModel = function() {
  return /** @type {ydn.ui.ILazyListModel} */(ydn.ui.LazyList.base(this, 'getModel'));
};


/**
 * @define {boolean} debug flag.
 */
ydn.ui.LazyList.DEBUG = true;

/**
 * @const
 * @type {string}
 */
ydn.ui.LazyList.CSS_CLASS = 'ydn-lazy-list';


/**
 * @override
 */
ydn.ui.LazyList.prototype.createDom = function() {
  var dom = this.getDomHelper();
  var el = ydn.ui.LazyList.createContainer_('100%', '100%', this.tag_);
  this.setElementInternal(el);
  el.classList.add(ydn.ui.LazyList.CSS_CLASS);

  var scroller = ydn.ui.LazyList.createScroller_(this.renderer_.getHeight() * this.getModel().getCount());
  el.appendChild(scroller);
};


/**
 * @inheritDoc
 */
ydn.ui.LazyList.prototype.disposeInternal = function() {
  this.delay_cleanup_.dispose();
  this.delay_cleanup_ = null;
  ydn.ui.LazyList.base(this, 'disposeInternal');
};


/**
 * @type {number}
 * @private
 */
ydn.ui.LazyList.prototype.lastRepaintY_ = 0;


/**
 * @param {goog.events.BrowserEvent} ev
 * @private
 */
ydn.ui.LazyList.prototype.onScroll_ = function(ev) {
  var el = this.getElement();
  var itemHeight = this.renderer_.getHeight();
  var maxBuffer = this.screenItemsLen * itemHeight;

  var scrollTop = el.scrollTop; // Triggers reflow
  if (!this.lastRepaintY_ || Math.abs(scrollTop - this.lastRepaintY_) > maxBuffer) {
    var first = ((scrollTop / itemHeight) | 0) - this.screenItemsLen;
    this.renderChunk_(this.getElement(), first < 0 ? 0 : first);
    this.lastRepaintY_ = scrollTop;
  }
  this.delay_cleanup_.start();
  ev.preventDefault();
};


/**
 * @override
 */
ydn.ui.LazyList.prototype.enterDocument = function() {
  ydn.ui.LazyList.base(this, 'enterDocument');

  this.getHandler().listen(this.getElement(), 'scroll', this.onScroll_);
  this.getHandler().listen(this.getModel(), goog.events.EventType.LOAD, this.reset);
  this.getHandler().listen(this.getModel(), goog.events.EventType.CHANGE, this.updated);

  this.reset();
};


/**
 * Reset.
 */
ydn.ui.LazyList.prototype.updated = function() {
  if (this.getElement().childElementCount == 1) {
    this.reset();
  } else {
    var h = this.renderer_.getHeight() * this.getModel().getCount();
    var el = this.getElement().querySelector('.ydn-lazy-list-scroller');
    goog.style.setHeight(el, h);
  }
};


/**
 * Reset.
 */
ydn.ui.LazyList.prototype.reset = function() {
  if (ydn.ui.LazyList.DEBUG) {
    console.log('reset');
  }
  this.delay_cleanup_.stop();
  this.lastRepaintY_ = 0;
  var el = this.getElement();

  var visible_height;
  if (goog.userAgent.product.SAFARI) {
    visible_height = el.parentElement.clientHeight;
    goog.style.setHeight(el, visible_height);
  } else {
    visible_height = el.clientHeight;
  }
  this.screenItemsLen = Math.ceil(visible_height / this.renderer_.getHeight());
  // Cache 4 times the number of items that fit in the container viewport
  this.cachedItemsLen = this.screenItemsLen * 3;
  var h = this.renderer_.getHeight() * this.getModel().getCount();
  goog.style.setHeight(el.querySelector('.ydn-lazy-list-scroller'), h);
  for (var i = el.childElementCount - 1; i > 0; i--) {
    el.removeChild(el.children[i]);
  }
  this.renderChunk_(el, 0);
  // HACK: Here calling offsetHeight is just to trigger a reflow.
  // otherwise, newly added elements are not show up, until user scroll the panel.
  el.scrollTop = Math.min(el.offsetHeight, 0);
};


ydn.ui.LazyList.prototype.createRow = function(i) {
  var itemHeight = this.renderer_.getHeight();
  var item = this.renderer_.render(this.getModel().getItemAt(i));
  item.style.position = 'absolute';
  item.style.top = (i * itemHeight) + 'px';
  item.setAttribute('data-offset', i);
  return item;
};


/**
 * @type {number}
 * @private
 */
ydn.ui.LazyList.prototype.prev_first_ = 0;


/**
 * Renders a particular, consecutive chunk of the total rows in the list. To
 * keep acceleration while scrolling, we mark the nodes that are candidate for
 * deletion instead of deleting them right away, which would suddenly stop the
 * acceleration. We delete them once scrolling has finished.
 *
 * @param {Element} node Parent node where we want to append the children chunk.
 * @param {number} from Starting position, i.e. first children index.
 * @return {void}
 */
ydn.ui.LazyList.prototype.renderChunk_ = function(node, from) {
  var finalItem = from + this.cachedItemsLen;
  var totalRows = this.getModel().getCount();

  if (ydn.ui.LazyList.DEBUG) {
    window.console.log('render from ' + from + ' to ' + finalItem + ' for ' + totalRows);
  }
  if (finalItem > totalRows) {
    finalItem = totalRows;
  }

  var prev_start = -1;
  var prev_end = -1;
  if (node.childElementCount > 1) {
    // Note: there is dummy element in the list
    prev_start = parseInt(node.firstElementChild.nextElementSibling.getAttribute('data-offset'), 10);
    prev_end = parseInt(node.lastElementChild.getAttribute('data-offset'), 10);
  }

  // Append all the new rows in a document fragment that we will later append to
  // the parent node
  var created = 0;
  var purged = 0;
  var fragment = document.createDocumentFragment();
  for (var i = from; i < finalItem; i++) {
    var already = i >= prev_start && i <= prev_end;
    if (!already) {
      fragment.appendChild(this.createRow(i));
      created++;
    }
  }

  // Hide and mark obsolete nodes for deletion.
  for (var i = 1; i < node.childElementCount; i++) {
    // Note: there is dummy element in the list
    var offset = parseInt(node.children[i].getAttribute('data-offset'), 10);
    if (offset < from || offset > finalItem) {
      node.children[i].style.display = 'none';
      node.children[i].setAttribute('data-rm', '1');
      purged++;
    }
  }

  if (ydn.ui.LazyList.DEBUG) {
    console.log(created + ' created' + ' ' + purged + ' purged from ' + prev_start + '-' + prev_end);
  }

  node.appendChild(fragment);
};


/**
 *
 * @private
 */
ydn.ui.LazyList.prototype.cleanUpItems_ = function() {
  var el = this.getElement();
  var badNodes = el.querySelectorAll('[data-rm="1"]');
  for (var i = 0, l = badNodes.length; i < l; i++) {
    el.removeChild(badNodes[i]);
  }
  if (ydn.ui.LazyList.DEBUG) {
    console.log(badNodes.length + ' elements clean');
  }
};


/**
 * @param {string} w
 * @param {string} h
 * @param {goog.dom.TagName=} opt_tag
 * @return {Element}
 * @private
 */
ydn.ui.LazyList.createContainer_ = function(w, h, opt_tag) {
  var c = document.createElement(opt_tag || goog.dom.TagName.DIV);
  c.style.width = w;
  c.style.height = h;
  c.style.overflowX = 'hidden';
  c.style.overflowY = 'scroll';
  c.style.position = 'relative';
  c.style.padding = 0;
  return c;
};


/**
 * @private
 * @param {number} h
 * @return {Element}
 */
ydn.ui.LazyList.createScroller_ = function(h) {
  var scroller = document.createElement('div');
  scroller.classList.add('ydn-lazy-list-scroller');
  scroller.style.opacity = 0;
  // scroller.style.position = 'absolute';
  scroller.style.top = '0';
  scroller.style.left = '0';
  scroller.style.width = '1px';
  scroller.style.height = h + 'px';
  return scroller;
};

