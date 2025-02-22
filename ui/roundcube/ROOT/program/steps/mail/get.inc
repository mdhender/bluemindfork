<?php

/*
 +-----------------------------------------------------------------------+
 | program/steps/mail/get.inc                                            |
 |                                                                       |
 | This file is part of the Roundcube Webmail client                     |
 | Copyright (C) 2005-2011, The Roundcube Dev Team                       |
 |                                                                       |
 | Licensed under the GNU General Public License version 3 or            |
 | any later version with exceptions for skins & plugins.                |
 | See the README file for a full license statement.                     |
 |                                                                       |
 | PURPOSE:                                                              |
 |   Delivering a specific part of a mail message                        |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Thomas Bruederli <roundcube@gmail.com>                        |
 +-----------------------------------------------------------------------+

 $Id$

*/


// show loading page
if (!empty($_GET['_preload'])) {
  $url = preg_replace('/[&?]+_preload=1/', '', $_SERVER['REQUEST_URI']);
  $message = rcube_label('loadingdata');

  header('Content-Type: text/html; charset=' . RCMAIL_CHARSET);
  print "<html>\n<head>\n"
        . '<meta http-equiv="refresh" content="0; url='.Q($url).'">' . "\n"
        . '<meta http-equiv="content-type" content="text/html; charset='.RCMAIL_CHARSET.'">' . "\n"
        . "</head>\n<body>\n$message\n</body>\n</html>";
  exit;
}

ob_end_clean();

// Now we need IMAP connection
if (!$RCMAIL->storage_connect()) {
  // Get action is often executed simultanously.
  // Some servers have MAXPERIP or other limits.
  // To workaround this we'll wait for some time
  // and try again (once).
  // Note: Random sleep interval is used to minimize concurency
  // in getting message parts
  if (!isset($_GET['_redirected'])) {
    usleep(rand(10,30)*100000); // 1-3 sec.
    header('Location: ' . $_SERVER['REQUEST_URI'] . '&_redirected=1');
  }
  else {
    raise_error(array(
      'code' => 500, 'type' => 'php',
      'file' => __FILE__, 'line' => __LINE__,
      'message' => 'Unable to get/display message part. IMAP connection error'),
      true, true);
  }
  // Don't kill session, just quit (#1486995)
  exit;
}

// similar code as in program/steps/mail/show.inc
if (!empty($_GET['_uid'])) {
  $RCMAIL->config->set('prefer_html', true);
  $MESSAGE = new rcube_message(get_input_value('_uid', RCUBE_INPUT_GET));
}

// show part page
if (!empty($_GET['_frame'])) {
  if (($part_id = get_input_value('_part', RCUBE_INPUT_GPC)) && ($part = $MESSAGE->mime_parts[$part_id]) && $part->filename)
    $OUTPUT->set_pagetitle($part->filename);

  $OUTPUT->send('messagepart');
  exit;
}

else if (strlen($pid = get_input_value('_part', RCUBE_INPUT_GET))) {

  if ($part = $MESSAGE->mime_parts[$pid]) {
    $ctype_primary = strtolower($part->ctype_primary);
    $ctype_secondary = strtolower($part->ctype_secondary);
    $mimetype = sprintf('%s/%s', $ctype_primary, $ctype_secondary);

    // allow post-processing of the message body
    $plugin = $RCMAIL->plugins->exec_hook('message_part_get',
      array('uid' => $MESSAGE->uid, 'id' => $part->mime_id, 'mimetype' => $mimetype, 'part' => $part, 'download' => !empty($_GET['_download'])));

    if ($plugin['abort'])
      exit;

    // overwrite modified vars from plugin
    $mimetype = $plugin['mimetype'];
    list($ctype_primary, $ctype_secondary) = explode('/', $mimetype);
    if ($plugin['body'])
      $part->body = $plugin['body'];

    $browser = $RCMAIL->output->browser;

    // send download headers
    if ($plugin['download']) {
      header("Content-Type: application/octet-stream");
      if ($browser->ie)
        header("Content-Type: application/force-download");
    }
    else if ($ctype_primary == 'text') {
      header("Content-Type: text/$ctype_secondary; charset=" . ($part->charset ? $part->charset : RCMAIL_CHARSET));
    }
    else {
      $mimetype = rcmail_fix_mimetype($mimetype);
      header("Content-Type: $mimetype");
      header("Content-Transfer-Encoding: binary");
    }

    // deliver part content
    if ($ctype_primary == 'text' && $ctype_secondary == 'html' && empty($plugin['download'])) {
      // Check if we have enough memory to handle the message in it
      // #1487424: we need up to 10x more memory than the body
      if (!rcmail_mem_check($part->size * 10)) {
        $out = '<body>' . rcube_label('messagetoobig'). ' '
          . html::a('?_task=mail&_action=get&_download=1&_uid='.$MESSAGE->uid.'&_part='.$part->mime_id
            .'&_mbox='. urlencode($RCMAIL->storage->get_folder()), rcube_label('download')) . '</body></html>';
      }
      else {
        // get part body if not available
        if (!$part->body)
          $part->body = $MESSAGE->get_part_content($part->mime_id);

        $out = rcmail_print_body($part, array('safe' => $MESSAGE->is_safe, 'inline_html' => false));
      }

      $OUTPUT = new rcube_html_page();
      $OUTPUT->write($out);
    }
    else {
      // don't kill the connection if download takes more than 30 sec.
      @set_time_limit(0);

      $ext      = '.' . ($mimetype == 'text/plain' ? 'txt' : $ctype_secondary);
      $filename = $part->filename ? $part->filename : ($MESSAGE->subject ? $MESSAGE->subject : 'roundcube') . $ext;
      $filename = preg_replace('[\r\n]', '', $filename);

      $disposition = !empty($plugin['download']) ? 'attachment' : 'inline';

      header('Pragma: private');
      header("Cache-Control: private, must-revalidate");

      // Workaround for nasty IE bug (#1488844)
      // If Content-Disposition header contains string "attachment" e.g. in filename
      // IE handles data as attachment not inline
      if ($disposition == 'inline' && $browser->ie && $browser->ver < 9) {
        $filename = str_ireplace('attachment', 'attach', $filename);
      }
      if ($browser->ie && $browser->ver < 9) {
        $filename = rawurlencode($filename);
        header("Content-Disposition: $disposition; filename=\"$filename\"");
      } elseif ($browser->safari || $browser->mz) {
        header("Content-Disposition: $disposition; filename=\"$filename\"");
      } else {
       $encoded = rawurlencode($filename);
      	header("Content-Disposition: $disposition; filename*=UTF-8''$encoded;filename=\"".rawurldecode($filename)."\"");
      }
      // do content filtering to avoid XSS through fake images
      if (!empty($_REQUEST['_embed']) && $browser->ie && $browser->ver <= 8) {
        if ($part->body)
          echo preg_match('/<(script|iframe|object)/i', $part->body) ? '' : $part->body;
        else if ($part->size) {
          $stdout = fopen('php://output', 'w');
          stream_filter_register('rcube_content', 'rcube_content_filter') or die('Failed to register content filter');
          stream_filter_append($stdout, 'rcube_content');
          $RCMAIL->storage->get_message_part($MESSAGE->uid, $part->mime_id, $part, false, $stdout);
        }
      }
      else {
        // turn off output buffering and print part content
        if ($part->body)
          echo $part->body;
        else if ($part->size)
          $RCMAIL->storage->get_message_part($MESSAGE->uid, $part->mime_id, $part, true);
      }
    }

    exit;
  }
}

// print message
else {
  // send correct headers for content type
  header("Content-Type: text/html");

  $cont = "<html>\n<head><title></title>\n</head>\n<body>";
  $cont .= rcmail_message_body(array());
  $cont .= "\n</body>\n</html>";

  $OUTPUT = new rcube_html_page();
  $OUTPUT->write($cont);

  exit;
}


// if we arrive here, the requested part was not found
header('HTTP/1.1 404 Not Found');
exit;


