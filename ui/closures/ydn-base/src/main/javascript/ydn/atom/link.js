/**
 * Created with IntelliJ IDEA.
 * User: mbikt
 * Date: 9/21/12
 * Time: 1:05 PM
 * To change this template use File | Settings | File Templates.
 */

goog.provide('ydn.atom.Link');


/**
 *
 * @constructor
 */
ydn.atom.Link = function() {

};


/**
 *  Link relation type.
 * @enum {string}
 */
ydn.atom.Link.Rel = {
  /**
   * Link that provides the URI of an alternate format of the entry's or feed's
   * contents. The type attribute of the link specifies a media type.
   */
  ALTERNATE: 'alternate',
  /**
   * Link that provides the URI that can be used to edit the entry. This
   * relation does not exist if the entry is read-only.
   */
  EDIT: 'edit',
  /**
   * Link that provides the URI of next page in a paged feed.
   */
  NEXT: 'next',
  PREVIOUS: 'previous',
  RELATED: 'related',
  /**
   * Link that provides the URI of the feed or entry. If this relation appears
   * on a feed that is the result of performing a query, then this URI includes
   * the same query parameters (or at least querying this URI produces the same
   * result as querying with those parameters).
   */
  SELF: 'self',
  // extension for GData
  FEED: 'http://schemas.google.com/g/2005#feed',
  LIST_FEED: 'http://schemas.google.com/spreadsheets/2006#listfeed',
  CELLS_FEED: 'http://schemas.google.com/spreadsheets/2006#cellsfeed',
  BATCH: "http://schemas.google.com/g/2005#batch",
  REVISION: "http://schemas.google.com/sites/2008#revision"
};


/**
 *
 * @param {Atom} data
 * @param {ydn.atom.Link.Rel} rel
 * @return {AtomLink}
 */
ydn.atom.Link.getLink = function (data, rel) {
  if (!data.link) {
    return null;
  } else if (goog.isArray(data.link)) {
    for (var i = 0; i < data.link.length; i++) {
      if (data.link[i].rel == rel) {
        return data.link[i];
      }
    }
    return null;
  } else {
    return data.link.rel == rel ? data.link : null;
  }
};