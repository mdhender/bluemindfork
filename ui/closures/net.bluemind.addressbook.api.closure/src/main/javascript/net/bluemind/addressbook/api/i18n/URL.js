goog.provide("net.bluemind.addressbook.api.i18n.URL.Caption");
/** @meaning contact.vcard.url.work */
net.bluemind.addressbook.api.i18n.URL.MSG_WORK = goog.getMsg('Work url');
/** @meaning contact.vcard.url.url */
net.bluemind.addressbook.api.i18n.URL.MSG_HOME = goog.getMsg('Home url');
/** @meaning contact.vcard.url.other */
net.bluemind.addressbook.api.i18n.URL.MSG_OTHER = goog.getMsg('Other url');

/**
 * @enum {object | string}
 */
net.bluemind.addressbook.api.i18n.URL.Caption = {
  ALL : {
    'work' : net.bluemind.addressbook.api.i18n.URL.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.URL.MSG_HOME,
    'other' : net.bluemind.addressbook.api.i18n.URL.MSG_OTHER
  },
  STANDARD : {
    'work' : net.bluemind.addressbook.api.i18n.URL.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.URL.MSG_HOME
  },
  FALLBACK : net.bluemind.addressbook.api.i18n.URL.MSG_OTHER
}
