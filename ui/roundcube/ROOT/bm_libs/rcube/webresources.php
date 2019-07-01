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

class WebResourcesService {

  private $baseUrl;

  public function __construct($host, $lang) {
    $this->baseUrl = 'http://' . $host . ':8080/module-webresources?module=/webmail';
    $this->lang = $lang;
  }

  public function get() {
    $curl = curl_init();

    $headers = array();
    array_push($headers, 'BMLANG: '.$this->lang);

    curl_setopt_array($curl, array(
      CURL_HTTP_VERSION_1_1 => TRUE,
      CURLOPT_URL => $this->baseUrl,
      CURLOPT_RETURNTRANSFER => TRUE,
      CURLOPT_ENCODING  => "deflate",
      CURLOPT_MAXREDIRS => 5,
      CURLOPT_FOLLOWLOCATION => TRUE,
      CURLOPT_CONNECTTIMEOUT => 10,
      CURLOPT_SSL_VERIFYPEER => FALSE,
      CURLOPT_SSL_VERIFYHOST, FALSE,
      CURLOPT_USERAGENT => "BlueMind PHP Client",
      CURLOPT_HTTPHEADER => $headers,
      CURLOPT_CUSTOMREQUEST => "GET")
    );
    $resp = curl_exec($curl);
  
    curl_close($curl);

    return json_decode($resp);
  }
}
