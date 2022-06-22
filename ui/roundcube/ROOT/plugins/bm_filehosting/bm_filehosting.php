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

class bm_filehosting extends filesystem_attachments {

  private $rcmail;
  private $output;
  private $fileHostingClient;
  private $detachmentThreshol;

  public function init() {
    parent::init();
    $this->rcmail = rcmail::get_instance();
    $this->fileHostingClient = new BM\FileHostingClient($_SESSION['bm']['core'], $this->rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain']);

    $this->output = $this->rcmail->output;


    $this->add_hook('attachment_add_to_mail', array($this, 'addAttachment'));
    $this->add_hook('part_disposition', array($this, 'disposition'));

    $this->add_hook('template_object_composeattachmentlist', array($this, 'attachmentButton'));
    $this->add_hook('template_object_composeattachmentform', array($this, 'uploadform'));

    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $this->add_texts('localization', true);
      $this->include_stylesheet($this->local_skin_path() . "/bm_filehosting.css");
      $this->include_script('bm_filehosting.js');
      $this->rcmail->output->add_label('B', 'KB', 'MB', 'GB');
    }

    $this->add_texts('localization', true);
  }

  /**
   * On mail parsing add a flag on mail part which seems to be a detached file.
   */
  public function disposition($args) {
    $mime = $args['mime'];
    if ($this->isDetached($mime)) {
      $args['disposition'] = 'filehosting';
    }
    return $args;
  }

  /**
   * On mail writing, modify detached file part to be less visible by clients.
   */
  public function addAttachment($args) {
    if ($this->isDetached($args['attachment'])) {
      $args['file'] = '';
      $args['name'] = '';
      $args['isfile'] = false;
      $args['encoding'] = '7bit';
      $args['disposition'] = '';
    }
    return $args;
  }

  /**
   * On attchment removed, if the attachment is a detached file, unshare the link
   * @override
   */
  public function remove($attachment) {
    if ($this->isDetached($attachment)) {
      if ($attachment['options']['uploaded']) {
        try {
          $this->fileHostingClient->unShare($attachment['options']['url']);
        } catch(Exception $e) {
          write_log('errors', $e);
        }
      }
      $attachment['status'] = true;
      return $attachment;
    }  else {
      return parent::remove($attachment);
    }
  }

  /**
   * On loading a mail from a saved source (draft, edit as new), set detached file options
   * @override
   */
  public function save($args) {
    if ($this->isDetached($args)) {
      $data = $this->extractFileData($args);
      $args = array_merge($args, $data);
      if (!isset($args['headers']['X-Mozilla-Cloud-Part'])) {
        $args['headers']['X-Mozilla-Cloud-Part'] = "cloudFile; url=$url'; name=$$args[name]";
      }
      if (!isset($args['headers']['X-BM-Disposition']) || !isset($args['headers']['X-BlueMind-Disposition'])) {
        $args['headers']['X-BM-Disposition'] = "filehosting; url=$url; name=$args[name]";
        if (isset($args['size']) && $args['size'] > 0) {
          $args['headers']['X-BM-Disposition'] .= "; size=$args[size]";
        } elseif ($size = @filesize($args['url'])) {
          $args['headers']['X-BM-Disposition'] .= "; size=$size";
        }
      }
      $args['options']['disposition'] = 'filehosting';
      $args['options']['url'] = $args['path'];
      $args['options']['size'] = $args['size'];
      $args['id'] = $this->file_id(); 
      $args['status'] = true;
      unset($args['data']);
      return $args;
    }
    return parent::save($args);
  }

  /**
   * Replace default upload form with BM one
   */ 
  public function uploadform($attrib) {

    // set defaults
    $attrib += array('id' => 'rcmUploadbox', 'buttons' => 'yes');

    /*
     * Upload buttons
     */
    $attachFromDisk = array(
      'name' => 'plugin.bm_filehosting.browse_computer',    
      'onclick' => 'return rcmail.filehosting_browse_computer($("#' . $attrib['id'] . '"))',      
      'type' => 'link', 
      'class' => 'goog-button-base-first goog-inline-block goog-menu-button goog-button-base btn',
      'classact' => 'goog-button-base-first goog-inline-block goog-menu-button goog-button-base btn',
      'classsel' => 'goog-button-base-first goog-inline-block goog-menu-button goog-button-base  goog-button-base-active btn',
      'title' => $this->gettext('attach'),
      'content' => $this->gettext('browse_local')
    );
    $out = $this->output->button($attachFromDisk);

    $buttons = $this->rcmail->plugins->exec_hook('plugin.bm_filehosting.attach_button',array('local' => null));
    if (!$buttons['local']) {
     $attachFromDisk = array(
        'name' => 'plugin.bm_filehosting.browse_computer',    
        'onclick' => 'return rcmail.filehosting_browse_computer($("#' . $attrib['id'] . '"))',      
        'type' => 'link', 
        'class' => 'goog-inline-block goog-menu-button goog-button-base btn' . ((count($buttons) > 1) ?'goog-button-base-first' : ''),
        'classact' => 'goog-button-base-first goog-inline-block goog-menu-button goog-button-base btn',
        'classsel' => 'goog-button-base-first goog-inline-block goog-menu-button goog-button-base  goog-button-base-active btn',
        'title' => $this->gettext('attach'),
        'content' => $this->gettext('browse_local')
      );      
      $buttons['local'] = $this->output->button($attachFromDisk);
    }
    $attachAFile = html::div('bm-filehosting-attach', implode('', $buttons));

    /*
     * File list
     */

    $fileList = html::div('bm-filehosting-filelist');

    /*
     * File from local drive form fields
     */
    $localFileField = rcmail_compose_attachment_field(array('onchange' => 'rcmail.filehosting_add_files_to_mail(this, $("#' . $attrib['id'] . '"));'));
    $localUpload = html::div(array('class' => 'bm-filehosting-local-upload'), $localFileField);

    $button = new html_inputfield(array('type' => 'button'));
    $content = html::div($attrib,
      $attachAFile . $this->output->form_tag(array('id' => $attrib['id'].'Frm', 'name' => 'uploadform', 'method' => 'post', 'enctype' => 'multipart/form-data'),
      $fileList . $localUpload.
      html::div(array('class'=>'hint')) .
      (get_boolean($attrib['buttons']) ? html::div('buttons',
        $button->show(rcube_label('close'), array('class' => 'button', 'onclick' => "$('#$attrib[id]').hide()")) . ' ' .
        $button->show(rcube_label('upload'), array('class' => 'button mainaction', 'onclick' => JS_OBJECT_NAME . ".command('send-attachment', this.form)"))
      ) : '')
    )
  );

    $attrib['content'] = $content;

    return $attrib;
  }

  /**
   * Replace default attachment button with BM buttons
   */ 
  public function attachmentButton($args) {
    $out = $this->output->button(array(
      'name' => 'plugin.bm_filehosting.upload',
      'onclick' => 'rcmail.filehosting_clear_attachment();UI.show_uploadform();return false',
      'type' => 'input', 
      'class' => 'goog-inline-block goog-menu-button goog-button-base goog-button-base-first',
      'classact' => 'goog-inline-block goog-menu-button goog-button-base goog-button-base-first',
      'classsel' => 'goog-inline-block goog-menu-button goog-button-base goog-button-base-active goog-button-base-first',
      'style' => 'width: 135px;',
      'title' =>  $this->gettext('addattachment'),
      'label' => 'addattachment'
    ));
    $out .= $this->output->button(array(
      'name' => 'plugin.bm_filehosting.upload',
      'onclick' => 'rcmail.filehosting_clear_attachment();UI.show_uploadform(true);return false',      
      'type' => 'link', 
      'class' => 'goog-inline-block goog-menu-button goog-button-base goog-button-base-last fa fa-plus btn',
      'classact' => 'goog-inline-block goog-menu-button goog-button-base goog-button-base-last fa fa-plus btn',
      'classsel' => 'goog-inline-block goog-menu-button goog-button-base goog-button-base-active goog-button-base-last fa fa-plus btn',
      'title' =>  $this->gettext('advancedupload'),
      'content' => '&nbsp;'
    ));

    $out = html::div(array('style' => 'text-align: center; position:relative; left: -9px; top:-9px; white-space:nowrap;'), $out);
    $args['content'] = $out . $args['content'];
    return $args;
  }

  /**
   * Extract remote data from extended headers
   */
  private function extractFileData($attachment) {
    $h = $attachment['headers'];
    $data = array('headers' => $h);
    foreach($attachment['headers'] as $key => $value) {
      if (strtolower($key) == 'x-bm-disposition' || strtolower($key) == 'x-bluemind-disposition' || strtolower($key) == 'x-mozilla-cloud-part') {
        unset($data['headers'][$key]);
        $pairs = explode(';', $value);
        array_shift($pairs);
        foreach($pairs as $pair) {
          list($prop, $val) = explode('=', $pair);
          $prop = trim($prop);
          $val = trim($val);
          switch($prop) {
          case 'name':
            $data[$prop] = $val;
            break;
          case 'size':
            $data[$prop] = (int) $val;
            break;
          case 'url':
            $data['path'] = $val;
            break;
          } 

        }
      }
      if (strtolower($key) == 'x-bm-disposition' || strtolower($key) == 'x-bluemind-disposition') {
        $data['headers']['X-BM-Disposition'] = $value;
      } elseif (strtolower($key) == 'x-mozilla-cloud-part') {
        $data['headers']['X-Mozilla-Cloud-Part'] = $value;
      }
    }
    return $data;
  }

  /** 
   * Is attachment or part a detached file
   */
  private function isDetached($attachment) {
    if (is_string($attachment)) {
      $mime = strtolower($attachment);
      return (strpos($mime, 'x-bm-disposition') !== FALSE || strpos($mime, 'x-bluemind-disposition') !== FALSE || strpos($mime, 'x-mozilla-cloud-part') !== FALSE);
    } else if (!is_array($attachment)) {
      return false;
    } else if ($attachment['options']['disposition'] == 'filehosting'){
      return true;
    } else if ($attachment['disposition'] == 'filehosting'){
      return true;
    } else if (is_array($attachment['headers'])) {
      foreach($attachment['headers'] as $key => $value) {
        if ((strtolower($key) == 'x-bm-disposition' || strtolower($key) == 'x-bluemind-disposition') && strtolower(array_shift(explode(';', $value))) == 'filehosting') {
          return true;
        }
        if (strtolower($key) == 'x-mozilla-cloud-part' && strtolower(array_shift(explode(';', $value))) == 'cloudfile') {
          return true;
        }
      }
      return false;
    }
    return false;
  }


  /**
   * Add an attachment to message composition data,
   * synchronize session data to prevent concurrency overwrite and
   * send command to UI to add an attachment
   */ 
  public static function add_to_attachment($attachment) {
    $compose_id = get_input_value('_id', RCUBE_INPUT_GPC);
    if ($compose_id && $_SESSION['compose_data_'.$compose_id]) {
      $compose =& $_SESSION['compose_data_'.$compose_id];
    }
    if (!$compose) {
      exit;
    }
    $attachment = rcmail::get_instance()->plugins->exec_hook('attachment_upload', $attachment);

    $compose['attachments'][$attachment['id']] = $attachment;

    $id = $attachment['id'];
    $uploadid = 'rcmfile'. $attachment['id'];

    $content = html::span(array(
      'href' => "#delete",
      'onclick' => sprintf("return %s.command('remove-attachment','rcmfile%s', this)", JS_OBJECT_NAME, $id),
      'title' => rcube_label('delete'),
      'class' => 'fa fa-lg fa-trash delete',
    ), '');

    $content .= Q($attachment['name']);
    $output = rcmail::get_instance()->output;
    $output->command('add2attachment_list', "rcmfile$id", array(
      'html' => $content,
      'name' => $attachment['name'],
      'mimetype' => $attachment['mimetype'],
      'classname' => rcmail_filetype2classname($attachment['mimetype'], $attachment['name']),
      'options' => $attachment['options'] ? $attachment['options'] : array(),
      'complete' => true),
    $uploadid);
  }
}

?>
