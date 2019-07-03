goog.provide('net.bluemind.ui.banner.widget.UnseeMails')
goog.require('goog.Timer');
goog.require('goog.dom');
goog.require('goog.dom.classlist');
goog.require('goog.events');
goog.require('net.bluemind.mailbox.api.MailboxesClient');
goog.require("net.bluemind.container.service.ContainerObserver");
goog.require("net.bluemind.container.service.ContainersObserver");

net.bluemind.ui.banner.widget.UnseeMails.loadUnseenMessageCount_ = function(element) {
  	var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
	      'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid']
	}));

	var domain = goog.global['bmcSessionInfos']['login'].split("@")[1];
	var mailboxClient = new net.bluemind.mailbox.api.MailboxesClient(rpc, '', domain);
	mailboxClient.getUnreadMessagesCount().then(function(count){
	  net.bluemind.ui.banner.widget.UnseeMails.display(element, count);
	});
}

net.bluemind.ui.banner.widget.UnseeMails.display = function(element, count){
	if (count > 0){
		element.innerHTML = "&nbsp;<span class='unread-mails unread-mails-bg-present'>"+count+"</span>&nbsp;";
	} else{
		element.innerHTML = "&nbsp;<span class='unread-mails unread-mails-bg-empty'>"+count+"</span>&nbsp;";
	}
}

net.bluemind.ui.banner.widget.UnseeMails.unseenMailsCreate = function() {
  if (goog.global['bmcSessionInfos']['domain'] == 'global.virt'){
      return;
  } 
  var element = goog.dom.createElement('span');
  net.bluemind.ui.banner.widget.UnseeMails.display(element, "");
  var handler = new goog.events.EventHandler(this);
  var obs = new net.bluemind.container.service.ContainersObserver();
  handler.listen(obs, net.bluemind.container.service.ContainersObserver.EventType.CHANGE, function() {
      net.bluemind.ui.banner.widget.UnseeMails.loadUnseenMessageCount_(element);
  });
  obs.observerContainers('mailbox', [goog.global['bmcSessionInfos']['userId'] ]);
  net.bluemind.ui.banner.widget.UnseeMails.loadUnseenMessageCount_(element);
  return element;
}

goog.global['unseenMailsCreate'] = net.bluemind.ui.banner.widget.UnseeMails.unseenMailsCreate;
