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
package net.bluemind.ui.imageupload.client;

import com.google.code.gwt.crop.client.GWTCropper;
import com.google.code.gwt.crop.client.GWTCropperPreview;
import com.google.code.gwt.crop.client.common.Dimension;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;

import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.ui.common.client.forms.DoneCancelActionBar;

public class ImageUpload extends DialogBox {

	interface ImageUploadUiBinder extends UiBinder<DockLayoutPanel, ImageUpload> {

	}

	private static ImageUploadUiBinder uiBinder = GWT.create(ImageUploadUiBinder.class);

	@UiField
	FlowPanel imagePanel;

	@UiField
	FlowPanel previewPanel;

	@UiField
	FlowPanel uploadPanel;

	@UiField
	FlowPanel imageUploadPanel;

	@UiField
	FormPanel imageUploadForm;

	@UiField
	FileUpload imageUpload;

	@UiField
	Button deleteButton;

	@UiField
	Anchor changePhoto;

	@UiField
	DoneCancelActionBar actionBar;
	private String uuid;

	@UiField(provided = true)
	GWTCropperPreview preview;

	private ImageUploadHandler result;

	private GWTCropper cropper;

	private DockLayoutPanel panel;

	private int imageMaxHeight;

	private int imageMaxWidth;

	public ImageUpload(String currentImage, ImageUploadHandler resHandler) {

		preview = new GWTCropperPreview(Dimension.HEIGHT, 150);
		panel = uiBinder.createAndBindUi(this);
		panel.setStyleName("dialog");
		actionBar.setDoneAction(new ScheduledCommand() {

			@Override
			public void execute() {
				okClicked(null);
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				cancelClicked(null);
			}
		});

		panel.setWidth("800px");
		panel.setHeight("400px");

		// setWidth(w + "px");
		// setHeight(h + "px");
		setWidget(panel);
		setGlassEnabled(true);
		setAutoHideEnabled(true);
		setGlassStyleName("modalOverlay");
		setModal(true);

		center();
		setupUpload();
		this.result = resHandler;

		imagePanel.setVisible(false);

		if (currentImage != null) {
			preview.add(new Image(currentImage));
			previewPanel.setVisible(true);
			imageUploadPanel.getElement().getStyle().setProperty("left", "0");
		} else {
			previewPanel.setVisible(false);
			deleteButton.setVisible(false);
			imageUploadPanel.getElement().getStyle().setProperty("left", "-200px");
		}
	}

	private void setupUpload() {
		uploadPanel.addDomHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (cropper == null) {
					imageUpload.click();
				}
			}
		}, ClickEvent.getType());

		changePhoto.addDomHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				imageUpload.click();
			}
		}, ClickEvent.getType());

		imageUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		imageUploadForm.setMethod(FormPanel.METHOD_POST);
		imageUploadForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {

				imageUploadForm.reset();
				imageUploadPanel.setVisible(false);
				imagePanel.setVisible(true);
				previewPanel.setVisible(true);
				imageUploadPanel.getElement().getStyle().setProperty("left", "0");

				uuid = new InlineHTML(event.getResults()).getText();
				preview.clear();
				if (cropper != null) {
					cropper.removeFromParent();
				}
				cropper = new GWTCropper("image/tmpupload?uuid=" + uuid);
				cropper.setAspectRatio(1.0);
				cropper.registerPreviewWidget(preview);
				imagePanel.add(cropper);
			}
		});

		imageUpload.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {

				imageMaxWidth = uploadPanel.getOffsetWidth();
				imageMaxHeight = uploadPanel.getOffsetHeight() - 30;
				imageUploadForm.setAction("image/tmpupload?width=" + imageMaxWidth + "&height=" + imageMaxHeight);
				imageUploadForm.submit();
			}
		});
	}

	public static void show(String currentImage, ImageUploadHandler resHandler) {
		final ImageUpload iu = new ImageUpload(currentImage, resHandler);
		iu.center();
		iu.show();
	}

	public void okClicked(ClickEvent e) {
		hide();

		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,
				"image/tmpcrop?uuid=" + uuid + "&x=" + cropper.getSelectionXCoordinate() + "&y="
						+ cropper.getSelectionYCoordinate() + "&w=" + cropper.getSelectionWidth() + "&h="
						+ cropper.getSelectionHeight() + "&sw=120&sh=132");
		builder.setCallback(new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				if (response.getStatusCode() == 200) {
					result.newImage(response.getText());
				} else {
					result.failure(new Exception(response.getStatusText()));
				}

			}

			@Override
			public void onError(Request request, Throwable exception) {
				result.failure(exception);
			}
		});
		try {
			builder.send();
		} catch (RequestException exception) {
			result.failure(exception);
		}
	}

	public void cancelClicked(ClickEvent e) {
		hide();
		result.cancel();
	}

	@UiHandler("deleteButton")
	public void deleteClicked(ClickEvent e) {
		hide();
		result.deleteCurrent();
	}

	public static void exportFunction() {
		JsHelper.createPackage("net.bluemind.ui");
		exportShow();
	}

	private static native void exportShow()
	/*-{
    $wnd['net']['bluemind']['ui']['uploadimage'] = function(existingImage, newImageCallback, cancelCallback,
        deleteImageCallback, errorCallback) {
      var handler = @net.bluemind.ui.imageupload.client.JsImageUploadHandler::new(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(newImageCallback, cancelCallback, deleteImageCallback, errorCallback);
      @net.bluemind.ui.imageupload.client.ImageUpload::show(Ljava/lang/String;Lnet/bluemind/ui/imageupload/client/ImageUploadHandler;)(existingImage,handler);

    };
	}-*/;
}
