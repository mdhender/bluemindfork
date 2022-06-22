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

class bm_drive extends filesystem_attachments {

  private $rcmail;
  private $output;
  private $fileHostingClient;

  public function init() {
    $this->add_texts('localization', true);
    $roles = $_SESSION['bm_sso']['bmRoles'];
    if (!in_array('canUseFilehosting', $roles)) {
      return;
    }
    $this->rcmail = rcmail::get_instance();
    $this->fileHostingClient = new BM\FileHostingClient($_SESSION['bm']['core'], $this->rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain']);

    $this->output = $this->rcmail->output;

    $this->add_hook('plugin.bm_filehosting.attach_button', array($this, 'driveAttachmentButton'));
    $this->register_action('plugin.bm_drive.add_link', array($this, 'addLinkAttachment'));
    $this->register_action('plugin.bm_drive.add_file', array($this, 'addFileAttachment'));
    
    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $this->add_texts('localization', true);
      $this->include_stylesheet($this->local_skin_path() . "/bm_drive.css");
      $this->include_script('bm_drive.js');
    }

  }

  public function addFileAttachment() {
    $compose_id = get_input_value('_id', RCUBE_INPUT_GPC);
    $size = get_input_value('_size', RCUBE_INPUT_GPC);
    $path = get_input_value('_path', RCUBE_INPUT_GPC);
    $name = get_input_value('_name', RCUBE_INPUT_GPC);
    $uploadid = get_input_value('_uploadid', RCUBE_INPUT_GPC);

    $data = $this->fileHostingClient->get($path);
     
    $attachment = array(
      'size' => (int)$size,
      'name' => $name,
      'group' => $compose_id,
      'data' => $data 
    );

    $attachment = parent::save($attachment);
    $attachment['mime'] = rc_mime_content_type($attachment['path'], $name);

    bm_filehosting::add_to_attachment($attachment);
    
  }

  public function addLinkAttachment() {
    $compose_id = get_input_value('_id', RCUBE_INPUT_GPC);
    $size = (int) get_input_value('_size', RCUBE_INPUT_GPC);
    $path = get_input_value('_path', RCUBE_INPUT_GPC);
    $name = get_input_value('_name', RCUBE_INPUT_GPC);
    $uploadid = get_input_value('_uploadid', RCUBE_INPUT_GPC);
    $share = $this->fileHostingClient->share($path, 0, null);
    $expiration = intval($share->expirationDate); 
    $url = $share->url;
    $id = $this->file_id();
    $mime = rc_mime_content_type(null, $name);
    $attachment = array(
      'size' => $size,
      'name' => $name,
      'mimetype' => 'application/octet-stream',
      'group' => $compose_id,
      'id' => $id,
      'path' => $url,
      'headers' => array(
        'X-Mozilla-Cloud-Part' => "cloudFile; url=$url; name=$name",
        'X-BM-Disposition' => "filehosting; url=$url; name=$name; size=$size; mime=$mime"
      ),
      'options' => array(
        'disposition' => 'filehosting',
        'url' =>  $url,
        'size' => $size,
        'uploaded' => true
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


  public function driveAttachmentButton($buttons) {
    $attachFromDrive = array(
      'name' => 'plugin.bm_drive.browse_drive',
      'onclick' => 'return rcmail.drive_browse()',
      'type' => 'link', 
      'class' => 'goog-button-base-last goog-inline-block goog-menu-button  goog-button-base btn',
      'classact' => 'goog-button-base-last goog-inline-block goog-menu-button goog-button-base btn',
      'classsel' => 'goog-button-base-last goog-inline-block goog-menu-button goog-button-base  goog-button-base-active btn',
      'title' => $this->gettext('browse_remote'),
      'content' => $this->gettext('browse_remote')
    );        
    $buttons['drive'] = $this->output->button($attachFromDrive);
    return $buttons;
  }
}
