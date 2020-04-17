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
package net.bluemind.eas.serdes.search;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.search.GAL;
import net.bluemind.eas.dto.search.SearchResponse;
import net.bluemind.eas.dto.search.SearchResult;
import net.bluemind.eas.serdes.AsyncBuildHelper;
import net.bluemind.eas.serdes.AsyncBuildHelper.IBuildOperation;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.base.AirSyncBaseResponseFormatter;
import net.bluemind.eas.serdes.calendar.CalendarResponseFormatter;
import net.bluemind.eas.serdes.contact.ContactResponseFormatter;
import net.bluemind.eas.serdes.documentlibrary.DocumentLibraryResponseFormatter;
import net.bluemind.eas.serdes.email.EmailResponseFormatter;
import net.bluemind.eas.serdes.notes.NotesResponseFormatter;
import net.bluemind.eas.serdes.tasks.TasksResponseFormatter;

public class SearchResponseFormatter implements IEasResponseFormatter<SearchResponse> {

	private static interface LoadResultOp {
		void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync);
	}

	@Override
	public void format(IResponseBuilder builder, final double protocolVersion, final SearchResponse response,
			final Callback<Void> completion) {
		builder.start(NamespaceMapping.Search).text("Status", response.status.xmlValue());

		if (response.store != null) {
			builder.container("Response").container("Store");
			builder.text("Status", response.store.status.xmlValue());

			if (!response.store.results.isEmpty()) {
				Callback<IResponseBuilder> afterBuild = new Callback<IResponseBuilder>() {

					@Override
					public void onResult(IResponseBuilder data) {

						data.text(NamespaceMapping.Search, "Range", response.range.min + "-" + response.range.max);
						data.text(NamespaceMapping.Search, "Total", response.total.toString());
						data.endContainer(); // Response
						data.endContainer(); // Store
						data.end(completion);
					}
				};
				IBuildOperation<SearchResult, IResponseBuilder> resultBuild = new IBuildOperation<SearchResult, IResponseBuilder>() {

					@Override
					public void beforeAsync(IResponseBuilder b, SearchResult result,
							Callback<IResponseBuilder> forAsync) {
						buildOneResult(b, protocolVersion, result, forAsync);
					}

					@Override
					public void afterAsync(IResponseBuilder b, SearchResult t) {
						b.endContainer();// Properties
						b.endContainer();// Result
					}
				};
				AsyncBuildHelper<SearchResult, IResponseBuilder> helper = new AsyncBuildHelper<>(
						response.store.results.iterator(), resultBuild, afterBuild);
				helper.build(builder);
				return;
			} else {
				builder.token("Result");
				builder.endContainer().endContainer();
			}
		}
		builder.end(completion);
	}

	private void buildOneResult(IResponseBuilder builder, final double protocolVersion, final SearchResult result,
			final Callback<IResponseBuilder> forAsync) {
		builder.container(NamespaceMapping.Search, "Result");
		if (result.clazz != null) {
			builder.text(NamespaceMapping.Sync, "Class", result.clazz);
		}
		if (result.longId != null) {
			builder.text(NamespaceMapping.Search, "LongId", result.longId.toString());
		}
		if (result.collectionId != null) {
			builder.text(NamespaceMapping.Sync, "CollectionId", result.collectionId.getValue());
		}

		builder.container(NamespaceMapping.Search, "Properties");

		// Airsync

		Callback<IResponseBuilder> afterAirSyncBase = new Callback<IResponseBuilder>() {

			@Override
			public void onResult(IResponseBuilder data) {
				LoadResultOp loadOp = getOp(protocolVersion, result);
				loadOp.doIt(data, forAsync);
			}
		};

		if (result.searchProperties.airSyncBase != null) {
			AirSyncBaseResponseFormatter airSyncBaseResponderFormatter = new AirSyncBaseResponseFormatter();
			airSyncBaseResponderFormatter.append(builder, protocolVersion, result.searchProperties.airSyncBase,
					afterAirSyncBase);
		} else {
			afterAirSyncBase.onResult(builder);
		}

	}

	private LoadResultOp getOp(final double protocolVersion, final SearchResult result) {
		LoadResultOp loadOp = null;
		// Calendar
		if (result.searchProperties.calendar != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					CalendarResponseFormatter calResponderFormatter = new CalendarResponseFormatter();
					calResponderFormatter.append(builder, protocolVersion, result.searchProperties.calendar, forAsync);
				}
			};
		}

		// Contact
		else if (result.searchProperties.contact != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					ContactResponseFormatter contactResponderFormatter = new ContactResponseFormatter();
					contactResponderFormatter.append(builder, protocolVersion, result.searchProperties.contact,
							forAsync);
				}
			};
		}

		// DirectoryLibrary
		else if (result.searchProperties.documentLibrary != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					DocumentLibraryResponseFormatter documentLibraryResponseFormatter = new DocumentLibraryResponseFormatter();
					documentLibraryResponseFormatter.append(builder, protocolVersion,
							result.searchProperties.documentLibrary, forAsync);
				}
			};
		}

		// Email
		else if (result.searchProperties.email != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					EmailResponseFormatter emailResponseFormatter = new EmailResponseFormatter();
					emailResponseFormatter.append(builder, protocolVersion, result.searchProperties.email, forAsync);
				}
			};
		}

		// Notes
		else if (result.searchProperties.notes != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					NotesResponseFormatter notesResponseFormatter = new NotesResponseFormatter();
					notesResponseFormatter.append(builder, protocolVersion, result.searchProperties.notes, forAsync);
				}
			};
		}

		// Tasks
		else if (result.searchProperties.tasks != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					TasksResponseFormatter tasksResponseFormatter = new TasksResponseFormatter();
					tasksResponseFormatter.append(builder, protocolVersion, result.searchProperties.tasks, forAsync);
				}
			};
		}

		// GAL
		else if (result.searchProperties.gal != null) {
			loadOp = new LoadResultOp() {

				@Override
				public void doIt(IResponseBuilder builder, Callback<IResponseBuilder> forAsync) {
					galResult(builder, protocolVersion, result.searchProperties.gal, forAsync);
				}
			};
		}
		return loadOp;
	}

	private void galResult(IResponseBuilder builder, double protocolVersion, GAL gal, Callback<IResponseBuilder> cb) {
		if (notEmpty(gal.getDisplayName())) {
			builder.text(NamespaceMapping.GAL, "DisplayName", gal.getDisplayName());
		}
		if (notEmpty(gal.phone)) {
			builder.text(NamespaceMapping.GAL, "Phone", gal.phone);
		}
		if (notEmpty(gal.office)) {
			builder.text(NamespaceMapping.GAL, "Office", gal.office);
		}
		if (notEmpty(gal.title)) {
			builder.text(NamespaceMapping.GAL, "Title", gal.title);
		}
		if (notEmpty(gal.company)) {
			builder.text(NamespaceMapping.GAL, "Company", gal.company);
		}
		if (notEmpty(gal.alias)) {
			builder.text(NamespaceMapping.GAL, "Alias", gal.alias);
		}
		if (notEmpty(gal.firstname)) {
			builder.text(NamespaceMapping.GAL, "FirstName", gal.firstname);
		}
		if (gal.lastname != null) {
			builder.text(NamespaceMapping.GAL, "LastName", gal.lastname);
		}
		if (gal.homePhone != null) {
			builder.text(NamespaceMapping.GAL, "HomePhone", gal.homePhone);
		}
		if (gal.mobilePhone != null) {
			builder.text(NamespaceMapping.GAL, "MobilePhone", gal.mobilePhone);
		}
		if (gal.emailAddress != null) {
			builder.text(NamespaceMapping.GAL, "EmailAddress", gal.emailAddress);
		}

		if (protocolVersion > 14.0 && gal.picture != null) {
			builder.container(NamespaceMapping.GAL, "Picture");
			if (gal.picture.status != null) {
				builder.text("Status", gal.picture.status.xmlValue());
			}
			if (gal.picture.data != null) {
				builder.text("Data", gal.picture.data);
			}
			builder.endContainer();
		}
		cb.onResult(builder);
	}

	private boolean notEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}

}
