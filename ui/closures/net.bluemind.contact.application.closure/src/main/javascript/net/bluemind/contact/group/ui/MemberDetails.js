goog.provide("net.bluemind.contact.group.ui.MemberDetails");
goog.provide("net.bluemind.contact.group.ui.MemberDetails.EventType");

goog.require("goog.events");
goog.require("goog.events.Event");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component");
goog.require("goog.ui.FlatButtonRenderer");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.contact.group.templates");

/**
 * @constructor
 *
 * @param {Object=} opt_model
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.contact.group.ui.MemberDetails = function(opt_model, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  var button = new goog.ui.Button(this.getDomHelper().createDom('div',
		  goog.getCssName('fa') + ' ' + goog.getCssName('fa-external-link')),
      goog.ui.FlatButtonRenderer.getInstance(), this.getDomHelper());
  button.setId('goto');
  /** @meaning contact.showDetails */
  var MSG_CONSULT_DETAILS = goog.getMsg('Show details');
  button.setTooltip(MSG_CONSULT_DETAILS);
  this.addChild(button);
  if (opt_model) {
    this.setModel(opt_model);
  }
}
goog.inherits(net.bluemind.contact.group.ui.MemberDetails, goog.ui.Component);

/** @override */
net.bluemind.contact.group.ui.MemberDetails.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('goto'), goog.ui.Component.EventType.ACTION, function(e) {
    e.stopPropagation();
    e = new goog.events.Event(net.bluemind.contact.group.ui.MemberDetails.EventType.GOTO);
    e.model = this.getModel();
    this.dispatchEvent(e);
  });
};

/** @override */
net.bluemind.contact.group.ui.MemberDetails.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  if (this.isInDocument()) {
    if (this.getChild('goto').isInDocument()) {
      this.getChild('goto').exitDocument();
    }
    this.getElement().innerHTML = net.bluemind.contact.group.templates.member({
      member : this.getModel()
    });
    this.getChild('goto').render(this.getElementByClass(goog.getCssName('member-goto')));
  }
};

/** @override */
net.bluemind.contact.group.ui.MemberDetails.prototype.createDom = function() {
  goog.base(this, 'createDom');
  if (this.getModel()) {
    if (this.getChild('goto').isInDocument()) {
      this.getChild('goto').exitDocument();
    }
    this.getElement().innerHTML = net.bluemind.contact.group.templates.member({
      member : this.getModel()
    });
    this.getChild('goto').render(this.getElementByClass(goog.getCssName('member-goto')));
  }
};

/**
 * @enum
 *
 */
net.bluemind.contact.group.ui.MemberDetails.EventType = {
  GOTO : goog.events.getUniqueId('goto')
};
