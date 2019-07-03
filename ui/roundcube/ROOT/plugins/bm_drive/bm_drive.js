
/** Add listeners */
rcmail.addEventListener('init', function(evt) {
  rcmail.addEventListener('beforesend-attachment', rcmail.drive_send_attachment); 
  rcmail.addEventListener('plugin.filehosting.addfile', rcmail.drive_add_link_to_mail);
});


/** Show remote drive file chooser */
rcube_webmail.prototype.drive_browse = function() {
  var setOptions, child, rcmail = this;
  var options = {
    success: function(links) {
      rcmail.drive_add_links_to_mail(links);
    },
    multi: true,
    close: true
  };
  var w = 640, h = 512;
  var t = (window.screenY || window.screenTop) + ((window.outerHeight || document.documentElement.offsetHeight) - h) / 2;
  var l = (window.screenX || window.screenLeft) + ((window.outerWidth || document.documentElement.offsetWidth) - w) / 2;
  child = window.open('/chooser/#', 'chooser', "width=" + w + ",height=" + h + ",left=" + l + ",top=" + t)
  setOptions = function() {
    if (child.application) {
      child.application.setOptions(options);
    } else {
      setTimeout(setOptions, 50);
    };
  }
  setOptions(); 
  $('#upload-dialog').on('dialogclose', function(e) {
    if (child) {
      child.close();
    }
  });

};

/** Send attachments into storage (mail or remote) and add into mail attachment list */
rcube_webmail.prototype.drive_send_attachment = function(props) {
  var files = $('.bm-filehosting-attachment');
  files.each(function(index) {
    if ($(this).data('source') == 'link' && $(this).data('file')) {    
      if ($(this).data('disposition') == 'mail') {
        rcmail.add_to_send_queue($(this).data('file'), rcmail.drive_send_to_mail);
      } else {
        rcmail.add_to_send_queue($(this).data('file'), rcmail.drive_send_to_filehosting);
      }
    }
  });
};


/** download linked file and store it into mail */
rcube_webmail.prototype.drive_send_to_mail = function(link, uploadid) {
  var postdata = {
    _id: rcmail.env.compose_id,
    _path: link.path,
    _size: link.size,
    _name: link.name,
    _uploadid: uploadid,
  }
  return rcmail.http_post('plugin.bm_drive.add_file', postdata).always(function() {
    return rcmail.remove_from_attachment_list(uploadid);    
  });  
};

/** Add linked file to mail */
rcube_webmail.prototype.drive_send_to_filehosting = function(link, uploadid) {
  var postdata = {
    _id: rcmail.env.compose_id,
    _path: link.path,
    _size: link.size,
    _name: link.name,
    _uploadid: uploadid,
  }
  return rcmail.http_post('plugin.bm_drive.add_link', postdata).always(function() {
    return rcmail.remove_from_attachment_list(uploadid);    
  });

};


/** Add links to the list of the file to attach to mail */
rcube_webmail.prototype.drive_add_links_to_mail = function(links) {
  for (var i = 0; i < links.length; i++) {
    var size = 0;
    var sizeKeys = ['Content-Length', 'size']
    for (var j = 0; j < links[i].metadata.length; j++) {
      if (sizeKeys.indexOf(links[i].metadata[j]['key']) >= 0) {
        links[i].size = links[i].metadata[j]['value'];
        break;
      }
    }
  }

  if (links.length > 0) {
    var list = $('.bm-filehosting-filelist');
    for(var i = 0; i < links.length; i++) {
      rcmail.filehosting_add_file_to_mail(links[i], list, 'link'); 
    }
  }
};

/** Add on link to the list of files to attach to mail */
rcube_webmail.prototype.drive_add_link_to_mail = function(container) {
  if (container.data('source') == 'link') {
    var link = container.data('file');
    container.data('disposition', 'filehosting');
    if (link.size <  rcmail.env.max_encoded_filesize) {
      var btn = $('<a class="goog-inline-block goog-menu-button  goog-button-base btn" href="#">'+rcmail.get_label('bm_detachment.store_mail')+'</a>');
      container.append(btn);
      btn.on('click', function() {
        var disposition = container.data('disposition') == 'mail' ? 'filehosting' : 'mail';
        container.data('disposition', disposition);
        container.children('.bm-filehosting-disposition').toggleClass('fa-paperclip', disposition == 'mail').toggleClass('fa-cloud', disposition != 'mail');
        container.children('.btn').html(disposition == 'mail' ? rcmail.get_label('bm_detachment.store_remote') : rcmail.get_label('bm_detachment.store_mail'));
        rcmail.triggerEvent('plugin.filehosting.listChanged');
      });  
    }
  }
  return container;
}
