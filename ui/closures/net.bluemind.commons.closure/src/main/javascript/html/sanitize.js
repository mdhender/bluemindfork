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
 * @fileoverview  An HTML sanitizer that can satisfy a variety of security
 * policies. 
 *
 */

goog.provide('bluemind.html.Sanitizer');
goog.provide('bluemind.html.sanitize');

goog.require('goog.array');
goog.require('goog.string');
goog.require('goog.string.html.HtmlSanitizer');
goog.require('goog.string.html.HtmlSanitizer.AttributeType');
goog.require('goog.string.html.HtmlSanitizer.Attributes');
goog.require('goog.string.StringBuffer');
goog.require('goog.string.html.HtmlParser');
goog.require('goog.string.html.HtmlParser.EFlags');
goog.require('goog.string.html.HtmlParser.Elements');
goog.require('goog.string.html.HtmlSaxHandler');
goog.require('goog.uri.utils');

/**
 * Strips unsafe tags and attributes from HTML.
 *
 * @param {string} htmlText The HTML text to sanitize.
 * @return {string} A sanitized HTML, safe to be embedded on the page.
 */
bluemind.html.sanitize = function(htmlText) {
  var stringBuffer = new goog.string.StringBuffer();
  var handler = new bluemind.html.Sanitizer(stringBuffer);
  var parser = new goog.string.html.HtmlParser();
  parser.parse(handler, htmlText);
  return stringBuffer.toString();
};

/**
 * An implementation of the {@code goog.string.HtmlSaxHandler} interface that
 * will take each of the html tags and sanitize it.
 *
 * @param {goog.string.StringBuffer} stringBuffer A string buffer, used to
 *     output the html as we sanitize it.
 * @constructor
 * @extends {goog.string.html.HtmlSaxHandler}
 */
bluemind.html.Sanitizer = function(stringBuffer) {
  goog.base(this);

  this.stringBuffer_ = stringBuffer;
  this.stack_ = [];
  this.ignoring_ = false;
};

goog.inherits(bluemind.html.Sanitizer, goog.string.html.HtmlSaxHandler);


/**
 * Regular expression that matches &s.
 * @type {RegExp}
 * @private
 */
bluemind.html.Sanitizer.AMP_RE_ = /&/g;


/**
 * Regular expression that matches <.
 * @type {RegExp}
 * @private
 */
bluemind.html.Sanitizer.LT_RE_ = /</g;


/**
 * Regular expression that matches >.
 * @type {RegExp}
 * @private
 */
bluemind.html.Sanitizer.GT_RE_ = />/g;


/**
 * Regular expression that matches ".
 * @type {RegExp}
 * @private
 */
bluemind.html.Sanitizer.QUOTE_RE_ = /\"/g;


/**
 * Regular expression that matches =.
 * @type {RegExp}
 * @private
 */
bluemind.html.Sanitizer.EQUALS_RE_ = /=/g;

bluemind.html.Sanitizer.SAFE_SCHEME = [
  'http',
  'https',
  'ftp',
  'data',
  'callto',
  'tel',
  'mailto'
];

bluemind.html.Sanitizer.SAFE_STYLE = [
  'backgroundColor',
  'textAlign',
  'color'
];

/**
 * The string buffer that holds the sanitized version of the html. Used
 * during the parse time.
 * @type {goog.string.StringBuffer}
 * @private
 */
bluemind.html.Sanitizer.prototype.stringBuffer_;

/**
 * A stack that holds how the handler is being called.
 * @type {Array}
 * @private
 */
bluemind.html.Sanitizer.prototype.stack_;
/**
 * Whether we are ignoring what is being processed or not.
 * @type {boolean}
 * @private
 */
bluemind.html.Sanitizer.prototype.ignoring_;


/** @override */
bluemind.html.Sanitizer.prototype.startTag = function(tagName, attribs) {
  if (this.ignoring_) {
    return;
  }
  if (!goog.string.html.HtmlParser.Elements.hasOwnProperty(tagName)) {
    return;
  }
  var eflags = goog.string.html.HtmlParser.Elements[tagName];
  if (eflags & goog.string.html.HtmlParser.EFlags.FOLDABLE) {
    return;
  } else if (eflags & goog.string.html.HtmlParser.EFlags.UNSAFE) {
    this.ignoring_ = !(eflags & goog.string.html.HtmlParser.EFlags.EMPTY);
    return;
  }
  attribs = this.sanitizeAttributes_(tagName, attribs);
  if (attribs) {
    if (!(eflags & goog.string.html.HtmlParser.EFlags.EMPTY)) {
      this.stack_.push(tagName);
    }

    this.stringBuffer_.append('<', tagName);
    for (var i = 0, n = attribs.length; i < n; i += 2) {
      var attribName = attribs[i],
          value = attribs[i + 1];
      if (value !== null && value !== void 0) {
        this.stringBuffer_.append(' ', attribName, '="',
            this.escapeAttrib_(value), '"');
      }
    }
    this.stringBuffer_.append('>');
  }
};


/** @override */
bluemind.html.Sanitizer.prototype.endTag = function(tagName) {
  if (this.ignoring_) {
    this.ignoring_ = false;
    return;
  }
  if (!goog.string.html.HtmlParser.Elements.hasOwnProperty(tagName)) {
    return;
  }
  var eflags = goog.string.html.HtmlParser.Elements[tagName];
  if (!(eflags & (goog.string.html.HtmlParser.EFlags.UNSAFE |
      goog.string.html.HtmlParser.EFlags.EMPTY |
      goog.string.html.HtmlParser.EFlags.FOLDABLE))) {
    var index;
    if (eflags & goog.string.html.HtmlParser.EFlags.OPTIONAL_ENDTAG) {
      for (index = this.stack_.length; --index >= 0;) {
        var stackEl = this.stack_[index];
        if (stackEl === tagName) {
          break;
        }
        if (!(goog.string.html.HtmlParser.Elements[stackEl] &
            goog.string.html.HtmlParser.EFlags.OPTIONAL_ENDTAG)) {
          // Don't pop non optional end tags looking for a match.
          return;
        }
      }
    } else {
      for (index = this.stack_.length; --index >= 0;) {
        if (this.stack_[index] === tagName) {
          break;
        }
      }
    }
    if (index < 0) { return; }  // Not opened.
    for (var i = this.stack_.length; --i > index;) {
      var stackEl = this.stack_[i];
      if (!(goog.string.html.HtmlParser.Elements[stackEl] &
          goog.string.html.HtmlParser.EFlags.OPTIONAL_ENDTAG)) {
        this.stringBuffer_.append('</', stackEl, '>');
      }
    }
    this.stack_.length = index;
    this.stringBuffer_.append('</', tagName, '>');
  }
};


/** @override */
bluemind.html.Sanitizer.prototype.pcdata = function(text) {
  if (!this.ignoring_) {
    this.stringBuffer_.append(text);
  }
};

/** @override */
bluemind.html.Sanitizer.prototype.rcdata = function(text) {
  if (!this.ignoring_) {
    this.stringBuffer_.append(text);
  }
};

/** @override */
bluemind.html.Sanitizer.prototype.cdata = function(text) {
  if (!this.ignoring_) {
    this.stringBuffer_.append(text);
  }
};

/** @override */
bluemind.html.Sanitizer.prototype.startDoc = function() {
  this.stack_ = [];
  this.ignoring_ = false;
};

/** @override */
bluemind.html.Sanitizer.prototype.endDoc = function() {
  for (var i = this.stack_.length; --i >= 0;) {
    this.stringBuffer_.append('</', this.stack_[i], '>');
  }
  this.stack_.length = 0;
};

/**
 * Escapes HTML special characters in attribute values as HTML entities.
 *
 * @param {string} s The string to be escaped.
 * @return {string} An escaped version of {@code s}.
 * @private
 */
bluemind.html.Sanitizer.prototype.escapeAttrib_ = function(s) {
  // Escaping '=' defangs many UTF-7 and SGML short-tag attacks.
  return s.replace(bluemind.html.Sanitizer.AMP_RE_, '&amp;').
      replace(bluemind.html.Sanitizer.LT_RE_, '&lt;').
      replace(bluemind.html.Sanitizer.GT_RE_, '&gt;').
      replace(bluemind.html.Sanitizer.QUOTE_RE_, '&#34;').
      replace(bluemind.html.Sanitizer.EQUALS_RE_, '&#61;');
};


/**
 * Sanitizes attributes found on html entities.
 * @param {string} tagName The name of the tag in which the {@code attribs} were
 *     found.
 * @param {Array.<?string>} attribs An array of attributes.
 * @return {Array.<?string>} A sanitized version of the {@code attribs}.
 * @private
 */
bluemind.html.Sanitizer.prototype.sanitizeAttributes_ =
    function(tagName, attribs) {
  for (var i = 0; i < attribs.length; i += 2) {
    var attribName = attribs[i];
    var value = attribs[i + 1];
    var atype = null, attribKey;
    if ((attribKey = tagName + '::' + attribName,
        goog.string.html.HtmlSanitizer.Attributes.hasOwnProperty(attribKey)) ||
        (attribKey = '*::' + attribName,
        goog.string.html.HtmlSanitizer.Attributes.hasOwnProperty(attribKey))) {
      atype = goog.string.html.HtmlSanitizer.Attributes[attribKey];
    }
    if (atype !== null) {
      switch (atype) {
        case 0: break;
        case goog.string.html.HtmlSanitizer.AttributeType.SCRIPT:
          value = null;
          break;
        case goog.string.html.HtmlSanitizer.AttributeType.STYLE:
          value = this.sanitizeStyles_(/** @type {string} */ (value));
          break;
        case goog.string.html.HtmlSanitizer.AttributeType.ID:
        case goog.string.html.HtmlSanitizer.AttributeType.IDREF:
        case goog.string.html.HtmlSanitizer.AttributeType.IDREFS:
        case goog.string.html.HtmlSanitizer.AttributeType.GLOBAL_NAME:
        case goog.string.html.HtmlSanitizer.AttributeType.LOCAL_NAME:
        case goog.string.html.HtmlSanitizer.AttributeType.CLASSES:
          break;
        case goog.string.html.HtmlSanitizer.AttributeType.URI:
          value = this.sanitizeURI_(/** @type {string} */ (value));
          break;
        case goog.string.html.HtmlSanitizer.AttributeType.URI_FRAGMENT:
          if (value && '#' === value.charAt(0)) {
            value = '#' + value;
          } else {
            value = null;
          }
          break;
        default:
          value = null;
          break;
      }
    } else {
      value = null;
    }
    attribs[i + 1] = value;
  }
  return attribs;
};

/**
 * Sanitizes URI found on html entities.
 * @param {string} uri URI to sanitize.
 * @return {?string} A sanitized version of the {@code uri}.
 * @private
 */
bluemind.html.Sanitizer.prototype.sanitizeURI_ = function(uri) {
  var scheme = goog.uri.utils.getScheme(uri) || 'invalid';
  if (goog.array.contains(bluemind.html.Sanitizer.SAFE_SCHEME, scheme.toLowerCase())) {
    if (scheme.toLowerCase() == 'data') {
      if (!goog.string.startsWith(uri, 'data:image/') || (uri.length > (1024 * 1024 * 10 * 1.37))) {
        return null;
      }
    }
    return uri;
  }
  return null;
};


/**
 * Sanitizes styles founded on html entities.
 * @param {string} styles Styles string to sanitize.
 * @return {?string} A sanitized version of the {@code uri}.
 * @private
 */
bluemind.html.Sanitizer.prototype.sanitizeStyles_ = function(styles) {
  var css = goog.style.parseStyleAttribute(styles);
  var valid = {};
  for ( var i = 0; i < bluemind.html.Sanitizer.SAFE_STYLE.length; i++) {
    var key = bluemind.html.Sanitizer.SAFE_STYLE[i];
    if (css[key]) {
      valid[key] = css[key];
    }
  }
  var value = goog.style.toStyleAttribute(valid);
  if (value.length > 0) {
    return value;
  }
  return null;
};
