goog.provide("net.bluemind.addressbook.api.i18n.Email.Caption");
/** @meaning contact.vcard.email.work */
net.bluemind.addressbook.api.i18n.Email.MSG_WORK = goog.getMsg('Work email');
/** @meaning contact.vcard.email.home */
net.bluemind.addressbook.api.i18n.Email.MSG_HOME = goog.getMsg('Home email');
/** @meaning contact.vcard.email.other */
net.bluemind.addressbook.api.i18n.Email.MSG_OTHER = goog.getMsg('Other email');

/**
 * @enum {object}
 */
net.bluemind.addressbook.api.i18n.Email.Caption = {
  ALL : {
    'work' : net.bluemind.addressbook.api.i18n.Email.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.Email.MSG_HOME,
    'other' : net.bluemind.addressbook.api.i18n.Email.MSG_OTHER
  },
  STANDARD : {
    'work' : net.bluemind.addressbook.api.i18n.Email.MSG_WORK,
    'home' : net.bluemind.addressbook.api.i18n.Email.MSG_HOME
  },
  FALLBACK : net.bluemind.addressbook.api.i18n.Email.MSG_OTHER
}
