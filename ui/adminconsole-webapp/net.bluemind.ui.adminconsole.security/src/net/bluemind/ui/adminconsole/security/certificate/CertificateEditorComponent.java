package net.bluemind.ui.adminconsole.security.certificate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;

import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.gwt.endpoint.SecurityMgmtGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.common.client.forms.Ajax;

public class CertificateEditorComponent extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.CertificateEditorComponent";

	private String keyData;

	@UiField
	CheckBox keyFilePresent;

	@UiField
	FormPanel keyUploadForm;

	@UiField
	FileUpload keyFileUpload;

	private String caData;

	@UiField
	CheckBox caFilePresent;

	@UiField
	FormPanel caUploadForm;

	@UiField
	FileUpload caFileUpload;

	private String certData;

	@UiField
	CheckBox certFilePresent;

	@UiField
	FormPanel certUploadForm;

	@UiField
	FileUpload certFileUpload;

	private static CertificateEditorComponentUiBinder uiBinder = GWT.create(CertificateEditorComponentUiBinder.class);

	interface CertificateEditorComponentUiBinder extends UiBinder<HTMLPanel, CertificateEditorComponent> {
	}

	public CertificateEditorComponent() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public void enableCheckBoxes(boolean enable) {
		keyFilePresent.setEnabled(false);
		caFilePresent.setEnabled(false);
		certFilePresent.setEnabled(false);
	}

	public void saveCertificate(String domainUid, boolean securityContext) {
		CertData certificateValues = new CertData();
		certificateValues.certificate = certData;
		certificateValues.certificateAuthority = caData;
		certificateValues.privateKey = keyData;
		certificateValues.domainUid = domainUid;

		createSecurityMgmtGwtEndpoint(certificateValues, securityContext);
	}

	private static void createSecurityMgmtGwtEndpoint(CertData certificateValues, boolean securityContext) {
		new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId()).updateCertificate(certificateValues,
				new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Notification.get().reportInfo("Certificate has been imported");
						if (securityContext) {
							Actions.get().showWithParams2("security", null);
						}
					}

				});
	}

	public void setupUploadForms() {
		setupUploadForm(keyUploadForm, UploadType.KEY, keyFileUpload);
		setupUploadForm(caUploadForm, UploadType.CA, caFileUpload);
		setupUploadForm(certUploadForm, UploadType.CERT, certFileUpload);
	}

	private void setupUploadForm(final FormPanel uploadForm, final UploadType type, FileUpload upload) {
		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction("utils/textfileupload");
		uploadForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				uploadForm.reset();
				String fileData = new InlineHTML(event.getResults()).getText();
				JavaScriptObject safeEval = JsonUtils.safeEval(fileData);
				JSONArray fileUploadData = new JSONArray(safeEval);
				switch (type) {
				case KEY:
					keyData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
					keyFilePresent.setValue(true);
					break;
				case CA:
					caData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
					caFilePresent.setValue(true);
					break;
				case CERT:
					certFilePresent.setValue(true);
					certData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
					break;
				}

			}
		});
		upload.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				uploadForm.submit();
			}
		});
	}

	private enum UploadType {
		KEY, CA, CERT;
	}
}
