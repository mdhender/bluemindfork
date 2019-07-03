<?php
/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
*/
?>
<?php

/**
 * This plugin is a bag to put all modification to fix unattended behaviour of 
 * rc that can be alter with a plugin. 
 *
 */

class bm_embedded_images extends rcube_plugin {

  public function init() {
    $this->add_hook('message_outgoing_body', array($this, 'message_outgoing_body'));
  }

  // global hooks
  public function message_outgoing_body($args) {
    $body = $args['body'];
    $mime_message = $args['message'];
    if (preg_match_all('#img src=[\'"](data:((image/([^;]+))|([<&lt;]+))?;base64,([^\'"]+))[\'"]#', $body, $matches)) {
      foreach ($matches[1] as $key => $data) {
        if($matches[3][$key]) {
          $type = $matches[3][$key];
          $ext = $matches[4][$key];
          $imgdata = base64_decode($matches[6][$key]);
        } else {
          $imgdata = base64_decode($matches[6][$key]);
          if (function_exists('finfo_open')) {
            $f = finfo_open();
            $type = finfo_buffer($f, $imgdata, FILEINFO_MIME_TYPE);
          } else {
            $type = $this->getImageMimeType($imgdata);
          }
          $ext = array_pop(explode('/', $type));
        }
        $cid = md5(uniqid(time()));
        $name = 'image-' . substr($cid,0,5) . '.' . $ext;
        if (!$mime_message->addHTMLImage($imgdata, $type, $name, false, $cid)) {
          $RCMAIL->output->show_message("attachment", 'error');
        }
        $body = str_replace($matches[1][$key], $name, $body);
      }
    }
    return array('body' => $body);
  } 
  
  private function getBytesFromHexString($hexdata) {
    for($count = 0; $count < strlen($hexdata); $count+=2)
      $bytes[] = chr(hexdec(substr($hexdata, $count, 2)));
    return implode($bytes);
  }

  private function getImageMimeType($imagedata) {
    $imagemimetypes = array( 
      "jpeg" => "FFD8", 
      "png" => "89504E470D0A1A0A", 
      "gif" => "474946",
      "bmp" => "424D", 
      "tiff" => "4949",
      "tiff" => "4D4D"
    );

    foreach ($imagemimetypes as $mime => $hexbytes) {
      $bytes = getBytesFromHexString($hexbytes);
      if (substr($imagedata, 0, strlen($bytes)) == $bytes)
        return $mime;
    }

    return NULL;
  }

}
