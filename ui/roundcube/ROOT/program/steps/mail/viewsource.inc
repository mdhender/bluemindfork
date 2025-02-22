<?php

/*
 +-----------------------------------------------------------------------+
 | program/steps/mail/viewsource.inc                                     |
 |                                                                       |
 | This file is part of the Roundcube Webmail client                     |
 | Copyright (C) 2005-2009, The Roundcube Dev Team                       |
 |                                                                       |
 | Licensed under the GNU General Public License version 3 or            |
 | any later version with exceptions for skins & plugins.                |
 | See the README file for a full license statement.                     |
 |                                                                       |
 | PURPOSE:                                                              |
 |   Display a mail message similar as a usual mail application does     |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Thomas Bruederli <roundcube@gmail.com>                        |
 +-----------------------------------------------------------------------+

 $Id$

*/

ob_end_clean();

// similar code as in program/steps/mail/get.inc
if ($uid = get_input_value('_uid', RCUBE_INPUT_GET))
{
  $headers = $RCMAIL->storage->get_message_headers($uid);
  $charset = $headers->charset ? $headers->charset : $CONFIG['default_charset'];
  header("Content-Type: text/plain; charset={$charset}");

  if (!empty($_GET['_save'])) {
    $subject = rcube_mime::decode_header($headers->subject, $headers->charset);
    $filename = ($subject ? $subject : $RCMAIL->config->get('product_name', 'email')) . '.eml';
    $filename = preg_replace('[\r\n]', '', $filename);
    $browser = $RCMAIL->output->browser;
    if ($browser->ie && $browser->ver < 9) {
      $filename = rawurlencode($filename);
      header("Content-Disposition: attachment; filename=$filename");
    } elseif ($browser->safari) {
      header("Content-Disposition: attachment; filename=$filename");
    } else {
      $encoded = rawurlencode($filename);
    	header("Content-Disposition: attachment; filename*=UTF-8''$encoded;filename=".rawurldecode($filename));
    }

    header("Content-Length: {$headers->size}");
  }

  $RCMAIL->storage->print_raw_body($uid, empty($_GET['_save']));
}
else
{
  raise_error(array(
      'code' => 500,
      'type' => 'php',
      'file' => __FILE__, 'line' => __LINE__,
      'message' => 'Message UID '.$uid.' not found'),
    true, true);
}

exit;

