if (window.rcmail) {
  rcmail.addEventListener('init', function(evt) {
    var target = $('#compose-attachments')[0];
    target.ondrop = function(e) {rcmail.ondropin(e)};
    target.ondragenter = function(e) {rcmail.ondragin(e)};
    target.ondragover = function(e) {rcmail.ondragin(e)};
    target.ondragleave = function(e) {rcmail.ondragout(e)};
  });
}

var XMLHttpBinaryRequest = function() {
  var xhr;
  if (window.XMLHttpRequest) {
    xhr = new XMLHttpRequest();
  } else {
    rcmail.display_message(rcmail.get_label('fileuploaderror'), 'error');
  }
  if (!xhr.sendAsBinary) {
    xhr.sendAsBinary = function(datastr) {
      function byteValue(x) {
          return x.charCodeAt(0) & 0xff;
      }
      var ords = Array.prototype.map.call(datastr, byteValue);
      var ui8a = new Uint8Array(ords);
      this.send(ui8a.buffer);
    }

  }
  return xhr;
};

rcube_webmail.prototype.ondragin = function (e) {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'copy';
  $(this).find('.boxlistcontent').css('background-color','#FFFFA6');
  return false;
};

rcube_webmail.prototype.ondragout = function (e) {
  e.preventDefault();
  e.dataTransfer.dropEffect = 'copy';
  $(this).find('.boxlistcontent').css('background-color','#FFF');
  return false;
};

rcube_webmail.prototype.ondropin = function(e, opt_field) {
 e.preventDefault();
 e.stopPropagation();
 var data = e.dataTransfer;
 var files = data.files;
 for(var i = 0; i < data.files.length; i++) {
   var file = data.files[i]; 
   var ts = new Date().getTime() + i;
   var ret = this.triggerEvent('plugin.drag.filedropped', file);
   if (ret !== undefined && !ret) continue;
   if (file.size && file.size > this.env.max_encoded_filesize) {
     this.display_message(this.env.filesizeerror, 'error');
     continue;
   }   
   rcmail.add_to_send_queue(file, rcmail.filehosting_send_to_mail);
 }
};

rcube_webmail.prototype.ondropcomplete = function (content, ts) {
  var frame_name = 'rcmupload' + new Date().getTime();
  try {
    if (!content.match(/add2attachment/) ) {
      if (!content.match(/display_message/))
        rcmail.display_message(rcmail.get_label('fileuploaderror'), 'error');
      rcmail.remove_from_attachment_list(ts);
    }
    if (document.all) {
      var html = '<iframe name="'+frame_name+'" src="program/blank.gif" style="width:0;height:0;visibility:hidden;"></iframe>';
      document.body.insertAdjacentHTML('BeforeEnd', html);
    }
    else { // for standards-compilant browsers
      var frame = document.createElement('iframe');
      frame.name = frame_name;
      frame.style.border = 'none';
      frame.style.width = 0;
      frame.style.height = 0;
      frame.style.visibility = 'hidden';
      document.body.appendChild(frame);
    }
    // handle upload errors, parsing iframe content in onload
    var doc = frame.document;
    if(frame.contentDocument)
      doc = frame.contentDocument; // For NS6
    else if(frame.contentWindow)
      doc = frame.contentWindow.document; // For IE5.5 and IE6
    // Put the content in the iframe
    doc.open();
    doc.writeln(content);
    doc.close();
  } catch (err) {}
};

