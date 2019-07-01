goog.provide("net.bluemind.addressbook.api.i18n.Tel.Caption");
/** @meaning contact.vcard.phone.work */
net.bluemind.addressbook.api.i18n.Tel.MSG_WORKVOICE = goog.getMsg('Work phone');
/** @meaning contact.vcard.phone.home */
net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEVOICE = goog.getMsg('Home phone');
/** @meaning contact.vcard.phone.mobile */
net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE = goog.getMsg('Mobile phone');
/** @meaning contact.vcard.fax.work */
net.bluemind.addressbook.api.i18n.Tel.MSG_WORKFAX = goog.getMsg('Work fax');
/** @meaning contact.vcard.fax.home */
net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEFAX = goog.getMsg('Home fax');
/** @meaning contact.vcard.phone.other */
net.bluemind.addressbook.api.i18n.Tel.MSG_OTHER = goog.getMsg('Other phone');
/** @meaning contact.vcard.phone.assistant */
net.bluemind.addressbook.api.i18n.Tel.MSG_VOICEASSISTANT = goog.getMsg('Assistant phone');
/** @meaning contact.vcard.phone.company */
net.bluemind.addressbook.api.i18n.Tel.MSG_VOICECOMPANY = goog.getMsg('Company phone');
/** @meaning contact.vcard.phone.car */
net.bluemind.addressbook.api.i18n.Tel.MSG_CARVOICE = goog.getMsg('Car phone');
/** @meaning contact.vcard.phone.pager */
net.bluemind.addressbook.api.i18n.Tel.MSG_PAGERVOICE = goog.getMsg('Pager phone');
/** @meaning contact.vcard.tlx */
net.bluemind.addressbook.api.i18n.Tel.MSG_TLX = goog.getMsg('TLX');
/** @meaning contact.vcard.telex */
net.bluemind.addressbook.api.i18n.Tel.MSG_TTYTDD = goog.getMsg('Telex');
/** @meaning contact.vcard.tty_tdd */
net.bluemind.addressbook.api.i18n.Tel.MSG_ISDN = goog.getMsg('TTY/TDD');
/** @meaning contact.vcard.isdn */
net.bluemind.addressbook.api.i18n.Tel.MSG_ISDN = goog.getMsg('ISDN');
/** @meaning contact.vcard.phone.callback */
net.bluemind.addressbook.api.i18n.Tel.MSG_VOICECALLBACK = goog.getMsg('Callback phone');
/**
 * @enum {Object}
 */
net.bluemind.addressbook.api.i18n.Tel.Caption = {
  ALL : {
	'work' : net.bluemind.addressbook.api.i18n.Tel.MSG_WORKVOICE,
    'voice,work' : net.bluemind.addressbook.api.i18n.Tel.MSG_WORKVOICE,
    'home' : net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEVOICE,
    'home,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEVOICE,
    'cell' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'cell,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'cell,work' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'cell,home' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'fax,work' : net.bluemind.addressbook.api.i18n.Tel.MSG_WORKFAX,
    'fax,home' : net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEFAX,
    'voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_OTHER,
    'assistant,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_VOICEASSISTANT,
    'company,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_VOICECOMPANY,
    'car,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_CARVOICE,
    'pager,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_PAGERVOICE,
    'tlx' : net.bluemind.addressbook.api.i18n.Tel.MSG_TLX,
    'ttytdd' : net.bluemind.addressbook.api.i18n.Tel.MSG_TTYTDD,
    'isdn' : net.bluemind.addressbook.api.i18n.Tel.MSG_ISDN,
    'voice,callback' : net.bluemind.addressbook.api.i18n.Tel.MSG_VOICECALLBACK

  },
  STANDARD : {
    'voice,work' : net.bluemind.addressbook.api.i18n.Tel.MSG_WORKVOICE,
    'work' : net.bluemind.addressbook.api.i18n.Tel.MSG_WORKVOICE,
    'home,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEVOICE,
    'home' : net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEVOICE,
    'cell,voice' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'cell' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'cell,work' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'cell,home' : net.bluemind.addressbook.api.i18n.Tel.MSG_CELLVOICE,
    'fax,work' : net.bluemind.addressbook.api.i18n.Tel.MSG_WORKFAX,
    'fax,home' : net.bluemind.addressbook.api.i18n.Tel.MSG_HOMEFAX
  },
  FALLBACK : net.bluemind.addressbook.api.i18n.Tel.MSG_OTHER

}
