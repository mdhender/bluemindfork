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
package net.bluemind.ui.adminconsole.progress.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.common.client.SizeHint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.ProgressBar;

/**
 * Starts a progress screen overlay watching the taskref in the
 * <code>task</code> parameter. If successful <code>success</code> screen is
 * shown, if not <code>return</code> screen is shown.
 * 
 * All parameters + the result from task execution (except task, return, success
 * and pictures) are sent to the next screen.
 * 
 *
 */
public class ProgressScreen extends Composite implements IGwtScreenRoot {

	private String taskId;
	private String returnScreen;
	private DockLayoutPanel dlp;
	private static final Set<String> knownParams = known();

	@UiField
	SimplePanel taskOutput;

	@UiField
	ProgressBar progress;

	@UiField
	Button seeLogs;

	@UiField
	Image logo;

	private int progressVal;
	private String successScreen;
	private String resultJson;
	private Map<String, String> resultRequest;
	private List<ImageResource> pictures;
	private Integer picturesIndex;
	private boolean isShowLogs;
	private final int SMALL_HEIGHT = 100;
	private final int NORMAL_HEIGHT = 320;
	private final int ROTATION_TIME = 15; // seconds
	private ScreenRoot instance;
	private static Map<String, String> resultRegistry = new HashMap<>();

	private static ProgressScreenUiBinder uib = GWT.create(ProgressScreenUiBinder.class);

	interface ProgressScreenUiBinder extends UiBinder<DockLayoutPanel, ProgressScreen> {

	}

	public ProgressScreen(ScreenRoot instance) {
		this.instance = instance;
		this.dlp = uib.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");
		this.progressVal = 0;
		setProgress(progressVal);
		isShowLogs = false;
		taskOutput.setVisible(false);
		pictures = new LinkedList<ImageResource>();
		picturesIndex = 0;
		// Change picture every ROTATION_TIME seconds. To set pictures, add a
		// list of "ImageResource" as a ssr parameter called "pictures"
		new Timer() {

			@Override
			public void run() {
				if (pictures.size() > 1) {
					int index = (++picturesIndex) % pictures.size();
					logo.setResource(pictures.get(index));
					if (!isShowLogs) {
						setHeight(SMALL_HEIGHT + logo.getHeight() + "px");
					}
				}
			}
		}.scheduleRepeating(ROTATION_TIME * 1000);
	}

	/**
	 * @return
	 */
	private static Set<String> known() {
		HashSet<String> ret = new HashSet<>();
		ret.add("return");
		ret.add("success");
		ret.add("pictures");
		ret.add("task");
		return ret;
	}

	@UiHandler("seeLogs")
	void seeLogs(ClickEvent ce) {
		isShowLogs = !isShowLogs;
		logo.setVisible(!isShowLogs);
		taskOutput.setVisible(isShowLogs);
		seeLogs.setText(isShowLogs ? ProgressTexts.INST.hideLogs() : ProgressTexts.INST.showLogs());
		setHeight((isShowLogs ? NORMAL_HEIGHT : SMALL_HEIGHT + logo.getHeight()) + "px");
	}

	public void setProgress(int percent) {
		int value = Math.abs(percent);
		value = Math.min(100, percent);
		progress.setProgressPercent(value);
	}

	/**
	 * Add server lines to the output area.
	 * 
	 * Line starting with <code>#progress XX</code> are interpreted as progress
	 * informations. XX is parsed as an integer and used to update the progress
	 * bar width.
	 * 
	 * @param lines
	 */
	public void addOutput(List<String> lines) {
		boolean progressUpdated = false;
		SafeHtmlBuilder html = new SafeHtmlBuilder();
		for (String l : lines) {
			// GWT.log("server: " + l);
			if (l == null) {
				continue;
			}
			if (l.startsWith("#progress ")) {
				progressUpdated = true;
				progressVal = Integer.parseInt(l.substring("#progress ".length()));
			} else {
				html.appendEscaped(l);
				html.appendHtmlConstant("<br/>");
			}
		}
		taskOutput.getElement().setInnerHTML(taskOutput.getElement().getInnerHTML() + html.toSafeHtml().asString());
		scrollToBottom();
		if (!progressUpdated) {
			progressVal++;
		}
		setProgress(progressVal);
	}

	/**
	 * Scroll to the bottom of this panel.
	 */
	private void scrollToBottom() {
		setScrollPosition(taskOutput.getElement().getPropertyInt("scrollHeight"));
	}

	private void setScrollPosition(int position) {
		taskOutput.getElement().setPropertyInt("scrollTop", position);
	}

	public static void putValue(String key, String value) {
		resultRegistry.put(key, value);
	}

	public static String getResult(String id) {
		return resultRegistry.remove(id);
	}

	public static boolean hasResult(String key) {
		return resultRegistry.containsKey(key);
	}

	private void close() {
		// getOverlayScreen().hide(true);
		if (resultJson != null) {
			resultRequest.put("resultId", taskId);
			resultRegistry.put(taskId, resultJson);
			GWT.log("successScreen => " + successScreen);
			Actions.get().showWithParams2(successScreen, resultRequest);
		} else {
			GWT.log("returnScreen => " + returnScreen);
			Actions.get().showWithParams2(returnScreen, resultRequest);
		}
	}

	@SuppressWarnings("unchecked")
	protected void onScreenShown(ScreenShowRequest ssr) {
		this.returnScreen = (String) ssr.get("return");
		if (returnScreen == null) {
			returnScreen = "root";
		}
		this.successScreen = (String) ssr.get("success");
		if (successScreen == null) {
			successScreen = "root";
		}
		this.resultRequest = new HashMap<String, String>();
		if (ssr.get("pictures") instanceof List) {
			this.pictures.addAll((List<ImageResource>) ssr.get("pictures"));
			logo.setResource(this.pictures.get(picturesIndex));
		}
		setHeight(SMALL_HEIGHT + logo.getHeight() + "px");
		for (String s : ssr.keySet()) {
			if (!knownParams.contains(s)) {
				GWT.log("Propagating param " + s);
				resultRequest.put(s, (String) ssr.get(s));
			}
		}

		this.taskId = (String) ssr.get("task");
		if (taskId == null) {
			GWT.log("Missing taskref in request");
			Actions.get().showWithParams2(returnScreen, resultRequest);
		} else {
			startProgressTimer();
		}

	}

	private void startProgressTimer() {
		GWT.log("start progress timer for " + taskId);
		TaskGwtEndpoint taskApi = new TaskGwtEndpoint(Ajax.TOKEN.getSessionId(), taskId);
		ProgressHandler ph = new ProgressHandler(this, taskApi);
		taskApi.status(ph);
	}

	public SizeHint getSizeHint() {
		return new SizeHint(700, 320);
	}

	public boolean isOverlayDisplay() {
		return true;
	}

	public void setTaskFinished(boolean b, String resultJson) {
		this.resultJson = resultJson;
		close();
	}

	@UiFactory
	ProgressTexts getTexts() {
		return ProgressTexts.INST;
	}

	@Override
	public void attach(Element e) {
		GWT.log("progressScreen on attach");
		DOM.appendChild(e, getElement());
		JsMapStringString request = instance.getState();
		JsArrayString keus = request.keys();
		ScreenShowRequest asSSR = new ScreenShowRequest();
		for (int i = 0; i < keus.length(); i++) {
			String k = keus.get(i);
			String v = request.get(k);
			asSSR.put(k, v);
			GWT.log(" * '" + k + "' => '" + v + "'");
		}
		onScreenShown(asSSR);
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doLoad(ScreenRoot instance) {
		// TODO Auto-generated method stub

	}

	public static void registerType() {
		GwtScreenRoot.register("bm.ac.Progress", new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new ProgressScreen(screenRoot);
			}
		});

	}

}
