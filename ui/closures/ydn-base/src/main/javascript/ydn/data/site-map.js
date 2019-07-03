// The MIT License (MIT)
// Copyright © 2012 Mechanobiology Institute, National University of Singapore.
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
 * @fileoverview Sitemap in tree data structure with list of children.
 *
 * @author mbikt@nus.edu.sg (Kyaw Tun)
 */

goog.provide('ydn.data.SiteMap');



/**
 * Create a site map data.
 * @param {string} url node url.
 * @param {string=} opt_title title.
 * @param {boolean=} opt_ignore_sub_pages Ignore subpage into the site map.
 * Subpage are denoted by page name starts with underscore flanked digit,
 * such as _12_page_title.
 * @constructor
 * @struct
 */
ydn.data.SiteMap = function(url, opt_title, opt_ignore_sub_pages) {

  /**
   * @type {string}
   * @final
   */
  this.url = url || '';
  /**
   * URL path consokdering also for Google Site url.
   * @final
   * @type {string}
   */
  this.path = this.url.replace(ydn.data.SiteMap.RE_PATH, '');
  /**
   * @type {string|undefined}
   */
  this.title = opt_title;
  /**
   * @type {string|undefined}
   */
  this.id = undefined;
  /**
   * @type {string|undefined}
   */
  this.name = undefined;
  /**
   * @type {string|undefined}
   */
  this.last_modified = undefined;
  /**
   * @type {!Array.<!ydn.data.SiteMap>}
   * @final
   * @private
   */
  this.children_ = [];
  /**
   * Sub pages.
   * @final
   * @type {!Array.<!ydn.data.SiteMap>} id of subpages.
   * @private
   */
  this.sub_pages_ = [];
  this.ignore_sub_pages_ = !!opt_ignore_sub_pages;

};


/**
 * Extract URL path.
 * @type {RegExp}
 */
ydn.data.SiteMap.RE_PATH = null;


/**
 * Node to be sorted.
 * @type {boolean}
 * @protected
 */
ydn.data.SiteMap.prototype.sorted = false;


/**
 * Ignore subpage into the site map. Subpage are denoted by page name
 * starts with underscore flanked digit, such as _12_page_title.
 * @type {boolean}
 * @private
 */
ydn.data.SiteMap.prototype.ignore_sub_pages_ = false;


/**
 * Default compare function by url.
 * @param {ydn.data.SiteMap} a
 * @param {ydn.data.SiteMap} b
 * @return {number}
 */
ydn.data.SiteMap.cmpByUrl = function(a, b) {
  return a.url > b.url ? 1 : b.url > a.url ? -1 : 0;
};


/**
 * Default compare function by Name.
 * @param {ydn.data.SiteMap} a
 * @param {ydn.data.SiteMap} b
 * @return {number}
 */
ydn.data.SiteMap.cmpByName = function(a, b) {
  return a.name > b.name ? 1 : b.name > a.name ? -1 : 0;
};


/**
 * Default compare function by Title.
 * @param {ydn.data.SiteMap} a
 * @param {ydn.data.SiteMap} b
 * @return {number}
 */
ydn.data.SiteMap.cmpByTitle = function(a, b) {
  return a.title > b.title ? 1 : b.title > a.title ? -1 : 0;
};


/**
 * Compare with other node.
 * @param {ydn.data.SiteMap} node other node to compare with.
 * @return {boolean} true if equivalent node.
 */
ydn.data.SiteMap.prototype.equals = function(node) {
  if (!node) {
    return false;
  }
  return ydn.data.SiteMap.cmpByUrl(this, node) == 0;
};


/**
 * Find tree node in all decendent nodes.
 * @param {string} url node url to find.
 * @return {ydn.data.SiteMap} node if found.
 */
ydn.data.SiteMap.prototype.find = function(url) {
  if (this.url == url) {
    return this;
  } else {
    for (var i = 0; i < this.children_.length; i++) {
      var node = this.children_[i].find(url);
      if (node) {
        return node;
      }
    }
    for (var i = 0; i < this.sub_pages_.length; i++) {
      var node = this.sub_pages_[i].find(url);
      if (node) {
        return node;
      }
    }
  }
  return null;
};


/**
 * Find tree node in all decendent nodes.
 * @param {string} name node url to find.
 * @return {ydn.data.SiteMap} node if found.
 */
ydn.data.SiteMap.prototype.findByName = function(name) {
  if (this.name == name) {
    return this;
  } else {
    for (var i = 0; i < this.children_.length; i++) {
      var node = this.children_[i].findByName(name);
      if (node) {
        return node;
      }
    }
    for (var i = 0; i < this.sub_pages_.length; i++) {
      var node = this.sub_pages_[i].findByName(name);
      if (node) {
        return node;
      }
    }
  }
  return null;
};


/**
 * Find tree node in all decendent nodes.
 * @param {string} id page id to find.
 * @return {ydn.data.SiteMap} node if found.
 */
ydn.data.SiteMap.prototype.getById = function(id) {
  if (this.id == id) {
    return this;
  } else {
    for (var i = 0; i < this.children_.length; i++) {
      var node = this.children_[i].getById(id);
      if (node) {
        return node;
      }
    }
    for (var i = 0; i < this.sub_pages_.length; i++) {
      var node = this.sub_pages_[i].getById(id);
      if (node) {
        return node;
      }
    }
  }
  return null;
};


/**
 * Traverse tree.
 * @param {string} url url to find.
 * @param {Array.<!ydn.data.SiteMap>} arr list of node to reach given url.
 * @return {ydn.data.SiteMap} node if found.
 */
ydn.data.SiteMap.prototype.walk = function(url, arr) {
  if (!url) {
    return null;
  }
  var path = url;
  if (ydn.data.SiteMap.RE_PATH) {
    path = url.replace(ydn.data.SiteMap.RE_PATH, '');
  }
  arr.push(this);
  if (this.path == path) {
    return this;
  } else {
    for (var i = 0; i < this.children_.length; i++) {
      var node = this.children_[i].walk(url, arr);
      if (node) {
        return node;
      }
    }
    for (var i = 0; i < this.sub_pages_.length; i++) {
      if (this.sub_pages_[i].path == path) {
        arr.push(this.sub_pages_[i]);
        return this.sub_pages_[i];
      }
    }
  }
  arr.pop();
  return null;
};


/**
 * @return {string|undefined}
 */
ydn.data.SiteMap.prototype.getPageName = function() {
  return this.name;
};


/**
 * @param {string} name page name.
 */
ydn.data.SiteMap.prototype.setPageName = function(name) {
  this.name = name;
};


/**
 * @param {boolean} val Sort label by title.
 */
ydn.data.SiteMap.prototype.setSorted = function(val) {
  this.sorted = val;
};


/**
 * subpage are start with digit and underscore like 2_sub_title
 * @param {string=} name
 * @return {boolean}
 */
ydn.data.SiteMap.isSubPage = function(name) {
  return !!name && /\d+_/.test(name);
};


/**
 * subpage are start with digit and underscore like 2_sub_title
 * @return {boolean}
 */
ydn.data.SiteMap.prototype.isSubPage = function() {
  return ydn.data.SiteMap.isSubPage(this.name);
};


/**
 * Add a child. Child node will inherit properties which are sorted and
 * ignore_sub_pages_.
 * @param {!ydn.data.SiteMap} node node to inserted.
 */
ydn.data.SiteMap.prototype.add = function(node) {
  if (this.ignore_sub_pages_ && node.isSubPage()) {
    goog.array.binaryInsert(this.sub_pages_, node,
        ydn.data.SiteMap.cmpByName);
    return;
  }
  if (this.sorted) {
    goog.array.binaryInsert(this.children_, node,
        ydn.data.SiteMap.cmpByTitle);
  } else {
    this.children_.push(node);
  }
  node.sorted = this.sorted;
  node.ignore_sub_pages_ = this.ignore_sub_pages_;
};


/**
 * Number of children.
 * @return {number}
 */
ydn.data.SiteMap.prototype.count = function() {
  return this.children_.length;
};


/**
 * Number of sub pages.
 * @return {number}
 */
ydn.data.SiteMap.prototype.countSubPage = function() {
  return this.sub_pages_.length;
};


/**
 * Get the child at given index.
 * Note: children are sorted.
 * @param {number} idx index.
 * @return {!ydn.data.SiteMap}
 */
ydn.data.SiteMap.prototype.child = function(idx) {
  return this.children_[idx];
};


/**
 * Get the sub page at given index.
 * Note: children are sorted.
 * @param {number} idx index.
 * @return {!ydn.data.SiteMap}
 */
ydn.data.SiteMap.prototype.subPage = function(idx) {
  return this.sub_pages_[idx];
};


/**
 * @return {Array.<string>}
 */
ydn.data.SiteMap.prototype.getSubPageIds = function() {
  return goog.array.map(this.sub_pages_, function(x) {
    return x.id;
  });
};


/**
 * Return title or url as label.
 * @return {string}
 */
ydn.data.SiteMap.prototype.getLabel = function() {
  return this.title || this.name || this.url;
};


/**
 * @inheritDoc
 */
ydn.data.SiteMap.prototype.toJSON = function() {
  var json = {};
  if (this.url) {
    json['url'] = this.url;
  }
  if (this.title) {
    json['title'] = this.title;
  }
  if (this.id) {
    json['id'] = this.id;
  }
  if (this.name) {
    json['name'] = this.name;
  }
  if (this.children_.length > 0) {
    json['children'] = [];
    for (var i = 0; i < this.children_.length; i++) {
      json['children'].push(this.children_[i].toJSON());
    }
  }
  if (this.sub_pages_.length > 0) {
    json['subpages'] = [];
    for (var i = 0; i < this.sub_pages_.length; i++) {
      json['subpages'].push(this.sub_pages_[i].toJSON());
    }
  }
  return json;
};


/**
 * @param {string} origin
 * @param {Array.<string>} buffer
 * @private
 */
ydn.data.SiteMap.prototype.buildSiteMap_ = function(origin, buffer) {
  if (this.url) {
    buffer.push('<url>');
    buffer.push('<loc>' + origin + this.path + '</loc>');
    if (this.last_modified) {
      buffer.push('<lastmod>' + this.last_modified + '</lastmod>');
    }
    buffer.push('</url>');
  }
  for (var i = 0; i < this.sub_pages_.length; i++) {
    this.sub_pages_[i].buildSiteMap_(origin, buffer);
  }
  for (var i = 0; i < this.children_.length; i++) {
    this.children_[i].buildSiteMap_(origin, buffer);
  }
};


/**
 * Get sitemap.xml file.
 * http://www.sitemaps.org/protocol.html
 * @param {string} origin
 * @return {string}
 */
ydn.data.SiteMap.prototype.toXml = function(origin) {
  var buffer = ['<?xml version="1.0" encoding="UTF-8"?>'];
  buffer.push('<urlset>');
  this.buildSiteMap_(origin, buffer);
  buffer.push('</urlset>');
  return buffer.join('\n');
};


/**
 * @inheritDoc
 */
ydn.data.SiteMap.prototype.toString = function() {
  return 'SiteMap:' + this.getLabel();
};


/**
 * @param {!Object} json site map in json.
 * @param {boolean=} opt_ignore_sub_pages
 * @return {ydn.data.SiteMap}
 */
ydn.data.SiteMap.fromJSON = function(json, opt_ignore_sub_pages) {
  var sitemap = new ydn.data.SiteMap(json['url'], json['title'],
      opt_ignore_sub_pages);
  sitemap.id = json['id'];
  sitemap.name = json['name'];
  var children = json['children'] || [];
  for (var i = 0; i < children.length; i++) {
    sitemap.children_.push(ydn.data.SiteMap.fromJSON(children[i], opt_ignore_sub_pages));
  }
  if (!opt_ignore_sub_pages) {
    var subpages = json['subpages'] || [];
    for (var i = 0; i < subpages.length; i++) {
      sitemap.sub_pages_.push(ydn.data.SiteMap.fromJSON(subpages[i], opt_ignore_sub_pages));
    }
  }
  return sitemap;
};


/**
 * @param {Document} xml
 * @param {string} root_url
 * @return {ydn.data.SiteMap} `null` if root_url not found in sitemap.
 */
ydn.data.SiteMap.fromXML = function(xml, root_url) {
  var n = xml.documentElement.childElementCount;
  var urlsets = {};
  for (var i = 0; i < n; i++) {
    var page = xml.documentElement.children[i];
    var url = page.children[0].textContent.trim();
    var title = page.children[2].textContent.trim();
    var idx = url.substr(0, url.length - 1).lastIndexOf('/');
    var parent = url.substr(0, idx + 1) + 'index.html';
    if (url.charAt(url.length - 1) == '/') {
      url = url + 'index.html';
    }
    urlsets[url] = {
      title: title,
      url: url,
      parent: parent
    };
  }
  var getChildren = function(url) {
    var ch = [];
    for (var name in urlsets) {
      var page = urlsets[name];
      if (page.parent == url) {
        ch.push(page);
      }
    }
    return ch;
  };
  if (!urlsets[root_url]) {
    return null;
  }
  var map = new ydn.data.SiteMap(root_url, urlsets[root_url].title);
  var appendChildren = function(map) {
    var children = getChildren(map.url);
    for (var i = 0; i < children.length; i++) {
      var ch = new ydn.data.SiteMap(children[i].url, children[i].title);
      appendChildren(ch);
      map.add(ch);
    }
  };
  appendChildren(map);
  return map;
};
