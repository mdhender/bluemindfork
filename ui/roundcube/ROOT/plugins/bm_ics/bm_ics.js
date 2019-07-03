function accept(container, id, recurid, me) {
  if (!$('#ics-toolbar-accepted').hasClass('highlight')) {
    update(container, id, recurid, me, 'Accepted');
  }
}

function tentative(container, id, recurid, me) {
  if (!$('#ics-toolbar-tentative').hasClass('highlight')) {
    update(container, id, recurid, me, 'Tentative');
  }
}

function decline(container, id, recurid, me) {
  if (!$('#ics-toolbar-declined').hasClass('highlight')) {
    update(container, id, recurid, me, 'Declined');
  }
}

function update(container, id, recurid, me, part) {
  rcmail.http_post('plugin.bm_ics.update', '_uid='+id+'&_recurid='+recurid+'&_me='+me+'&_part='+part+'&_container='+container);
  $('#ics-toolbar-accepted').removeClass('highlight');
  $('#ics-toolbar-tentative').removeClass('highlight');
  $('#ics-toolbar-declined').removeClass('highlight');
  if (part == 'Accepted') {
    $('#ics-toolbar-accepted').addClass('highlight');
  } else if (part == 'Tentative') {
    $('#ics-toolbar-tentative').addClass('highlight');
  } else if (part == 'Declined') {
    $('#ics-toolbar-declined').addClass('highlight');
  }
}

