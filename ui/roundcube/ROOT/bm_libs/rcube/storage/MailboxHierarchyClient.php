<?php

/* BEGIN LICENSE
 * Copyright Â© Blue Mind SAS, 2012-2016
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

/**
 * Custom implementation of net.bluemind.mailbox.api.IMailboxFolderHierarchy.
 */

class MailboxHierarchyClient { 

  protected $base;
  protected $sid;
  public $mailboxUid;

  /*
   * Constructor.
   *
   * @param base
   * @param mailboxUid
   *
   */
  public function __construct($base, $sid , $mailboxUid) {
    $this->sid = $sid;
    $this->base = $base."/api/mailbox-folders/{mailboxUid}";
    $this->mailboxUid = $mailboxUid;
    $this->base = str_replace("{mailboxUid}", $mailboxUid, $this->base);
  }



  /*
   * Execute the request
   *
   * @param url
   * @param data
   * @param body
   */
  private function execute($url, $queryParam, $body, $method) {

    $curl = curl_init();
    
    $headers = array();
    array_push($headers, 'X-BM-ApiKey: '.$this->sid);
  
    if (sizeof($queryParam) > 0) {
      $url .= '?'.http_build_query($queryParam);
    }
  
    curl_setopt_array($curl, array(
      CURL_HTTP_VERSION_1_1 => TRUE,
      CURLOPT_URL => $url,
      CURLOPT_RETURNTRANSFER => TRUE,
      CURLOPT_ENCODING  => "deflate",
      CURLOPT_MAXREDIRS => 5,
      CURLOPT_FOLLOWLOCATION => TRUE,
      CURLOPT_CONNECTTIMEOUT => 10,
      CURLOPT_SSL_VERIFYPEER => TRUE,
      CURLOPT_USERAGENT => "BlueMind PHP Client",
      CURLOPT_HTTPHEADER => $headers,
      CURLOPT_CUSTOMREQUEST => $method)
    );
    
  
    if ($method == 'POST') {
      curl_setopt($curl, CURLOPT_POST, TRUE);
      curl_setopt($curl, CURLOPT_POSTFIELDS, array());
    } else if ($method == 'PUT') {
      if(is_object($body)) {
        curl_setopt($curl, CURLOPT_CUSTOMREQUEST, "PUT");
      } else if (is_string($body)){
        curl_setopt($curl, CURLOPT_PUT, TRUE);
      }
    }
    
    if (is_object($body) && method_exists($body, 'serialize')) {
      $body = $body->serialize();
      $size = strlen($body);
      
      array_push($headers, 'Content-Type: application/json');
      array_push($headers, 'Content-Length: '.$size);
      curl_setopt($curl, CURLOPT_HTTPHEADER, $headers);
      
      curl_setopt($curl, CURLOPT_POSTFIELDS, $body);
    } else if (is_string($body)) {
      $size = strlen($body);
      $f = tmpfile();
      fwrite($f, $body);
      fseek($f, 0);
      curl_setopt($curl, CURLOPT_INFILE, $f);
      curl_setopt($curl, CURLOPT_INFILESIZE, $size);
    }
  
    $resp = curl_exec($curl);
    curl_close($curl);
    if (!$resp) {
      return;
    } else if ($js = json_decode($resp)) {
      return $js;
    } else {
      return $resp;
    }
  }

}
