// The MIT License (MIT)
// Copyright © 2013 Mechanobiology Institute, National University of Singapore.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the “Software”), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.


/**
 * @fileoverview Breadcrumb UI.
 *
 * Inspire by xBreadcrumbs:
 * http://codecanyon.net/item/xbreadcrumbs-dynamic-expandable-navigation/5459238
 * http://www.ajaxblender.com/xbreadcrumbs.html
 * @author mbikt@nus.edu.sg (Kyaw Tun)
 */


goog.provide('ydn.ui.Breadcrumb');
goog.require('goog.Uri');
goog.require('goog.dom');
goog.require('goog.dom.classes');
goog.require('goog.events');
goog.require('goog.events.EventTarget');
goog.require('goog.fx.css3');
goog.require('goog.style');
goog.require('ydn.data.SiteMap');



/**
 * Create a breadcrumb control.
 * @param {ydn.data.SiteMap=} opt_model sitemap.
 * @param {boolean=} opt_dispatch_click prevent default click and dispatch
 * the event from this instance instead.
 * @constructor
 * @extends {goog.events.EventTarget}
 * @struct
 */
ydn.ui.Breadcrumb = function(opt_model, opt_dispatch_click) {
  goog.base(this);
  /**
   * @type {ydn.data.SiteMap}
   * @private
   */
  this.model_ = opt_model || null;
  /**
   * @type {Element}
   * @private
   */
  this.root_ = null;
  /**
   * @type {Element}
   * @private
   */
  this.ele_peer_ = null;
  /**
   * @type {Element}
   * @private
   */
  this.ul_ = null;
  this.dispatchClick_ = !!opt_dispatch_click;
  /**
   * Detect iframe rendering. This is initialize in init() method.
   * @protected
   * @type {boolean}
   */
  this.in_iframe = false;
};
goog.inherits(ydn.ui.Breadcrumb, goog.events.EventTarget);


/**
 * @define {boolean} debug flag;
 */
ydn.ui.Breadcrumb.DEBUG = false;


/**
 * A collapsible breadcomb label shorten if not active node. It dynamically
 * expend upon hover.
 * @type {boolean}
 */
ydn.ui.Breadcrumb.prototype.collapsible = true;


/**
 * Default width of collapsed width.
 * @type {number}
 */
ydn.ui.Breadcrumb.prototype.collapsedWidth = 60;


/**
 * Render UI.
 * @param {Element} ele root element of the breadcrumb.
 * @param {Element=} opt_ele_peer option element for peer nodes.
 */
ydn.ui.Breadcrumb.prototype.render = function(ele, opt_ele_peer) {
  if (ydn.ui.Breadcrumb.DEBUG) {
    window.console.log('rendering breadcrumb');
  }
  this.root_ = ele;
  // goog.style.setHeight(ele, '2em');
  this.ul_ = document.createElement('UL');
  this.ul_.className = 'ydn-breadcrumb';
  if (opt_ele_peer) {
    this.ele_peer_ = document.createElement('UL');
    this.ele_peer_.className = 'ydn-breadcrumb-sidemenu';
    opt_ele_peer.innerHTML = '';
    opt_ele_peer.appendChild(this.ele_peer_);
    goog.style.setElementShown(opt_ele_peer, false);
    if (this.dispatchClick_) {
      goog.events.listen(this.ele_peer_, goog.events.EventType.CLICK,
          this.handleClick, false, this);
    }
  }
  /*
  goog.events.listen(this.ul_, goog.events.EventType.MOUSEOVER,
      this.handleMouseOver, false, this);
  goog.events.listen(this.ul_, goog.events.EventType.MOUSEOUT,
      this.handleMouseOut, false, this);
  if (this.dispatchClick_) {
    goog.events.listen(this.ul_, goog.events.EventType.CLICK,
        this.handleClick, false, this);
  }
  */
  this.root_.innerHTML = '';
  this.root_.appendChild(this.ul_);
  this.in_iframe = ele.ownerDocument != window.document;

  if (this.model_) {
    this.walk(window.location.pathname);
  }
};


/**
 * Return path from google site url.
 * @param {string} url
 * @return {string} path.
 */
ydn.ui.Breadcrumb.url2Path = function(url) {
  if (!url) {
    return '';
  }
  if (url.indexOf('//') >= 0) {
    url = (new goog.Uri(url)).getPath();
  }
  return url;
};


/**
 * Render breadcrumb using site map tree structure. Existing breadcrumb will
 * be disposed.
 * @param {!Array.<ydn.data.SiteMap>} maps list of SiteMap node from left
 * node to the right node in order.
 * @private
 */
ydn.ui.Breadcrumb.prototype.initBreadcrumb_ = function(maps) {
  this.ul_.innerHTML = '';
  var ul = this.ul_;
  for (var i = 0; i < maps.length; i++) {
    var map = maps[i];
    var li = document.createElement('LI');
    li.className = 'ydn-breadcrumb-li';
    var a = document.createElement('A');
    a.textContent = map.getLabel();
    var href = ydn.ui.Breadcrumb.url2Path(map.url);
    if (href) {
      a.href = href;
    }
    // console.log(a);
    li.appendChild(a);
    ul.appendChild(li);
    // drop down menu for children
    var n = map.count();
    // console.log(i, map.count(), n, map.title);
    if (n > 0) {
      var menu = document.createElement('UL');
      // menu.className = 'ydn-breadcrumb-menu';
      var next = maps[i + 1];
      for (var j = 0; j < n; j++) {
        var child = map.child(j);
        if (child.equals(next)) {
          continue;
        }
        var menu_li = document.createElement('LI');
        menu_li.className = 'ydn-breadcrumb-menu';
        var ma = document.createElement('A');
        ma.textContent = child.getLabel();
        ma.href = ydn.ui.Breadcrumb.url2Path(child.url);
        menu_li.appendChild(ma);
        menu.appendChild(menu_li);
      }
      li.appendChild(menu);
    }
    if (i == maps.length - 1) {
      li.className = 'ydn-breadcrumb-li current';
    }
  }
  /*
  if (this.collapsible && !this.in_iframe) {
    for (var i = 0, n = this.ul_.childElementCount; i < n; i++) {
      var li = this.ul_.children[i];
      var a = li.getElementsByTagName('A')[0];
      if (a) {
        a.style.whiteSpace = 'nowrap';
        goog.style.setFloat(a, 'left');
        if (i != n - 1) {
          a.style.overflow = 'hidden';
          var sz = goog.style.getSize(a);
          if (sz.width > this.collapsedWidth) {
            goog.style.setWidth(a, this.collapsedWidth);
          }
        }
      }
    }
  }
  */
};


/**
 * Initialize peer items.
 * @param {ydn.data.SiteMap} current current node.
 * @param {ydn.data.SiteMap} parent parent node.
 * @param {ydn.data.SiteMap} grand grand parent node.
 * @private
 */
ydn.ui.Breadcrumb.prototype.initMenu_ = function(current, parent, grand) {
  if (!this.ele_peer_) {
    return;
  }
  if (parent && parent.count() > 1) {
    this.ele_peer_.innerHTML = '';
    for (var i = 0, n = parent.count(); i < n; i++) {
      var peer = parent.child(i);
      var li = document.createElement('LI');
      var a = document.createElement('A');
      a.textContent = peer.getLabel();
      var href = ydn.ui.Breadcrumb.url2Path(peer.url);
      if (href) {
        a.href = href;
      }
      li.appendChild(a);
      if (current.url == peer.url) {
        a.className = 'current';
        var ul = document.createElement('UL');
        ul.className = 'ydn-breadcrumb-sidemenu';
        li.appendChild(ul);
        for (var j = 0, cn = peer.count(); j < cn; j++) {
          var child = peer.child(j);
          var cli = document.createElement('LI');
          var ca = document.createElement('A');
          ca.textContent = child.getLabel();
          ca.href = ydn.ui.Breadcrumb.url2Path(child.url);
          cli.appendChild(ca);
          ul.appendChild(cli);
        }
      }
      this.ele_peer_.appendChild(li);
    }
    goog.style.setElementShown(this.ele_peer_, true);
  } else if (current && current.count() > 0) {
    this.ele_peer_.innerHTML = '';
    for (var k = 0, n = current.count(); k < n; k++) {
      var ch = current.child(k);
      var li = document.createElement('LI');
      var a = document.createElement('A');
      a.textContent = ch.getLabel();
      a.href = ydn.ui.Breadcrumb.url2Path(ch.url);
      li.appendChild(a);
      this.ele_peer_.appendChild(li);
    }
    goog.style.setElementShown(this.ele_peer_, true);
  } else if (grand && grand.count() > 0) {
    this.ele_peer_.innerHTML = '';
    for (var k = 0, n = grand.count(); k < n; k++) {
      var ch = grand.child(k);
      var li = document.createElement('LI');
      var a = document.createElement('A');
      a.textContent = ch.getLabel();
      a.href = ydn.ui.Breadcrumb.url2Path(ch.url);
      li.appendChild(a);
      this.ele_peer_.appendChild(li);
    }
    goog.style.setElementShown(this.ele_peer_, true);
  } else {
    goog.style.setElementShown(this.ele_peer_, false);
  }
};


/**
 * Render breadcrumb using site map tree structure. Existing breadcrumb will
 * be disposed.
 * @param {!Array.<ydn.data.SiteMap>} maps list of SiteMap node from left
 * node to the right node in order.
 * @see #walk
 */
ydn.ui.Breadcrumb.prototype.show = function(maps) {
  if (maps.length == 0) {
    return;
  }
  this.initBreadcrumb_(maps);
  this.initMenu_(maps[maps.length - 1], maps[maps.length - 2],
      maps[maps.length - 3]);
};


/**
 * @param {goog.events.BrowserEvent} e event.
 * @protected
 */
ydn.ui.Breadcrumb.prototype.handleMouseOver = function(e) {
  var ele = /** @type {Element} */ (e.target);
  var li = ele;
  if (li.tagName != 'LI' || !li.classList.contains('ydn-breadcrumb-li')) {
    li = goog.dom.getAncestorByTagNameAndClass(ele, 'LI', 'ydn-breadcrumb-li');
  }
  // console.log('in', ele.tagName, li);
  if (!li) {
    return;
  }
  if (this.collapsible) {
    var a = goog.dom.getElementsByTagNameAndClass('A', null, li)[0];
    if (a) {
      var initWidth = a.getAttribute('init-width');
      // window.console.log('restore', a.href, initWidth);
      if (initWidth) {
        goog.style.setWidth(a, parseFloat(initWidth));
      }
    }
  }
  if (li.classList.contains(li, 'hover')) {
    return;
  }
  this.hideAllSubLevels_();

  var sub_label = goog.dom.getElementsByTagNameAndClass('UL', null, li)[0];
  if (!ydn.ui.Breadcrumb.subLevelExists_(sub_label)) {
    return;
  }
  // Show sub-level
  this.showSubLevel_(sub_label);
};


/**
 * @param {Event} e event.
 * @return {boolean} true if handled.
 * @protected
 */
ydn.ui.Breadcrumb.prototype.handleClick = function(e) {
  var a = e.target;
  if (a.href) {
    e.preventDefault();
    this.dispatchEvent(e);
    return false;
  }
  return true;
};


/**
 * @param {Event} e event.
 * @protected
 */
ydn.ui.Breadcrumb.prototype.handleMouseOut = function(e) {
  var ele = /** @type {Element} */ (e.target);
  if (ele.tagName != 'UL' || ele.classList.contains('ydn-breadcrumb')) {
    return;
  }
  var li = ele;
  if (li.tagName != 'LI' || !li.classList.contains('ydn-breadcrumb-li')) {
    li = goog.dom.getAncestorByTagNameAndClass(ele, 'LI', 'ydn-breadcrumb-li');
  }
  var ul = goog.dom.getElementsByTagNameAndClass('UL', null, li)[0];
  // console.log('out', ul.tagName, ul, ele.tagName, ele);
  this.hideSubLevel_(ul);
  if (this.collapsible && !this.in_iframe) {
    var a = goog.dom.getElementsByTagNameAndClass('A', null, li)[0];
    if (a) {
      var sz = goog.style.getSize(a);
      if (sz.width > this.collapsedWidth) {
        goog.style.setWidth(a, this.collapsedWidth);
      }
    }
  }
};


/**
 * @param {Element} subLevel element.
 * @private
 */
ydn.ui.Breadcrumb.prototype.showSubLevel_ = function(subLevel) {
  var ele = subLevel;
  var parent = ele.parentElement;

  if (!parent) {
    return;
  }

  parent.classList.add('hover');

  ele.style.display = 'block';
  // goog.fx.css3.fadeIn(ele, 1);
};


/**
 * @param {Element} ele
 * @private
 */
ydn.ui.Breadcrumb.prototype.hideSubLevel_ = function(ele) {
  if (!ele) {
    return;
  }
  var parent = ele.parentElement;

  parent.classList.remove('hover');
  ele.style.display = '';
  // goog.fx.css3.fadeOut(ele, 1);
};


/**
 * @param {Element} obj
 * @return {boolean}
 * @private
 */
ydn.ui.Breadcrumb.subLevelExists_ = function(obj) {
  if (!obj) {
    return false;
  }
  for (var i = 0, n = obj.childElementCount; i < n; i++) {
    if (obj.children[i].tagName == goog.dom.TagName.LI) {
      return true;
    }
  }
  return false;
};


/**
 * @private
 */
ydn.ui.Breadcrumb.prototype.hideAllSubLevels_ = function() {
  // console.log('hide all sub levels')
  for (var i = 0, n = this.ul_.childElementCount; i < n; i++) {
    var li = this.ul_.children[i];
    for (var j = 0, m = li.childElementCount; j < m; j++) {
      var ul = li.children[j];
      if (ul.tagName == goog.dom.TagName.UL) {
        goog.style.setElementShown(ul, false);
        li.classList.remove('hover');
      }
    }
  }
};


/**
 * Show a site map.
 * @param {string} url
 * @param {ydn.data.SiteMap=} opt_site_map
 */
ydn.ui.Breadcrumb.prototype.walk = function(url, opt_site_map) {
  var arr = [];
  var site_map = this.model_ || opt_site_map;
  var node = site_map.walk(url, arr);
  if (!node && url.charAt(url.length - 1) == '/') {
    node = site_map.walk(url + 'index.html', arr);
  }
  if (ydn.ui.Breadcrumb.DEBUG) {
    window.console.log('breadcrumb walking to ' + url + ' results ' + arr.length +
        ' steps');
  }
  this.show(arr);
};
