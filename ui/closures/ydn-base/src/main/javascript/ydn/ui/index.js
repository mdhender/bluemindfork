// Copyright 2012 YDN Authors. All Rights Reserved.
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
 * @fileoverview Base utilities for YDN-UI module.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */

goog.provide('ydn.ui');


/**
 * @type {Document} default template document
 * @private
 */
ydn.ui.template_doc_ = null;


/**
 * @type {?string} default template document URL.
 * @private
 */
ydn.ui.template_url_ = null;


/**
 * Set default template document.
 * @param {Document|string} doc Document or URL of the document to load. Note:
 * Document will be load asynchronously.
 * @param {Function=} opt_cb callback after loaded.
 */
ydn.ui.setTemplateDocument = function(doc, opt_cb) {
  if (goog.isString(doc)) {
    ydn.ui.template_url_ = doc;
    if (!ydn.ui.template_doc_ || ydn.ui.template_doc_.URL != doc) {
      ydn.ui.template_doc_ = null;
      var xhr = new XMLHttpRequest();
      xhr.open('GET', ydn.ui.template_url_, true);
      xhr.onload = function() {
        var parser = new DOMParser();
        ydn.ui.template_doc_ = parser.parseFromString(xhr.responseText, 'text/html');
        xhr = null;
        if (opt_cb) {
          opt_cb();
        }
      };
      xhr.send();
    }
  } else {
    ydn.ui.template_doc_ = doc;
    if (opt_cb) {
      opt_cb();
    }
  }
};


/**
 * Load template synchronously.
 * @return {Document}
 * @private
 */
ydn.ui.getTemplateDocument_ = function() {
  if (!ydn.ui.template_doc_) {
    if (goog.DEBUG && !ydn.ui.template_url_) {
      throw new Error('default template document not set.');
    }
    var xhr = new XMLHttpRequest();
    xhr.open('GET', ydn.ui.template_url_, false);
    xhr.onload = function() {
      var parser = new DOMParser();
      ydn.ui.template_doc_ = parser.parseFromString(xhr.responseText, 'text/html');
      xhr = null;
    };
    xhr.send();
  }
  return ydn.ui.template_doc_;
};


/**
 * Get or import template element.
 * <pre>
 *   var t = ydn.ui.getTemplateById('template-id').content;
 *   el.appendChild(t.cloneNode(true));
 * </pre>
 * @param {string} id get template element by id from template document.
 * @param {Document=} opt_doc template document. Default to default template
 * document.
 * @return {Element}
 */
ydn.ui.getTemplateById = function(id, opt_doc) {
  var el = document.getElementById(id);
  if (!el) {
    var doc = opt_doc || ydn.ui.getTemplateDocument_();
    el = doc.documentElement.querySelector('#' + id);
  }
  if (!document.body.contains(el)) {
    el = document.importNode(el, true);
    document.body.appendChild(el);
  }
  return /** @type {Element} */ (el);
};


/**
 * Instead of creating a new tab, open like a dialog box.
 * @param {Event} e
 */
ydn.ui.openPageAsDialog = function(e) {
  e.preventDefault();
  var w = 600;
  var h = 400;
  var wh = e.target.getAttribute('data-window-height');
  var ww = e.target.getAttribute('data-window-width');
  if (ww) {
    w = parseInt(ww, 10);
  }
  if (wh) {
    h = parseInt(wh, 10);
  }
  // dual monitor solution
  var left = (window.innerWidth / 2) - (w / 2) + window.screenLeft;
  var top = (window.innerWidth / 2) - (h / 2) + window.screenTop;
  var url = e.target.href;
  window.open(url, undefined, 'toolbar=no, location=no, directories=no, status=no, menubar=no, scrollbars=no, resizable=no, copyhistory=no, width=' + w + ', height=' + h + ', top=' + top + ', left=' + left);
};


/**
 * Dump all css rules to console.
 * @private
 */
ydn.ui.dumpAllCssRules_ = function() {
  var text = ['/*  Created with ydn.ui.dumpAllCssRules_() */'];
  for (var i = 0; i < document.styleSheets.length; i++) {
    var ss = document.styleSheets[i];
    for (var j = 0; j < ss.rules.length; j++) {
      var rule = ss.rules[j];
      text.push(rule.cssText);
    }
  }
  window.all_css_rules = text.join('\n');
  window.console.log(window.all_css_rules);
  window.console.log('copy(window.all_css_rules); // to copy clipboard');
  // chrome command line api
  // copy(window.all_css_rules);
  // console.log('All css rule copy to clipboard');
};
