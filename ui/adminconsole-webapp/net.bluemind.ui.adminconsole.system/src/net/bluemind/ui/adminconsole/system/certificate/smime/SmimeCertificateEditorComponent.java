package net.bluemind.ui.adminconsole.system.certificate.smime;

import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.container.api.Ack;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.smime.cacerts.api.SmimeCacertInfos;
import net.bluemind.smime.cacerts.api.gwt.endpoint.SmimeCACertGwtEndpoint;
import net.bluemind.ui.adminconsole.system.certificate.smime.l10n.SmimeCertificateConstants;

public class SmimeCertificateEditorComponent extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SmimeCertificateEditorComponent";

	private String cacertData;
	private Domain domain;

	@UiField
	CheckBox cacertFilePresent;

	@UiField
	FormPanel cacertUploadForm;

	@UiField
	FileUpload cacertFileUpload;

	@UiField
	Button uploadBtn;

	@UiField
	Button resetBtn;

	@UiField
	Button listBtn;

	@UiField
	SimplePanel certsListPanel;

	@UiField
	SmimeCertsGrid certsList;

	@UiField
	Label emptyCertLabel;

	private static SmimeCertificateEditorComponentUiBinder uiBinder = GWT
			.create(SmimeCertificateEditorComponentUiBinder.class);

	public static interface SmimeCertificateEditorConstants extends Messages {
		String resetSmimeConfirmation(String domain);
	}

	private static final SmimeCertificateEditorConstants constants = GWT.create(SmimeCertificateEditorConstants.class);

	private static final SmimeCertificateConstants smimeConst = GWT.create(SmimeCertificateConstants.class);

	interface SmimeCertificateEditorComponentUiBinder extends UiBinder<HTMLPanel, SmimeCertificateEditorComponent> {
	}

	public SmimeCertificateEditorComponent() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public void init() {
		setupUploadForm();
		cacertFilePresent.setEnabled(false);
		uploadBtn.setVisible(true);
		uploadBtn.setEnabled(false);
		resetBtn.setVisible(true);
		resetBtn.setEnabled(true);
		certsListPanel.setVisible(false);
		certsList.setVisible(false);
		emptyCertLabel.setVisible(false);
	}

	public void setupUploadForm() {
		cacertUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		cacertUploadForm.setMethod(FormPanel.METHOD_POST);
		cacertUploadForm.setAction("utils/textfileupload");
		cacertUploadForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				String fileData = new InlineHTML(event.getResults()).getText();
				JavaScriptObject safeEval = JsonUtils.safeEval(fileData);
				JSONArray fileUploadData = new JSONArray(safeEval);
				cacertData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
				cacertFilePresent.setValue(true);
				uploadBtn.setEnabled(true);
			}
		});
		cacertFileUpload.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				cacertUploadForm.submit();
			}
		});
	}

	private void resetFileUploadContent() {
		cacertUploadForm.reset();
		cacertFilePresent.setValue(false);
		uploadBtn.setEnabled(false);
		resetBtn.setEnabled(true);
		cacertData = null;
	}

	private void listCacertFiles() {
		if (certsList.getValues().isEmpty()) {
			new SmimeCACertGwtEndpoint(Ajax.TOKEN.getSessionId(), ISmimeCacertUids.domainCreatedCerts(domain.name))
					.getCacertWithRevocations(new DefaultAsyncHandler<List<SmimeCacertInfos>>() {

						@Override
						public void success(List<SmimeCacertInfos> infos) {
							loadCertList(infos);
						}
					});
		} else {
			loadCertList(Collections.emptyList());
			emptyCertLabel.setVisible(false);
		}
	}

	private void loadCertList(List<SmimeCacertInfos> infos) {
		certsList.setValues(infos);
		certsListPanel.setVisible(!infos.isEmpty());
		certsList.setVisible(!infos.isEmpty());
		listBtn.setText(infos.isEmpty() ? smimeConst.displayBtn() : smimeConst.hideBtn());
		emptyCertLabel.setVisible(certsList.getValues().isEmpty());
	}

	private void uploadCacertFile() {
		SmimeCacert cacert = SmimeCacert.create(cacertData);
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid().toLowerCase();

		new SmimeCACertGwtEndpoint(Ajax.TOKEN.getSessionId(), ISmimeCacertUids.domainCreatedCerts(domain.name))
				.create(uid, cacert, new DefaultAsyncHandler<Ack>() {
					@Override
					public void success(Ack value) {
						Notification.get().reportInfo("Certificate has been imported");
						resetFileUploadContent();
					}

					@Override
					public void failure(Throwable e) {
						Notification.get().reportError("Error occured trying to import S/MIME certificate");
					}
				});
	}

	private void reset() {
		new SmimeCACertGwtEndpoint(Ajax.TOKEN.getSessionId(), ISmimeCacertUids.domainCreatedCerts(domain.name))
				.deleteAll(new DefaultAsyncHandler<Void>() {
					@Override
					public void success(Void value) {
						Notification.get().reportInfo("All S/MIME Certificates have been deleted");
						resetBtn.setEnabled(false);
						loadCertList(Collections.emptyList());
					}

					@Override
					public void failure(Throwable e) {
						Notification.get().reportError("Error occured trying to delete S/MIME certificates");
					}
				});
	}

	public void load(Domain domain) {
		this.domain = domain;
		uploadBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				uploadCacertFile();
			}
		});

		resetBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (confirmResetAction()) {
					reset();
				}
			}
		});

		listBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				listCacertFiles();
			}
		});
	}

	private boolean confirmResetAction() {
		return Window.confirm(constants.resetSmimeConfirmation(domain.defaultAlias));
	}
}
