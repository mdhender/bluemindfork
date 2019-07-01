// Copyright 2014 YDN Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/**
 * @fileoverview Infinite scroll decorator.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.ui.InfiniteScrollDecorator');
goog.provide('ydn.ui.InfiniteScrollItemProvider');



/**
 * Infinite scroll decorator.
 * @param {Element} el scroll base element.
 * @param {ydn.ui.InfiniteScrollItemProvider} provider
 * @constructor
 * @struct
 */
ydn.ui.InfiniteScrollDecorator = function(el, provider) {
  /**
   * @final
   * @type {ydn.ui.InfiniteScrollItemProvider}
   * @private
   */
  this.provider_ = provider;
  /**
   * @type {goog.async.Deferred}
   * @private
   */
  this.appending_df_ = null;
  /**
   * @type {number}
   * @private
   */
  this.prev_pos_ = el.scrollTop;
  /**
   * @type {?boolean}
   * @private
   */
  this.appending_ = null;
  /**
   * @type {Element}
   * @private
   */
  this.scroll_el_ = el;
  el.style.position = 'absolute';
  el.style.overflow = 'auto';
  el.style.top = '0';
  el.style.bottom = '0';
  el.style.left = '0';
  el.style.right = '0';
  el.addEventListener('scroll', this.onScroll_.bind(this), false);
  el.addEventListener('wheel', this.onScroll_.bind(this), false);
};


/**
 * @define {boolean} debug flag
 */
ydn.ui.InfiniteScrollDecorator.DEBUG = false;


/**
 * @define {number} reserve number of items.
 */
ydn.ui.InfiniteScrollDecorator.RESERVE = 5;


/**
 * @param {Event} ev
 * @private
 */
ydn.ui.InfiniteScrollDecorator.prototype.onScroll_ = function(ev) {
  if (ev.type == 'wheel') {
    if (this.prev_pos_ == ev.currentTarget.scrollTop) {
      var we = /** @type {WheelEvent} */(ev);
      this.appending_ = we.deltaY > 0;
    }
  } else {
    if (this.prev_pos_ != ev.currentTarget.scrollTop) {
      this.appending_ = this.prev_pos_ < ev.currentTarget.scrollTop;
      this.prev_pos_ = ev.currentTarget.scrollTop;
    }
  }
  if (ydn.ui.InfiniteScrollDecorator.DEBUG) {
    window.console.log(this.appending_ ? 'down' : 'up');
  }
  this.doAppend_();
};


/**
 * @private
 */
ydn.ui.InfiniteScrollDecorator.prototype.continue_ = function() {
  this.appending_df_ = null;
  this.doAppend_();
};


/**
 * @private
 */
ydn.ui.InfiniteScrollDecorator.prototype.stop_ = function() {
  this.appending_df_ = null;
};


/**
 * @private
 */
ydn.ui.InfiniteScrollDecorator.prototype.doAppend_ = function() {
  if (!this.appending_df_ && this.appending_ !== null && this.shouldAppend_()) {

    var should_remove = false;
    if (this.appending_) {
      var third = this.scroll_el_.children[ydn.ui.InfiniteScrollDecorator.RESERVE];
      if (third && !ydn.dom.isElementVisible(third)) {
        if (ydn.ui.InfiniteScrollDecorator.DEBUG) {
          window.console.log('removing first');
        }
        should_remove = true;
      }
    } else {
      var last_third = this.scroll_el_.children[this.scroll_el_.childElementCount -
          ydn.ui.InfiniteScrollDecorator.RESERVE];
      if (last_third && !ydn.dom.isElementVisible(last_third)) {
        if (ydn.ui.InfiniteScrollDecorator.DEBUG) {
          window.console.log('removing last');
        }
        should_remove = true;
      }
    }
    if (ydn.ui.InfiniteScrollDecorator.DEBUG) {
      window.console.log('appending');
    }

    this.appending_df_ = this.provider_.showMoreItemsOnScroll(!this.appending_,
        should_remove);
    this.appending_df_.addCallbacks(function() {
      setTimeout(this.continue_.bind(this), 10);
    }, function() {
      // we have to use async because we want to assign after resolving,
      // even if deferred is resolved synchronously.
      setTimeout(this.stop_.bind(this), 10);
    }, this);
    this.appending_ = null;
  }
};


/**
 * @private
 * @return {boolean}
 */
ydn.ui.InfiniteScrollDecorator.prototype.shouldAppend_ = function() {
  if (this.appending_) {
    var n = this.scroll_el_.childElementCount;
    for (var i = 1; i < ydn.ui.InfiniteScrollDecorator.RESERVE; i++) {
      var el = this.scroll_el_.children[n - i];
      if (!el || ydn.dom.isElementVisible(el)) {
        return true;
      }
    }
  } else {
    for (var i = 0; i < ydn.ui.InfiniteScrollDecorator.RESERVE; i++) {
      var el = this.scroll_el_.children[i];
      if (!el || ydn.dom.isElementVisible(el)) {
        return true;
      }
    }
  }
  return false;
};



/**
 * Provide element in the scroll as needed.
 * @interface
 */
ydn.ui.InfiniteScrollItemProvider = function() {};


/**
 * Append items to the scroll element. Number of items can be one or more.
 * @param {boolean} prepend if `true`, prepend, otherwise `append` items to
 * the list.
 * @param {boolean} should_remove should remove item before appending.
 * @return {!goog.async.Deferred} resolve after adding item.
 */
ydn.ui.InfiniteScrollItemProvider.prototype.showMoreItemsOnScroll = function(prepend, should_remove) {};

