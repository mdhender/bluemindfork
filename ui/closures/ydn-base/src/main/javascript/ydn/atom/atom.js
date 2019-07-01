/**
 * @fileoverview Atom entry.
 *
 * @suppress {invalidCasts}
 *
 * http://tools.ietf.org/html/rfc5023
 * http://www.atomenabled.org/developers/syndication/
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */

goog.provide('ydn.atom.Atom');
goog.require('goog.log');
goog.require('ydn.atom.Link');
goog.require('ydn.error');



/**
 *
 * @param {!Atom} data
 * @constructor
 * @struct
 */
ydn.atom.Atom = function(data) {
  /**
   * @protected
   * @final
   * @type {!Atom}
   */
  this.data = data;
};


/**
 *
 * @param {string} text
 * @return {!AtomText}
 */
ydn.atom.Atom.text = function(text) {
  var atom_text = {'$t': text};
  return /** @type {!AtomText} */ (atom_text);
};


/**
 *
 * @param {string} text
 * @param {string=} opt_type
 * @return {!AtomContent}
 */
ydn.atom.Atom.content = function(text, opt_type) {
  var content = {
    '$t': text
  };
  if (opt_type) {
    content['type'] = opt_type;
  }
  return /** @type {!AtomContent} */ (content);
};


/**
 * @const
 * @type {string}
 */
ydn.atom.Atom.TYPE = 'ydn.atom.Atom';


/**
 *
 * @return {string}
 */
ydn.atom.Atom.prototype.getId = function() {
  return this.data.id.$t;
};


/**
 *
 * @return {string}
 */
ydn.atom.Atom.prototype.getTitle = function() {
  var title = this.data.title ? this.data.title.$t : undefined;
  return /** @type {string} */ (title); // title is always defined.
};


/**
 *
 * @return {string}
 */
ydn.atom.Atom.prototype.getUpdated = function() {
  return this.data.updated.$t;
};


/**
 *
 * @return {!Date}
 */
ydn.atom.Atom.prototype.getUpdatedDate = function() {
  return new Date(this.getUpdated());
};


/**
 *
 * @param {ydn.atom.Link.Rel} rel
 * @return {AtomLink}
 */
ydn.atom.Atom.prototype.getLink = function(rel) {
  return ydn.atom.Link.getLink(this.data, rel);
};


/**
 * Return href attribute of ydn.atom.Link.Rel.EDIT of links.
 *
 * Subclass should override for the case Link do not have EDIT.
 * @final
 * @return {string|undefined}
 */
ydn.atom.Atom.prototype.getLinkEdit = function() {
  var link = this.getLink(ydn.atom.Link.Rel.EDIT);
  return link ? link.href : undefined;
};


/**
 * Return href attribute of ydn.atom.Link.Rel.SELF of links.
 *
 * Subclass should override for the case Link do not have EDIT.
 * @final
 * @return {string|undefined}
 */
ydn.atom.Atom.prototype.getLinkSelf = function() {
  var link = this.getLink(ydn.atom.Link.Rel.SELF);
  return link ? link.href : undefined;
};


/**
 * Note: batch url include gdata version number.
 * @final
 * @return {string|undefined}
 */
ydn.atom.Atom.prototype.getLinkBatch = function() {
  var link = this.getLink(ydn.atom.Link.Rel.BATCH);
  return link ? link.href : '';
};


/**
 * Get type attribute of content element.
 * type attribute ends in +xml or /xml, then an xml document of this type is contained inline.
 * type attribute starts with text, then an escaped document of this type is contained inline.
 * @return {string|undefined}
 */
ydn.atom.Atom.prototype.getContentType = function() {
  return this.data.content ? this.data.content.type : undefined;
};


/**
 *
 * @return {AtomContent}
 */
ydn.atom.Atom.prototype.getContent = function() {
  return this.data.content || null;
};
