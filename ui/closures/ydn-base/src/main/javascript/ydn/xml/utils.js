/**
 * @fileoverview XML Utils.
 * User: mbikt
 * Date: 7/12/12
 */

goog.provide('ydn.xml.utils');
goog.require('goog.dom.xml');


/**
 * Convert from XML to JSON format
 *
 * @param {Element|Document|Node|string} xml XML data.
 * @param {string=} opt_root_xmlns root xmlns name space.
 * @return {Object|*} JSON object.
 */
ydn.xml.utils.xml2json = function(xml, opt_root_xmlns) {
  // this code is based on http://davidwalsh.name/convert-xml-json

  // see http://code.google.com/apis/gdata/docs/json.html for Google
  // specification about conversion
  var is_atom_format = opt_root_xmlns == 'atom';
  var is_plain = !opt_root_xmlns;

  /**
   * If an element has a namespace alias, the alias and element are concatenated
   * using "$". For example, ns:element becomes ns$element.
   * atom: is stripped.
   * @param {string} namespace
   * @return {string}
   */
  var ns2name = function(namespace) {
    if (is_atom_format && goog.string.startsWith(namespace, 'atom:')) {
      namespace = namespace.substring(5, namespace.length);
    }
    return namespace.replace('#text', '$t').replace(/:/g, '$');
  };

  // Create XML document object
  if (goog.isString(xml)) {
    xml = goog.dom.xml.loadXml(xml);
  }

  // Create the return object
  var obj = {};

  if (xml.nodeType == 1) { // element
    // do attributes
    if (!is_plain && xml.attributes.length > 0) {
      for (var j = 0, n = xml.attributes.length; j < n; j++) {
        var attribute = xml.attributes.item(j);
        obj[ns2name(attribute.nodeName)] = attribute.nodeValue;
      }
    }
  } else if (xml.nodeType == 3) { // text
    obj = xml.nodeValue;
  }

  // do children
  if (xml.hasChildNodes()) {
    for (var i = 0, m = xml.childNodes.length; i < m; i++) {
      var item = xml.childNodes.item(i);
      var nodeName = ns2name(item.nodeName);
      if (is_plain && nodeName == '$t') {
        obj = item.nodeValue;
      } else if (!goog.isDef(obj[nodeName])) {
        obj[nodeName] = ydn.xml.utils.xml2json(item, opt_root_xmlns);
      } else {
        if (!goog.isArray(obj[nodeName])) {
          var old = obj[nodeName];
          obj[nodeName] = [];
          obj[nodeName].push(old);
        }
        obj[nodeName].push(ydn.xml.utils.xml2json(item, opt_root_xmlns));  // ?
      }
    }
  }
  return obj;
};



