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

class bm_attachment extends rcube_plugin { 

  const WIDTH = 100;

  private $rcmail;

  function init() {
    $this->rcmail = rcmail::get_instance();
    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $this->include_script('bm_attachment.js');
      $this->include_stylesheet($this->local_skin_path() . "/style.css");
      $_SESSION['bm']['attachment']['filesize'] = parse_bytes(ini_get('upload_max_filesize')) / 1.33;
      $this->rcmail->output->set_env('max_encoded_filesize', $_SESSION['bm']['attachment']['filesize']);
    }
    $this->register_action('plugin.bm_attachment.download', array($this, 'download'));
    $this->register_action('plugin.bm_attachment.preview', array($this, 'preview'));
    $this->add_hook('attachment_delete', array($this, 'attachment_delete'));
    $this->add_hook('attachments_cleanup', array($this, 'attachments_cleanup'));
    $this->add_hook('attachment_upload', array($this, 'attachment_upload'));
    $this->add_hook('attachment_save', array($this, 'attachment_save'));
    $this->add_hook('session_destroy', array($this, 'clearcache'));
    $this->add_hook('template_object_composeattachmentlist', array($this, 'attachmentSizeBar'));    
    $this->add_texts('localization', true);
  }
  function attachment_delete($args) {
    $this->sanitize();
    return $this->uncache($args);
  }
  function attachments_cleanup($args) {
    return $this->clearcache();
  }
  function attachment_upload($args) {
    $this->sanitize();
    return $this->upload($args);
  }
  function attachment_save($args) {
    return $this->save($args);
  }

  function sanitize() {
    $COMPOSE_ID = get_input_value('_id', RCUBE_INPUT_GPC);

    $COMPOSE =& $_SESSION['compose_data_'.$COMPOSE_ID];
    if (!$COMPOSE) {
      return;
    }

    $GLOBALS['thread'] = uniqid();
    $this->rcmail->session->lock(session_id());
    
    if (!is_array($COMPOSE['attachments'])) {
      $COMPOSE['attachments'] = array();
    }
    
    // First check if another process is writing on 
    $buffer = $_SESSION;
    session_decode($this->rcmail->session->mc_read(session_id()));
    $session = $_SESSION;
    $_SESSION = $buffer;
    $attachments = $session['compose_data_'.$COMPOSE_ID]['attachments'];
    
    if ($attachments) {
      foreach($attachments as $id => $att) {
        if (!$COMPOSE['attachments'][$id]) {
          $COMPOSE['attachments'][$id] = $att;
        }
      }
    }
  }


  function thumbnail($file) {
    if (file_exists($file['path'])) {
      $ext = pathinfo($file['name'], PATHINFO_EXTENSION);
      if (extension_loaded('imagick')) {
        $this->getImagickThumbnail($file['path'], $ext);
      } else {
        $this->getGdThumbnail($file['path'], $ext);
      } 
    }
  }

  function getEmptyThumbnail() {
    header('Content-type: image/gif');
    echo base64_decode('R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7');
  }
 
  function getImagickThumbnail($file, $ext) {
    try {
      if (count(Imagick::queryFormats(strtoupper($ext))) > 0) {
        $image = new Imagick($file."[0]");
        $image->setFormat("jpg");
        $image->scaleImage(self::WIDTH,0); 
	$image->setResolution(36, 36);
        header('Content-Type: image/jpeg');
        echo $image;
      } else {
        $this->getEmptyThumbnail();
      }
    } catch (Exception $e) {
      $this->getEmptyThumbnail();
    } 
  }

  function getGdThumbnail($file, $ext) {
    switch(strtolower($ext)){
      case "png":
        $src = imagecreatefrompng($file);
        break;
      case "jpg":
      case "jpeg":
        $src = imagecreatefromjpeg($file);
        break;
      case "wbmp":
        $src = imagecreatefromwbmp($file);
        break;
      case "gif":
        $src = imagecreatefromgif($file);
        break;
      case "xpm":
        $src = imagecreatefromxpm($file);
        break;
      case "xbm":
        $src = imagecreatefromxbm($file);
        break;
      case "gd2":
        $src = imagecreatefromgd2($file);
        break;
      case "gd":
        $src = imagecreatefromgd($file);
        break;
      default:
        $this->getEmptyThumbnail();
        return;
    }
    list($width, $height) = getimagesize($file);
    $r = $height / $width;
    $w = self::WIDTH;
    $h = $r * $w;

    $dst = imagecreatetruecolor($w, $h);
    imagecopyresampled($dst, $src, 0, 0, 0, 0, $w, $h, $width, $height);

    header('Content-Type: image/jpeg');
    imagejpeg($dst, NULL, 50);
  }

  function download() {
    $this->sanitize();
    $id = 'compose_data_' . get_input_value('_id', RCUBE_INPUT_GPC);
    $file =  get_input_value('_file', RCUBE_INPUT_GET);
    $attachment = $_SESSION[$id]['attachments'][$file];

    header('Content-Type: application/octet-stream');
    header('Content-Transfer-Encoding: binary');
    if (file_exists($attachment['path'])) {
      if ($this->rcmail->browser->firefox) {
	      header('Content-Disposition: attachment; filename*="utf8\'\'' . $attachment['name'] . '"');
      } elseif ($this->rcmail->browser->ie) {
        $encoded = rawurlencode($attachment['name']);
        header('Content-Disposition: attachment; filename="' . $encoded . '"'); 
      } else {
	      header('Content-Disposition: attachment; filename="' . $attachment['name'] . '"');
      }
      header('Content-Length: ' . filesize( $attachment['path'] ));
      header('Pragma: private');
      header("Cache-Control: private, must-revalidate");
      ob_clean();
      flush();
      readfile($attachment['path']);
    }
    exit;
  }

  function preview() {
    $this->sanitize();
    $id = 'compose_data_' . get_input_value('_id', RCUBE_INPUT_GPC);
    $file =  get_input_value('_file', RCUBE_INPUT_GET);
    $attachment = $_SESSION[$id]['attachments'][$file];
    if (($tmp = $_SESSION['bm']['attachment'][$file]) && file_exists($tmp)) {
      readfile($_SESSION['bm']['attachment'][$file]);      
    } else if (file_exists($attachment['path'])) {
      $this->thumbnail($attachment);
      $content = ob_get_contents();
      $this->cache($file, $content);
    }
    exit;
  }

  function cache($name, $data) {
    // use common temp dir for file uploads
    $temp_dir = $this->rcmail->config->get('temp_dir');
    $tmp_file = tempnam($temp_dir, 'rcmAttPrev');
    if ($fp = fopen($tmp_file, 'w')) {
      fwrite($fp, $data);
      fclose($fp);
      $_SESSION['bm']['attachment'][$name] = $tmp_file;
    } 
  }

  function uncache($attachment) {
    $id = $attachment['id'];
    if (isset($_SESSION['bm']['attachment'][$id])) {
      if(file_exists($_SESSION['bm']['attachment'][$id])){
        unlink($_SESSION['bm']['attachment'][$id]);
      }
      unset($_SESSION['bm']['attachment'][$id]);
    }
    return $attachment;
  }

  function clearcache() {
    if (is_array($_SESSION['bm']['attachment'])){
      foreach ($_SESSION['bm']['attachment'] as $id => $file) {
        if(file_exists($file)){
          unlink($file);
        }
      }
    }
  }

  function attachmentSizeBar($args) {
    $out = html::div(array('class' => 'progress'), html::div(array('class' => 'progress-text'), '0/' . show_bytes($_SESSION['bm']['attachment']['filesize'])) . html::div(array('class' => 'bar'), html::div(array('class' => 'progress-text'))));
    $args['content'] = $out . $args['content'];
    return $args;    
  }

  /**
   * On loading a mail from a saved source (draft, edit as new), set detached file options
   * @override
   */
  public function save($args) {
    if (!$args['options']['size'] && $args['size']) {
      $args['options']['size'] = $args['size'];
    }
    return $args;
  }


  /**
   * On loading a mail from a saved source (draft, edit as new), set detached file options
   * @override
   */
  public function upload($args) {
    if (!$args['options']['size'] && $args['size']) {
      $args['options']['size'] = $args['size'];
    }    
    return $args;
  }
}

?>
