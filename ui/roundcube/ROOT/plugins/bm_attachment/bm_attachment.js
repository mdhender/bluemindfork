/* Drag upload plugin script */
/* TODO: Preview, Hover enlarge, Hover Menu, Click Preview Window, Ajax Thumbnail loading

 */
if (window.rcmail) {

  
  var originalBS = $.ajaxSettings.beforeSend;
  var originalXHR = $.ajaxSettings.xhr;
  $.ajaxSetup({
    beforeSend: function(xhr, settings) {
      originalBS(xhr, settings);
      this.deferred = $.Deferred();
      xhr.progress = this.deferred.progress;
    },
      xhr: function(callback) {
        var xhr = originalXHR(), that = this;
        if (xhr.upload) {
          if (typeof xhr.upload.addEventListener == "function") {
            xhr.upload.addEventListener("progress", function(evt) {
              that.deferred.notify(evt);
            },false);
          }
        }
        return xhr;      
      }
  });

  rcmail.attachment_add2attachment_list = rcmail.add2attachment_list;

  rcmail.add2attachment_list = function(name, attachment, id, opt_init) {
    
    this.attachment_progress(attachment);
    this.attachment_add2attachment_list(name, attachment, id, opt_init);
    this.attachment_preview(name, attachment, id ,opt_init);
    this.attachment_size(attachment);
    this.triggerEvent('fileuploaded', {name: name, attachment: attachment, id: id});

  };

  rcmail.addEventListener('init', function(evt) {
    rcmail.addEventListener('plugin.filehosting.listChanged', rcmail.attachment_upload_size);
    if (rcmail.env.attachments)  {
      for (id in rcmail.env.attachments) {
        var att = rcmail.env.attachments[id];
        att['html'] = $('#' + id).html();
        att['classname'] = $('#' + id).prop('class');
        rcmail.add2attachment_list(id, rcmail.env.attachments[id], id, true);

      }
    }
    $('#upload-dialog').on('dialogopen', function(e) {
      var progress = $('#compose-attachments > .progress').clone().addClass('active');
      $('#upload-dialog .hint').empty().append(progress);
      $('#upload-dialog .hint .progress-text').remove();
      $('#upload-dialog .hint .bar').append('<div class="progress-text">' + rcmail.show_bytes($('#compose-attachments .progress').data('size'))+ '</div>')
      var bar = $('#upload-dialog .hint .progress .bar').clone();
      bar.empty().addClass('bar-stripped').append('<div class="progress-text"></div>');
      bar.css('width', '');
      progress.append(bar);
    })
  });

  rcmail.attachment_remove_from_attachment_list = rcmail.remove_from_attachment_list;

  rcmail.remove_from_attachment_list = function(name) {;
    this.attachment_remove_from_attachment_list(name);
    this.attachment_size();
  };
  

}
  rcube_webmail.prototype.attachment_upload_size = function() {
    var files = $('.bm-filehosting-attachment');
    var size = 0;
    files.each(function(index) {
      if ($(this).data('disposition') == 'mail' && $(this).data('file')) {
        size += parseInt($(this).data('file').size, 10);
      }
    });
    $('#upload-dialog .hint .bar.bar-stripped .progress-text').html(rcmail.show_bytes(size));
    var current = Math.round((($('#compose-attachments .progress').data('size') || 0) / rcmail.env.max_encoded_filesize) * 100);
    var neo = Math.min(Math.round((size / rcmail.env.max_encoded_filesize) * 100), 100 - current);
    var total = current + neo;
    $('#upload-dialog .hint .bar.bar-stripped').css('width', neo + '%').toggleClass('bar-warning', (total > 75 && total < 100)).toggleClass('bar-danger', (total >= 100));
    if (total >= 100) {
      rcmail.display_message(rcmail.get_label('bm_attachment.size_error'), 'warning')
    }

  }

  rcube_webmail.prototype.attachment_preview = function(name, attachment, id, opt_init) {
    if(attachment.complete && !this.is_detached_attachment(attachment)){
      var element = $('#' + name);
      element.append('<span id="popup' + name + 'link">&nbsp;</span>');

      var file = name.replace('rcmfile', '');
      var previewURL = rcmail.env.comm_path + '&_action=plugin.bm_attachment.preview&_file=' + file + '&_id=' + rcmail.env.compose_id;
      var popup = '<div id="popup' + name + '" class="' + element.attr('class') + ' attachmentpreview popupmenu">' + rcmail.env.attachments[name].name + ' <br />' + '<img src="' + previewURL + '">';
      $(document.body).append(popup);
      element.mouseover(function (evt) {UI.show_popup('popup' +  name , true)});
      element.mouseout(function (evt) {UI.show_popup('popup' + name , false)});
      $('.delete', element).click(function(evt) {
        evt.stopPropagation();
      })
      element.click(function(evt) {
        document.location.href = rcmail.env.comm_path + '&_action=plugin.bm_attachment.download&_file=' + file + '&_id=' + rcmail.env.compose_id;
      });
    }
  };
  rcube_webmail.prototype.attachment_size= function(attachment) {
    if(!attachment || attachment.complete && !this.is_detached_attachment(attachment)){
      var size = 0;
      for (id in rcmail.env.attachments) {
        var att = rcmail.env.attachments[id];
        if (att.complete && !this.is_detached_attachment(att) && att.options) {
          size += parseInt(att.options.size, 10);
        }
      }      
      var progress = Math.min(Math.round((size / rcmail.env.max_encoded_filesize) * 100), 100);

      $('#compose-attachments .progress').data('size', size);
      $('#compose-attachments .progress-text').html(this.show_bytes(size) + '/' + this.show_bytes(rcmail.env.max_encoded_filesize));
      $('#compose-attachments .bar').css('width', progress + '%').toggleClass('bar-warning', (progress > 75 && progress < 100)).toggleClass('bar-danger', (progress >= 100));
      $('#compose-attachments .bar .progress-text').css('width', $('#compose-attachments .progress').css('width'));
      if (progress >= 100) {
        this.display_message(this.get_label('bm_attachment.size_error'), 'warning')
      }
    }
  };
  rcube_webmail.prototype.attachment_progress= function(attachment) {
    if(attachment && attachment.request && !attachment.complete) {
      attachment.html += '<div class="progress"><div class="progress-text"></div><div class="bar"></div></div>';
      attachment.request.progress(function(evt) {
        if (evt.lengthComputable) {
          var complete = (evt.loaded / evt.total) * 100;
          $('#' + attachment.id + ' .bar').css('width', complete + '%');
        } else {
          $('#' + attachment.id + ' .progress').addClass('active');
          $('#' + attachment.id + ' .bar').addClass('stripped').css('width', '100%');
        }
      });
    }
  };
