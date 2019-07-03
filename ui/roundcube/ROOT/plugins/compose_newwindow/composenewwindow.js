/**
 * compose_newwindow - Compose(Reply/Forward) in a New Window
 *
 * @version 3.00 (20110822)
 * @author Karl McMurdo (user xrxca on roundcubeforum.net)
 * @url http://github.com/xrxca/cnw
 * @copyright (c) 2010-2011 Karl McMurdo
 *
 */ 

$(document).ready(function() {
  if (window.rcmail) {
    rcmail.addEventListener('init', function(evt) {
      rcmail.register_command('plugin.composenewwindow', composenewwindow, true);
      rcmail.register_command('plugin.editnewwindow', editnewwindow, true);
      rcmail.register_command('plugin.replynewwindow', replynewwindow, true); 
      rcmail.register_command('plugin.reply-allnewwindow', replyallnewwindow, true);
      rcmail.register_command('plugin.reply-listnewwindow', replylistnewwindow, true);
      rcmail.register_command('plugin.composenewwindow', composenewwindow, true); 
      rcmail.register_command('plugin.forwardnewwindow', forwardnewwindow, true);
      rcmail.register_command('plugin.forward-attachmentnewwindow', forwardattachmentnewwindow, true);
      rcmail.register_command('plugin.abookcomposenewwindow', abookcomposenewwindow, true); 
      rcmail.addEventListener('plugin.composenewwindow_draftsaved', refreshdraft);
      rcmail.addEventListener('plugin.composenewwindow_abooksend', abookcomposecallback);
    });  

    rcmail.msglist_dbl_click = function(list) {
      if (this.preview_timer)
          clearTimeout(this.preview_timer);
      
      if (this.preview_read_timer)
          clearTimeout(this.preview_read_timer);
      
      var uid = list.get_single_selection();
      if (uid && this.env.mailbox == this.env.drafts_mailbox)
          rcmail.env.composenewwindow = opencomposewindow(rcmail.env.comm_path+'&_action=compose&_draft_uid='+uid+'&_mbox='+urlencode(this.env.mailbox));
      else if (uid)
          this.show_message(uid, false, false);
    };

    rcmail.origCommand = rcmail.command;
    rcmail.command = function(command, props, obj) {
      if( command == 'edit' && this.task=='mail' && (cid = this.get_single_uid())) {
          url = (this.env.mailbox == this.env.drafts_mailbox) ? '_draft_uid=' : '_uid=';
          rcmail.env.composenewwindow = opencomposewindow(rcmail.env.comm_path+'&_action=compose&'+url+cid+'&_mbox='+urlencode(this.env.mailbox));
          return(false);
      }else {
        return(this.origCommand(command, props, obj));
      }
    };
  }
});

function composenewwindow(emailaddr) {
    if (!rcmail) return(true);
    var url=rcmail.env.comm_path+"&_action=compose"; 
    url = rcmail.get_task_url('mail', url)+'&_to='+urlencode(emailaddr);
    rcmail.env.composenewwindow = opencomposewindow(url);
    return(false);
}

function refreshdraft() {
  if (self.window.name == 'rc_compose_child' && window.opener && window.opener.rcmail) {
    if (window.opener.rcmail.env.mailbox == 'Drafts') {
      window.opener.rcmail.command('checkmail','');
    }
  }
}

function editnewwindow(emailaddr) {
    if (!rcmail) return(true);
    var url=rcmail.env.comm_path+"&_action=compose"; 
    uid = (rcmail.env.mailbox == rcmail.env.drafts_mailbox) ? '_draft_uid=' : '_uid=';
    if (typeof rcmail.message_list === "undefined") {
        messageId = getUrlParam("_uid");
    }else {
        messageId = rcmail.message_list.get_selection();
    }
    url = url+"&"+uid+messageId;
    rcmail.env.composenewwindow = opencomposewindow(url);
    return(false);
}

function replynewwindow(i) {
    if (!rcmail) return(true);
    var uid;
    if(uid=newwindow_contextuid(i)) {
        var url=rcmail.env.comm_path+"&_action=compose&_mbox="+urlencode(rcmail.env.mailbox)+"&_reply_uid="+uid;
        rcmail.env.composenewwindow = opencomposewindow(url);
    }
    return(false);
}

function replyallnewwindow(i) {
    if (!rcmail) return(true);
    var uid;
    if(uid=newwindow_contextuid(i)) {
        var url=rcmail.env.comm_path+"&_action=compose&_mbox="+urlencode(rcmail.env.mailbox)+"&_reply_uid="+uid+"&_all=1";
        rcmail.env.composenewwindow = opencomposewindow(url);
    }
    return(false);
}

function replylistnewwindow(i) {
    if (!rcmail) return(true);
    var uid;
    if(uid=newwindow_contextuid(i)) {
        var url=rcmail.env.comm_path+"&_action=compose&_mbox="+urlencode(rcmail.env.mailbox)+"&_reply_uid="+uid+"&_all=list";
        rcmail.env.composenewwindow = opencomposewindow(url);
    }
    return(false);
}

function forwardnewwindow(i) {
    if (!rcmail) return(true);
    var uid;
    if(uid=newwindow_contextuid(i)) {
        var url=rcmail.env.comm_path+"&_action=compose&_mbox="+urlencode(rcmail.env.mailbox)+"&_forward_uid="+uid;
        if (i != 'sub' && i != 'context' &&  rcmail.env.forward_attachment) {
	    url += '&_attachment=1';
	}
        rcmail.env.composenewwindow = opencomposewindow(url);
    }
    if ( i == 'sub' ) rcmail_ui.show_popupmenu('forwardmenu',false);
    return(false);
}

// For new builtin forward attachment
function forwardattachmentnewwindow(i) {
    if (!rcmail) return(true);
    var uid;
    if(uid=newwindow_contextuid(i)) {
        var url=rcmail.env.comm_path+"&_action=compose&_mbox="+urlencode(rcmail.env.mailbox)+"&_forward_uid="+uid;
        url += '&_attachment=1';
        rcmail.env.composenewwindow = opencomposewindow(url);
    }
    if ( i == 'sub' ) rcmail_ui.show_popupmenu('forwardmenu',false);
    return(false);
}

// for forwardattachment plugin
function forwardattnewwindow(id) {
    if (!rcmail) return(true);
    var url=rcmail.env.comm_path+"&_action=compose"; 
    url = rcmail.get_task_url('mail', url)+'&_id='+id;
    rcmail.env.composenewwindow = opencomposewindow(url);
    return(false);
}

function sendinvitationnewwindow(id, edit) {
    if (!rcmail) return(true);
    var url=rcmail.env.comm_path+"&_action=compose"; 
    url = rcmail.get_task_url('mail', url)+'&_attachics=1&_eid='+id + '&_edit='+edit;
    rcmail.env.composenewwindow = opencomposewindow(url);
    return(false);
}

function abookcomposenewwindow(i) {
	if (!rcmail) return(true);
	if(!rcmail.contact_list) return(true);
	var prev_sel = null;
	var prev_cid = rcmail.env.cid;
	if (i == 'context' ) {
		if (rcmail.env.cid) {
			if (!rcmail.contact_list.in_selection(rcmail.env.cid)) {
				prev_sel = rcmail.contact_list.get_selection();
				rcmail.contact_list.select(rcmail.env.cid);
			} else if (rcmail.contact_list.get_single_selection() == rcmail.env.cid) {
				rcmail.env.cid = null;
			} else {
				prev_sel = rcmail.contact_list.get_selection();
				rcmail.contact_list.select(rcmail.env.cid);
			}
		}
	}

	var selection = rcmail.contact_list.get_selection();
	if(selection.length) {
		rcmail.http_request('plugin.composenewwindow_abooksend', '_cid='+urlencode(selection.join(','))+'&_source='+urlencode(rcmail.env.source), true);
	}

	if (prev_sel) {
		rcmail.contact_list.clear_selection();
		for (var i in prev_sel)
			rcmail.contact_list.select_row(prev_sel[i],CONTROL_KEY);
	}
	rcmail.env.cid = prev_cid;
	return(false);
}

function abookcomposecallback(response) {
    var url = rcmail.env.comm_path+'&_action=compose';
    url = rcmail.get_task_url('mail', url)+'&_mailto='+response;
    rcmail.env.composenewwindow = opencomposewindow(url);
}

function opencomposewindow(url) {
    var w = 1100;
    var h = 600;
    var sl = screen.left != undefined ? screen.left : window.screenLeft;
    var st = screen.top != undefined ? screen.top : window.screenTop;

    var width = screen.width;
    var height = screen.height;

    var l = ((width / 2) - (w / 2)) + sl;
    var t = ((height / 2) - (h / 2)) + st;
    var childwin = window.open(url,'','width='+w+',height='+h+',top='+t+',left='+l);

    childwin.focus();
    // Give the child window a name so we can close it later
    childwin.name = 'rc_compose_child';
    return childwin;
}

function newwindow_contextuid(orig) {
    if ( orig == 'context' && $('#messagelist tbody tr.contextRow') && String($('#messagelist tbody tr.contextRow').attr('id')).match(/rcmrow([a-z0-9\-_=]+)/i))
       return RegExp.$1;
    return rcmail.get_single_uid();
}

function getUrlParam(name){
    var results = new RegExp('[\\?&amp;]' + name + '=([^&amp;#]*)').exec(window.location.href);
    return results[1] || 0;
}
