if (window.rcmail) {

  rcmail.addEventListener('init', function(evt) {
    // register command (directly enable in message view mode)
    rcmail.addEventListener('beforesend-attachment', rcmail.detachment_send_attachment);   
    rcmail.addEventListener('plugin.filehosting.addfile', rcmail.detachment_add_file_to_mail);
    rcmail.addEventListener('plugin.drag.filedropped', rcmail.detachment_drop_file_to_mail);

  });

}

/** Send attachments into storage (mail or remote) and add into mail attachment list */
rcube_webmail.prototype.detachment_send_attachment = function(props) {
  var files = $('.bm-filehosting-attachment');
  var cancel = rcmail.get_label('cancel');
  var upload = rcmail.get_label('upload');
  files.each(function(index) {
    if ($(this).data('source') == 'file' && $(this).data('disposition') == 'filehosting') {
      rcmail.add_to_send_queue($(this).data('file'), rcmail.detachment_send_to_filehosting);
    } 
  });
};

/** Send attachments to remote storage */
rcube_webmail.prototype.detachment_send_to_filehosting = function(file, uploadid) {
  var url = '/api/attachment/' + rcmail.env.bmDomain + '/' + file.name + '/share';
  var xhr = $.ajax({
    type: 'PUT', 
    url: url, 
    timeout: 0,
    headers: {
      'X-BM-ApiKey' : rcmail.env.bmSid
    },
    contentType: false,      
    data: file, 
    dataType: 'json',    
    processData: false,
    error: function(o, status, err) { rcmail.http_error(o, status, err, false, 'send-attachment'); }
  });
  xhr.pipe(function(data) {
    var postdata = {
      _id: rcmail.env.compose_id,
      _path: data.name,
      _url: data.publicUrl,
      _size: file.size,
      _mime: file.type,
      _uploadid: uploadid,
      _expiration: data.expirationDate
    }
    return rcmail.http_post('plugin.bm_detachment.add_link', postdata);
  }).always(function() {
    return rcmail.remove_from_attachment_list(uploadid);    
  });
  return xhr;

};
/** If a file is dropped automatically detach it if size > treshold */
rcube_webmail.prototype.detachment_drop_file_to_mail = function(file) {
  if (file.size && file.size > rcmail.env.max_attachmentsize) {
    rcmail.display_message(rcmail.env.filesizeerror, 'error');
    return null;
  }
  if (file.size >  rcmail.env.detachment_threshold || file.size >=  rcmail.env.max_encoded_filesize) {
    rcmail.add_to_send_queue(file, rcmail.detachment_send_to_filehosting);
    return null;
  }
  return file;
}

/** Add a files to the list of the file to attach to mail */
rcube_webmail.prototype.detachment_add_file_to_mail = function(container) {
  if (container.data('source') == 'file') {
    var file = container.data('file');
    if (file.size <  rcmail.env.detachment_threshold && file.size <  rcmail.env.max_encoded_filesize) {
      container.data('disposition', 'mail');
      var btn = $('<a class="goog-inline-block goog-menu-button  goog-button-base btn" href="#">' + rcmail.get_label('bm_detachment.store_remote') + '</a>');
      container.append(btn);
    } else if (file.size <  rcmail.env.max_attachmentsize){
      container.data('disposition', 'filehosting');
      if (file.size <  rcmail.env.max_encoded_filesize) {
        var btn = $('<a class="goog-inline-block goog-menu-button  goog-button-base btn" href="#">' + rcmail.get_label('bm_detachment.store_mail') + '</a>');
        container.append(btn);
      }
    } else {
      window.alert(rcmail.env.filesizeerror);
      return null;
    }
    if (btn) {
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
};
