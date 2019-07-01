// FIXME no licence
// FIXME wrong package
goog.provide('net.bluemind.ui.banner.widget.UnseenEvents');
goog.require('goog.Timer');
goog.require('goog.dom');
goog.require('goog.dom.classlist');
goog.require('goog.events');
goog.require('goog.ui.Component.EventType');
goog.require('goog.events.EventHandler');
goog.require('net.bluemind.calendar.api.CalendarClient');
goog.require('net.bluemind.ui.PendingActions');
goog.require('net.bluemind.date.DateTime');

function unseenEventsCreate() {
  if (goog.global['bmcSessionInfos']['domain'] == 'global.virt') {
    return;
  }
  var loc = goog.global['location'];
  var url = loc.href.split('#')[0];
  url += '#/pending/';
  var pendingActions = new net.bluemind.ui.PendingActions(url);
  pendingActions.render();
  var element = pendingActions.getElement();

  startTimer_(pendingActions);
  return element;
}

function startTimer_(element) {
  var timer = new goog.Timer(30000);
  timer.start();
  goog.events.listen(timer, goog.Timer.TICK, function() {
    loadUnseenEventCount_(element);
  });
  loadUnseenEventCount_(element);
}

function loadUnseenEventCount_(pendingActions) {
  var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid']
  }));

  var domain = goog.global['bmcSessionInfos']['login'].split("@")[1];

  var calendarClient = new net.bluemind.calendar.api.CalendarClient(rpc, '', 'calendar:Default:'
      + goog.global['bmcSessionInfos']['userId']);
  var today = new net.bluemind.date.DateTime();
  var query = {
    'size' : 0,
    'dateMin' : new net.bluemind.date.DateHelper().toBMDateTime(today),
    'attendee' : {
      'dir' : 'bm://' + goog.global['bmcSessionInfos']['domain'] + '/users/' + goog.global['bmcSessionInfos']['userId'],
      'partStatus' : 'NeedsAction'
    }
  }

  calendarClient.search(query).then(function(res) {

    if (res.total == 0) {
      /** @meaning calendar.banner.noPendingEvents */
      var MSG_ZERO = goog.getMsg('No pending events here!');

      pendingActions.setModel({
        count : res.total,
        title : MSG_ZERO
      });
    } else {

      /** @meaning calendar.banner.pendingEvents */
      var MSG_MANY = goog.getMsg('{$count} pending events.', {
        count : res.total
      });
      pendingActions.setModel({
        count : res.total,
        title : MSG_MANY
      });
    }
  });
}

goog.global['unseenEventsCreate'] = unseenEventsCreate;
