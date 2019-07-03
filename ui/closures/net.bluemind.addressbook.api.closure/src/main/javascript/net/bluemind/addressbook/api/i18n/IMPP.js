goog.provide("net.bluemind.addressbook.api.i18n.IMPP.Caption");
/** @meaning contact.vcard.messaging.work */
net.bluemind.addressbook.api.i18n.IMPP.MSG_WORK = goog.getMsg('Work messaging');
/** @meaning contact.vcard.messaging.home */
net.bluemind.addressbook.api.i18n.IMPP.MSG_HOME = goog.getMsg('Home messaging');
/** @meaning contact.vcard.messaging.other */
net.bluemind.addressbook.api.i18n.IMPP.MSG_OTHER = goog.getMsg('Other messaging');

/**
 * @enum {object}
 */
net.bluemind.addressbook.api.i18n.IMPP.Caption = {
  ALL : {
    'work' : net.bluemind.addressbook.api.i18n.IMPP.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.IMPP.MSG_HOME,
    'other' : net.bluemind.addressbook.api.i18n.IMPP.MSG_OTHER
  },
  STANDARD : {
    'work' : net.bluemind.addressbook.api.i18n.IMPP.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.IMPP.MSG_HOME
  },
  FALLBACK : net.bluemind.addressbook.api.i18n.IMPP.MSG_OTHER
}
