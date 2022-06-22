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

require_once('plugins/filesystem_attachments/filesystem_attachments.php');

class bm_detachment extends filesystem_attachments {
  private $rcmail;
  private $attachmentClient;
  public function init() {
    $this->add_texts('localization', true);
    $roles = $_SESSION['bm_sso']['bmRoles'];
    if (!in_array('canRemoteAttach', $roles)) {
      return;
    }
    $this->rcmail = rcmail::get_instance();
    $this->attachmentClient = new BM\AttachmentClient($_SESSION['bm']['core'], $this->rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain']);
    
    $this->register_action('plugin.bm_detachment.add_link', array($this, 'addLinkAttachment'));
    $this->add_hook('template_object_composeattachmentform', array($this, 'uploadform'));

    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $this->setConfiguration();
      $this->add_texts('localization', true);
      $this->include_script('bm_detachment.js');
    }

  }

  public function setConfiguration() {
    $values = $this->attachmentClient->getConfiguration();
    $_SESSION['bm']['detachment']['detachment-threshold'] = $values->autoDetachmentLimit;
    $_SESSION['bm']['detachment']['max-attachmentsize'] = $values->maxAttachmentSize;
    if (!$values->autoDetachmentLimit) {
      $_SESSION['bm']['detachment']['detachment-threshold'] = $_SESSION['bm']['attachment']['filesize'];
    }
    if (!$values->maxAttachmentSize) {
      $_SESSION['bm']['detachment']['max-attachmentsize'] = PHP_INT_MAX;
    }
    $this->rcmail->output->set_env('max_attachmentsize', $_SESSION['bm']['detachment']['max-attachmentsize']);
    $this->rcmail->output->set_env('detachment_threshold', $_SESSION['bm']['detachment']['detachment-threshold']);

  }

  public function addLinkAttachment() {
    $compose_id = get_input_value('_id', RCUBE_INPUT_GPC);
    $size = get_input_value('_size', RCUBE_INPUT_GPC);
    $path = get_input_value('_path', RCUBE_INPUT_GPC);
    $mime = get_input_value('_mime', RCUBE_INPUT_GPC);
    $url = get_input_value('_url', RCUBE_INPUT_GPC);
    $uploadid = get_input_value('_uploadid', RCUBE_INPUT_GPC);
    $expiration = intval(get_input_value('_expiration', RCUBE_INPUT_GPC));
    $id = $this->file_id();
    $attachment = array(
      'size' => $size,
      'name' => $path,
      'mimetype' => 'application/octet-stream',
      'group' => $compose_id,
      'id' => $id,
      'path' => $url,
      'headers' => array(
        'X-Mozilla-Cloud-Part' => "cloudFile; url=$url; name=$path",
        'X-BM-Disposition' => "filehosting; url=$url; name=$path; size=$size; mime=$mime"
      ),
      'options' => array(
        'disposition' => 'filehosting',
        'url' =>  $url,
         'uploaded' => true,
        'size' => show_bytes($size)
      )
    );

    if ($expiration != null) {
      $dateformat = $_SESSION['bm']['settings']['date'] == 'yyyy-MM-dd' ? 'Y-m-d' : 'd/m/Y';
      $tz = $_SESSION['bm']['settings']['timezone'];
      $d = new DateTime($tz);
      $d->setTimestamp($expiration/1000);
      $attachment['options']['expiration'] = $d->format($dateformat); 
    }

    bm_filehosting::add_to_attachment($attachment);    
  }


  /**
   * Replace default upload form with BM one
   */ 
  public function uploadform($attrib) {
    $max_filesize = show_bytes(parse_bytes($_SESSION['bm']['detachment']['max-attachmentsize']));
    $this->rcmail->output->set_env('filesizeerror', rcube_label(array(
        'name' => 'filesizeerror', 'vars' => array('size' => $max_filesize))));
    return $attrib;
  }

}
