/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.api;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import net.bluemind.core.api.BMApi;

/**
 * A fetched data is a data initially generated from a raw data. A fetched data
 * can contain some data pieces giving a more detailed version of this main
 * fetched data (e.g. the main fetched data could be a date (dd-mm-yyyy), and
 * its data pieces could separate the day, month and year (dd, yy and yyyy).
 * Usually used for technical data.
 */
@BMApi(version = "3")
public class FetchedData {

	/**
	 * The title given to the fetched data.
	 */
	public String title;

	/**
	 * The data itself (usually a single line or a single value).
	 */
	public String data;

	/**
	 * The list of data pieces held by the fetched data. Can be null.
	 */
	public List<FetchedData> dataPieces;

	public FetchedData() {
	}

	/**
	 * Creates an untitled {@link FetchedData}.
	 * 
	 * @param data the data to be added
	 * 
	 */
	public FetchedData(String data) {
		this.data = data;
	}

	/**
	 * Creates a new {@link FetchedData} with a specified title and a data.
	 * 
	 * @param title the title to be given
	 * @param data  the data to be given
	 */
	public FetchedData(String title, String data) {
		this.title = title;
		this.data = data;
	}

	/**
	 * Adds a new data piece to the fetched data. Prefer this method over
	 * getDataPieces().add(...) because the dataPieces field will be instantiated if
	 * not already contrary to the previously mentioned method.
	 */
	public void addDataPiece(FetchedData dataPiece) {
		if (this.dataPieces == null) {
			this.dataPieces = new ArrayList<FetchedData>();
		}

		if (dataPiece != null) {
			this.dataPieces.add(dataPiece);
		}
	}

	/**
	 * Fetches a data piece from its title. If there are several data pieces having
	 * the same name, this method will return the first data piece found (no control
	 * is operated).
	 * 
	 * @param title the title of the data piece to be fetched
	 * @return the data piece with the given title
	 */
	public FetchedData getDataPieceByTitle(String title) throws NoSuchElementException {

		if (!this.dataPieces.isEmpty()) {
			for (FetchedData data : this.dataPieces) {
				if (data.title.equals(title)) {
					return data;
				}
			}
		}
		throw new NoSuchElementException("FetchedData.getDataPieceByTitle()");
	}

}
