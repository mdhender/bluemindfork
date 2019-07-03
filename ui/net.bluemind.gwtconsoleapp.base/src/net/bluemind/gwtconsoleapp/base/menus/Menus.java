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
package net.bluemind.gwtconsoleapp.base.menus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.editor.Ajax;

public class Menus {

	private Map<String, Section> sectionsIndex = new HashMap<>();
	private JsArray<Section> root;
	private List<Screen> screens = new ArrayList<>();

	public Menus(String extensionPointId) {
		buildMenus(extensionPointId);
	}

	public JsArray<Section> getRoot() {
		return root;
	}

	private void buildMenus(String extensionPointId) {
		List<MenuContributor> contributors = MenuContributors.getContributors(extensionPointId);

		List<Contributed<Section>> csections = new LinkedList<>();
		List<Contributed<Screen>> cscreens = new LinkedList<>();
		for (MenuContributor c : contributors) {
			MenuContribution contribution = c.contribution();
			JsArray<Contributed<Section>> cSections = contribution.getSections();
			for (int i = 0; i < cSections.length(); i++) {
				csections.add(cSections.get(i));
			}

		}

		for (MenuContributor c : contributors) {
			JsArray<Contributed<Screen>> screen = c.contribution().getScreens();
			for (int i = 0; i < screen.length(); i++) {
				cscreens.add(screen.get(i));
			}
		}

		buildSections(csections);
		buildScreens(cscreens);
	}

	private void buildScreens(List<Contributed<Screen>> cscreens) {
		for (int i = 0; i < cscreens.size(); i++) {
			addScreens(root, cscreens.get(i));
		}
	}

	private void buildSections(List<Contributed<Section>> contributions) {
		JsArray<Section> sections = JsArray.createArray().cast();

		int last = 0;

		GWT.log("build screens...");
		while (!contributions.isEmpty() && last != contributions.size()) {
			GWT.log("build screens contrib " + contributions.size() + " last " + last);
			last = contributions.size();
			for (Iterator<Contributed<Section>> it = contributions.iterator(); it.hasNext();) {
				Contributed<Section> contribution = it.next();
				Section cs = contribution.getContribution();

				if (contribution.getParentId() == null) {
					// root
					if (cs.getId() != null) {
						sectionsIndex.put(cs.getId(), cs);
					}

					sections.push(contribution.getContribution());
					it.remove();

					JsArray<Section> ss = cs.getSections();
					for (int i = 0; i < ss.length(); i++) {
						String id = ss.get(i).getId();
						if (id != null) {
							sectionsIndex.put(id, ss.get(i));
						}
					}
				} else {

					Section contributed = sectionsIndex.get(contribution.getParentId());

					if (contributed != null) {
						contributed.getSections().push(cs);
						it.remove();
					}

					if (cs.getId() != null) {
						sectionsIndex.put(cs.getId(), cs);
					}
					JsArray<Section> ss = cs.getSections();
					for (int i = 0; i < ss.length(); i++) {
						String id = ss.get(i).getId();
						if (id != null) {
							sectionsIndex.put(id, ss.get(i));
						}
					}
				}
			}
		}

		if (contributions.size() > 0) {
			for (Contributed<Section> c : contributions) {
				GWT.log("not found " + c.getParentId() + " id " + c.getContribution().getId());
			}
		}
		GWT.log("build screens contrib " + contributions.size() + " last " + last);

		this.root = sort(sections);

	}

	public static boolean isInRoles(JsArrayString roles) {
		if (roles == null || roles.length() == 0) {
			return true;
		} else {
			for (int i = 0; i < roles.length(); i++) {
				if (Ajax.TOKEN.getRoles().contains(roles.get(i))) {
					return true;
				}
			}
		}
		return false;
	}

	private JsArray<Section> sort(JsArray<Section> sections) {
		if (sections == null) {
			return null;
		}

		List<Section> secs = new ArrayList<>(sections.length());
		for (int i = 0; i < sections.length(); i++) {
			secs.add(sort(sections.get(i)));
		}

		Collections.sort(secs, new Comparator<Section>() {

			@Override
			public int compare(Section o1, Section o2) {
				return o2.getPriority() - o1.getPriority();
			}
		});

		JsArray<Section> ret = JsArray.createArray().cast();
		for (Section s : secs) {
			ret.push(s);
		}
		return ret;
	}

	private Section sort(Section section) {
		JsArray<Section> sortedSecs = sort(section.getSections());

		Section ret = Section.create(section.getId(), section.getName(), section.getPriority(), section.getIconStyle(),
				section.getScreens(), sortedSecs);
		ret.setRoles(section.getRoles());
		return ret;
	}

	private void addSections(JsArray<Section> sections, Contributed<Section> contributed) {

		if (contributed.getParentId() == null) {
			Section contribution = contributed.getContribution();
			sectionsIndex.put(contribution.getId(), contribution);
			sections.push(contribution);
			return;
		}

		Section parentSection = sectionsIndex.get(contributed.getParentId());
		if (parentSection == null) {
			GWT.log("no section found for " + contributed.getParentId() + " sectionIds [" + sectionsIndex.keySet()
					+ "]");
			return;
		}

		GWT.log("*******contriubte MENU to " + contributed.getParentId());
		Section contribution = contributed.getContribution();
		sectionsIndex.put(contribution.getId(), contribution);
		parentSection.getSections().push(contribution);

		if (contribution.getSections() != null) {
			JsArray<Section> ss = contribution.getSections();
			for (int i = 0; i < ss.length(); i++) {
				String id = ss.get(i).getId();
				if (id != null) {
					sectionsIndex.put(id, ss.get(i));
				}
			}
		}
	}

	private void addScreens(JsArray<Section> sections, Contributed<Screen> contributed) {
		Section parentSection = sectionsIndex.get(contributed.getParentId());
		if (parentSection == null) {
			screens.add(contributed.getContribution());
			GWT.log("no section found for " + contributed.getParentId() + " sectionIds [" + sectionsIndex.keySet()
					+ "]");
			return;
		}

		Screen contribution = contributed.getContribution();
		parentSection.getScreens().push(contribution);
	}

	public List<Section> getRootAsList() {
		List<Section> ret = new ArrayList<>();

		for (int i = 0; i < root.length(); i++) {
			ret.add(root.get(i));
		}
		return ret;
	}

	public List<Screen> getRootScreens() {
		return screens;
	}

	public Section getSectionById(String id) {
		return sectionsIndex.get(id);
	}
}
