
/**********************************************************/
/*      Detached file list (links)                        */
/**********************************************************/

var ATTACHMENT_IMG = 'skins/bluemind/images/attachment.png';
var BLUEMIND_IMG = 'skins/bluemind/images/bluemind16.png';

/** Remove from detached files list */
rcube_webmail.prototype.remove_detachment_list = function(opt_is_html) {
  var is_html = opt_is_html === undefined ? ($("input[name='_is_html']").val() == '1') : opt_is_html;
  if (!is_html) {
    this.remove_txt_detachment_list();
  } else {
    this.remove_html_detachment_list();
  }
};

/** draw detached files list from scratch */
rcube_webmail.prototype.draw_detachment_list = function(opt_is_html) {
  var attachments = [];
  for(id in this.env.attachments) {  
    var att = this.env.attachments[id];
    if (this.is_detached_attachment(att)) {
      attachments.push(att);
    }
  }  
  var is_html = opt_is_html === undefined ? ($("input[name='_is_html']").val() == '1') : opt_is_html;
  if (!is_html) {
    if (attachments.length > 0) {
      this.draw_txt_detachment_list(attachments);
    } else {
      this.remove_txt_detachment_list();
    }
   
  } else {
    if (attachments.length > 0) {
      this.draw_html_detachment_list(attachments);
    } else {
      this.remove_html_detachment_list();
    }
  }
};

/** Remove from detached files HTML list */
rcube_webmail.prototype.remove_html_detachment_list = function () {
  var editor = tinyMCE.get(rcmail.env.composebody);
  var container = editor.dom.get('cloudAttachmentListRoot');
  if (container) {
    $(container).remove();
  }
};

/** draw detached files HTML list from scratch */
rcube_webmail.prototype.draw_html_detachment_list = function(attachments) {
  var editor = tinyMCE.get(rcmail.env.composebody);
  var container = editor.dom.get('cloudAttachmentListRoot');
  if (!container) {
    var body = editor.getBody(), doc = editor.getDoc();
    container = doc.createElement('div');
    $(container).attr('id', 'cloudAttachmentListRoot');
    $(container).css({'padding': '15px', 'background-color': '#D9edff'});
    // Put container above signature.
    var sig = editor.dom.get('_rc_sig');
    if (sig) {
       body.insertBefore(container, sig);
    } else if (rcmail.env.sig_above) {
      editor.getWin().focus();
      var node = editor.selection.getNode();
      if (node.nodeName == 'BODY') {
        // no real focus, insert at start
        body.insertBefore(container, body.firstChild);
        body.insertBefore(doc.createElement('br'), body.firstChild);
      }
      else {
        body.insertBefore(container, node.nextSibling);
        body.insertBefore(doc.createElement('br'), node.nextSibling);
      }
      
    } else {
      if (bw.ie) {
        body.appendChild(doc.createElement('br'));
      }
      body.appendChild(container);
    }
  } 
 
  $(container).html('<div id="cloudAttachmentListHeader" style="margin-bottom: 15px;"></div><div id="cloudAttachmentList" style="background-color: #ffffff; padding: 15px;"></div>');
  for(var i = 0; i < attachments.length; i++) {
    var att = attachments[i];
    var row = $('<div class="cloudAttachmentItem"></div>').css({'border': '1px solid #cdcdcd', 'border-radius': '5px', 'margin-top': '10px', 'margin-bottom': '10px', 'padding': '15px'});
    row.append('<img style="margin-right: 5px; float: left; width: 24px; height: 24px;" src="' + ATTACHMENT_IMG + '" alt="Attachment" />');

    var watermark = $('<span style="float: right;"></span>');
    watermark.append('<img style="margin-right: 5px;vertical-align:middle" src="' + BLUEMIND_IMG + '" alt="BlueMind" /><a style="color: #0f7edb ! important;" href="http://www.blue-mind.net/">BlueMind</a>');
    row.append(watermark);

    row.append('<a style="color: #0f7edb ! important;" href="' + att.options.url + '">' + att.name +'</a>');
    row.append('<span style="margin-left: 5px; font-size: small; color: grey;">(' + this.show_bytes(att.options.size) + ')</span>');

    if (att.options.expiration) {
      row.append('<span style="display:block; font-size: small; color: grey;">' + this.get_label('bm_filehosting.expires_on') + ' ' + att.options.expiration + '</span>');
    }

    $(container).find('#cloudAttachmentList').append(row);
  }


  if (attachments.length == 1) {
    $(container).find('#cloudAttachmentListHeader').html(this.get_label('bm_filehosting.linked_file'));
  } else {
    $(container).find('#cloudAttachmentListHeader').html(this.get_label('bm_filehosting.linked_files').replace('%d', attachments.length));
  }

};


/** Remove from detached files TXT list */
rcube_webmail.prototype.remove_txt_detachment_list = function () {
  var input_message = $("[name='_message']");  
  var message = input_message.val();
  var p = -1;
  var separator = '#--------------------------------------#';
  // remove the 'old' links
  // TODO
  var old = new RegExp('^' + separator + '$', 'm');
  //FIXME
  p = message.search(old);
  if (p >= 0) {
    var to = message.substring(p + 1, message.length).search(old);
    if (to >= 0) {
      to += p + 1 + separator.length;
      message = message.substring(0, p) + message.substring(to, message.length);
      p = p -1;
      var i = 0;
      while (message.substring(p, p+1) == '\n' && i < 3) {
        message = message.substring(0, p) + message.substring(p+1, message.length);
        i++;
      }
    }
  }  
  input_message.val(message);

};


/** draw detached files TXT list from scratch */
rcube_webmail.prototype.draw_txt_detachment_list = function(attachments) {
  var input_message = $("[name='_message']");
  var message = input_message.val();
  var p = -1;
  var separator = '#--------------------------------------#';
  //Generate link text
  var links = separator + '\n';
  if (attachments.length == 1) {
    links += this.get_label('bm_filehosting.linked_file') + "\n";
  } else {
    links += this.get_label('bm_filehosting.linked_files').replace('%d', attachments.length)  + "\n";
  }
 
  for(var i = 0; i < attachments.length; i++) {
    var att = attachments[i];    
    links += '- ' + att.name + ' : ' + att.options.url + " ( " + this.show_bytes(att.options.size) + " )\n";
    if (att.options.expiration) {
      links += '- ' + this.get_label('bm_filehosting.expires_on') + ' ' + att.options.expiration + '\n';
    }
  };
  links += separator + '\n';


  // remove the 'old' links
   var old = new RegExp('^' + separator + '$', 'm');
  p = message.search(old);
  if (p >= 0) {
    var to = message.substring(p + 1, message.length).search(old);
    if (to >= 0) {
      to += p + 1 + separator.length + 1;
      message = message.substring(0, p) + message.substring(to, message.length);
    } else {
      p = -1;
    }
  }
  
  if (this.env.sig_above) {
    if (p >= 0) { // in place of removed signature
      message = message.substring(0, p) + links + message.substring(p, message.length);
      cursor_pos = p - 1;
    }
    else if (!message) { // empty message
      cursor_pos = 0;
      message = '\n\n' + links;
    }
    else if (pos = this.get_caret_pos(input_message.get(0))) { // at cursor position
      message = message.substring(0, pos) + '\n' + links + '\n\n' + message.substring(pos, message.length);
      cursor_pos = pos;
    }
    else { // on top
      cursor_pos = 0;
      message = '\n\n' + links + '\n\n' + message.replace(/^[\r\n]+/, '');
    }
  }
  else {
    message = message.replace(/[\r\n]+$/, '');
    cursor_pos = !this.env.top_posting && message.length ? message.length+1 : 0;
    message += '\n\n' + links;
  }
  

  input_message.val(message);

  // move cursor before the signature
  this.set_caret_pos(input_message.get(0), cursor_pos);


};

rcmail.addEventListener('init', function(evt) {
  window.setTimeout(function() {rcmail.draw_detachment_list();}, 500);   
  rcmail.addEventListener('beforesend-attachment', rcmail.filehosting_send_attachments);   

});

/** Clear attachment content */
rcube_webmail.prototype.filehosting_clear_attachment = function() {
  $('.bm-filehosting-attachment').remove();
};

/** Override remove_from_attachment_list method */
rcmail.bm_filehosting_remove_from_attachment_list = rcmail.remove_from_attachment_list;

rcmail.remove_from_attachment_list = function(name) {;
  this.bm_filehosting_remove_from_attachment_list(name);
  this.draw_detachment_list();
};

/** Override add2attachment_list method */
rcmail.bm_filehosting_add2attachment_list = rcmail.add2attachment_list;

rcmail.add2attachment_list = function(name, att, upload_id, opt_init) {
  this.bm_filehosting_add2attachment_list(name, att, upload_id, !!opt_init);
  if (att.complete) {
    var element = $('#' + name);
    if (this.is_detached_attachment(att)) {
      element.prepend('<span class="fa fa-lg fa-cloud bm-filehosting-disposition"></span>');
    } else {
      element.prepend('<span class="fa fa-lg fa-paperclip bm-filehosting-disposition"></span>');
    }
  }
  if (!opt_init) {
    this.draw_detachment_list();
  }

};  

/** On editor toggle, redraw detached list file */
rcmail.addEventListener('beforetoggle-editor', function(evt) {
  rcmail.remove_detachment_list(!evt.html);
});

/** On editor toggle, redraw detached list file */
rcmail.addEventListener('aftertoggle-editor', function(evt) {
  window.setTimeout(function() {rcmail.draw_detachment_list(evt.html)}, 250);
});
  
  
/** Utils methods */
rcube_webmail.prototype.is_detached_attachment = function(attachment) {
  if (attachment['options'] && attachment['options']['disposition']) {
    return attachment['options']['disposition'] == 'filehosting';
  }
  return false;
};




/** On attach from local drive button click */
rcube_webmail.prototype.filehosting_browse_computer = function(dialog) {
  $('input[type=file]', dialog).last().click();
};

/** Add a files to the list of the file to attach to mail */
rcube_webmail.prototype.filehosting_add_files_to_mail = function(field, dialog) {
  if (field.files.length > 0) {
    var list = $('.bm-filehosting-filelist', dialog);
    for(var i = 0; i < field.files.length; i++) {
      rcmail.filehosting_add_file_to_mail(field.files.item(i), list, 'file'); 
    }
    $(field).val('');
  }
};

/** Add a files to the list of the file to attach to mail */
rcube_webmail.prototype.filehosting_add_file_to_mail = function(file, list, source) {
  var size = rcmail.show_bytes(file.size);
  var container = $('<div class="bm-filehosting-attachment"></div>');
  container.data('source', source)
  container.data('disposition', 'mail');
  container.data('file', file);
  var ret = this.triggerEvent('plugin.filehosting.addfile', container); 
  if (ret !== undefined && !ret) return;
  container = (ret === undefined) ? container : ret;
  container.append('<span class="bm-filehosting-filename" title="'+file.name+'">' + file.name + '</span><span class="bm-filehosting-filesize">(' + size + ')</span>');
  if (container.data('disposition') == 'mail') {
    container.prepend('<span class="bm-filehosting-disposition fa fa-2x fa-paperclip"></span>');
  } else {
    container.prepend('<span class="bm-filehosting-disposition fa fa-2x fa-cloud"></span>');
  }
  var rm = $('<a href="#" class="fa fa-2x fa-times"></a>');
  container.append(rm);
  rm.on('click', function() {
    container.remove();
    rcmail.triggerEvent('plugin.filehosting.listChanged');
  });
  list.append(container);
  this.triggerEvent('plugin.filehosting.listChanged');

};

/** Send attachments into storage (mail or remote) and add into mail attachment list */
rcube_webmail.prototype.filehosting_send_attachments = function(props) {
  var files = $('.bm-filehosting-attachment');
  files.each(function(index) {
    if ($(this).data('source') == 'file' && $(this).data('disposition') == 'mail') {
      rcmail.add_to_send_queue($(this).data('file'), rcmail.filehosting_send_to_mail)
    }
  });
};

/** Send attachments to mail storage */
rcube_webmail.prototype.filehosting_send_to_mail = function(file, uploadid) {
  var params = {
    '_uploadid': uploadid,
    '_id':rcmail.env.compose_id,
    '_attachments[]': file
  };
  return rcmail.http_multipart('upload', params).always(function() {
    rcmail.remove_from_attachment_list(uploadid);    
  });

};

/** Cancel file upload  */
rcube_webmail.prototype.filehosting_cancel_attachment = function(uploadid) {
  if (rcmail.env.attachments[uploadid] && rcmail.env.attachments[uploadid].request) {
    rcmail.env.attachments[uploadid].request.abort();
    rcmail.remove_from_attachment_list(uploadid);    
  }
};

/** Send a multipart request to webmail  */
rcube_webmail.prototype.http_multipart = function(action, params, opt_lock) {
  var url = this.url(action);
  params._remote = 1;
  params._unlock = (opt_lock ? opt_lock : 0);

  // trigger plugin hook
  var result = this.triggerEvent('request'+action, params);
  if (result !== undefined) {
  //  // abort if one the handlers returned false
    if (result === false)
      return false;
    else
      params = result;
  }

  var data = new FormData();
  for (key in params) {
    if ($.isArray(params[key])) {
      params[key].forEach(function(value) {
        data.append(key + '[]', value);
      });
    } else {
      data.append(key, params[key]);
    }
  }
  // send request
  this.log('HTTP MULTIPART: ' + url);

  return $.ajax({
    type: 'POST', 
    url: url, 
    contentType: false,      
    data: data, 
    processData: false,
    dataType: 'json',
    success: function(data){ rcmail.http_response(data); },
    error: function(o, status, err) { rcmail.http_error(o, status, err, opt_lock, 'send-attachment'); }
  });

};


/** Convert size in byte to human readable size */
rcube_webmail.prototype.show_bytes = function(size) {
  if (size == 0 || !$.isNumeric(size))
    return size;
  var unit = rcmail.get_label('B');
  if (size > 1024) {
    size = Math.floor(size/1024)
    unit = rcmail.get_label('KB');
  }
  if (size > 1024) {
    size = Math.floor(size * 10 / 1024) / 10;
    unit = rcmail.get_label('MB');
  }
  if (size > 1024) {
    size = Math.floor(size * 100 / 1024) / 100;
    unit = rcmail.get_label('GB');
  }  
  return size + ' ' + unit;
};


/** Convert size in byte to human readable size */
rcube_webmail.prototype.add_to_send_queue = function(file, callback) {
  var index = rcmail.send_queue_index || 0;
  var ts =  new Date().getTime() + (index++);
  var request = callback(file, ts);
  var cancel = rcmail.get_label('cancel');
  var upload = rcmail.get_label('uploading');  
  var content = '<a title="' + cancel + '" onclick="return rcmail.filehosting_cancel_attachment(\''+ts+'\');" href="#cancelupload" class="cancelupload fa fa-close"></a>' + '<span>' + upload + '</span>';
  rcmail.add2attachment_list(ts, { name:'', html:content, classname:'uploading', complete:false, request: request, id: ts});
  
};
