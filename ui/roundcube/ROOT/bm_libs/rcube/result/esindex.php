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
 * Class for accessing ES SORT/SEARCH/ESEARCH result
 */

class rcube_result_esindex extends rcube_result_index {
    /**
     * Object constructor.
     */
    public function __construct($data = null, $meta = null)  {
	parent::__construct(null, $data);
        $this->meta = $meta;
        $this->order = $meta['ORDER'];
    }

  /**
     * Initializes object with SORT command response
     *
     * @param string $data IMAP response string
     */
    public function init($data = null) {
        if (is_array($data)) {
          $this->raw_data = $data;
        }
    }

    public function count()  {
      if ($this->meta['count'] !== null)
          return $this->meta['count'];
      if (empty($this->raw_data)) {
          $this->meta['count']  = 0;
          $this->meta['length'] = 0;
      }
      else {
          $this->meta['count'] = count($this->raw_data);
      }

      return $this->meta['count'];
    }
    /**
     * Slices data set.
     *
     * @param $offset Offset (as for PHP's array_slice())
     * @param $length Number of elements (as for PHP's array_slice())
     *
     */
    public function slice($offset, $length) {
        $data = $this->get();
        $data = array_slice($data, $offset, $length);

        $this->meta          = array();
        $this->meta['count'] = count($data);
        $this->raw_data      = $data;
    }


    /**
     * Filters data set. Removes elements listed in $ids list.
     *
     * @param array $ids List of IDs to remove.
     */
    public function filter($ids = array())  {
        $data = $this->get();
        $data = array_diff($data, $ids);

        $this->meta          = array();
        $this->meta['count'] = count($data);
        $this->raw_data      = $data;
    }


    /**
     * Filters data set. Removes elements not listed in $ids list.
     *
     * @param array $ids List of IDs to keep.
     */
    public function intersect($ids = array()) {
        $data = $this->get();
        $data = array_intersect($data, $ids);

        $this->meta          = array();
        $this->meta['count'] = count($data);
        $this->raw_data      = $data;
    }



    /**
     * Check if the given message ID exists in the object
     *
     * @param int  $msgid     Message ID
     * @param bool $get_index When enabled element's index will be returned.
     *                        Elements are indexed starting with 0
     *
     * @return mixed False if message ID doesn't exist, True if exists or
     *               index of the element if $get_index=true
     */
    public function exists($msgid, $get_index = false) {
      if (empty($this->raw_data)) {
        return false;
      }
      if ($get_index) {
        return array_search($msgid, $this->raw_data);
      }
      return in_array($msgid, $this->raw_data);
    }

    /**
     * Return all messages in the result.
     *
     * @return array List of message IDs
     */
    public function get() {
      return $this->raw_data;
    }


    /**
     * Return all messages in the result.
     *
     * @return array List of message IDs
     */
    public function get_compressed(){
        if (empty($this->raw_data)) {
            return array();
        }
        return rcube_imap_generic::compressMessageSet($this->raw_data);
    }


    /**
     * Return result element at specified index
     *
     * @param int|string  $index  Element's index or "FIRST" or "LAST"
     *
     * @return int Element value
     */
    public function get_element($index) {
        $count = $this->count();

        if (!$count) {
            return null;
        }

        // first element
        if ($index === 0 || $index === '0' || $index === 'FIRST') {
          return $this->raw_data[0];
        }

        // last element
        if ($index === 'LAST' || $index == $count-1) {
          return $this->raw_data[$count - 1];
        }
        return $this->raw_data[$index];
    }

    /**
     * Reverts order of elements in the result
     */
    public function revert() {
      $this->order = $this->order == 'ASC' ? 'DESC' : 'ASC';

      if (empty($this->raw_data)) {
          return;
      }

      // @TODO: maybe do this in chunks
      $data = $this->get();
      $reverse = array();
      $data = array_reverse($data);
      $this->raw_data = $data;

      $this->meta['pos'] = array();
    }
}
