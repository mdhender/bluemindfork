goog.provide("net.bluemind.addressbook.api.i18n.Address.Caption");
/** @meaning contact.vcard.address.work */
net.bluemind.addressbook.api.i18n.Address.MSG_WORK = goog.getMsg('Work address');
/** @meaning contact.vcard.address.home */
net.bluemind.addressbook.api.i18n.Address.MSG_HOME = goog.getMsg('Home address');
/** @meaning contact.vcard.address.other */
net.bluemind.addressbook.api.i18n.Address.MSG_OTHER = goog.getMsg('Other address');

/**
 * @enum {object}
 */
net.bluemind.addressbook.api.i18n.Address.Caption = {
  ALL : {
    'work' : net.bluemind.addressbook.api.i18n.Address.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.Address.MSG_HOME,
    'other' : net.bluemind.addressbook.api.i18n.Address.MSG_OTHER
  },
  STANDARD : {
    'work' : net.bluemind.addressbook.api.i18n.Address.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.Address.MSG_HOME
  },
  FALLBACK : net.bluemind.addressbook.api.i18n.Address.MSG_OTHER
}
