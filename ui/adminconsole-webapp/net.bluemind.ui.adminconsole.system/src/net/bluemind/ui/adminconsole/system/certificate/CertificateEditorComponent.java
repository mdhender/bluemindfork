package net.bluemind.ui.adminconsole.system.certificate;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.task.api.TaskRef;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.gwt.endpoint.SecurityMgmtGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.certificate.l10n.CertificateEditorComponentConstants;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.TrPanel;
import net.bluemind.ui.gwttask.client.TaskWatcher;

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

	@UiField
	TrPanel choicePanel;

	private ListBox sslCertEngineTypeSel;

	@UiField
	HTMLPanel certFilesPanel;

	@UiField
	HTMLPanel letsEncryptPanel;

	@UiField
	HTMLPanel disablePanel;

	@UiField
	TextBox email;

	@UiField
	Button acceptTos;

	@UiField
	Anchor tos;

	@UiField
	Button generateBtn;

	@UiField
	Label certificateEndDate;

	@UiField
	Label disableInfo;

	@UiField
	Button disableBtn;

	@UiField
	Button fileBtn;

	private boolean securityContext;

	private int disableIndex = -1;
	private int certFilesIndex = -1;
	private int letsEncryptIndex = -1;

	private static CertificateEditorComponentUiBinder uiBinder = GWT.create(CertificateEditorComponentUiBinder.class);
	private static final CertificateEditorComponentConstants constants = GWT
			.create(CertificateEditorComponentConstants.class);

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

	public void saveCertificate(CertificateDomainEngine sslCertifEngine, String domainUid) {
		createSecurityMgmtGwtEndpoint(
				CertData.create(sslCertifEngine, caData, certData, keyData, domainUid, email.getText()));
	}

	private void createSecurityMgmtGwtEndpoint(CertData certificateValues) {
		new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId()).updateCertificate(certificateValues,
				new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						switch (certificateValues.sslCertificateEngine) {
						case LETS_ENCRYPT:
						case DISABLED:
							break;
						case FILE:
							if (certificateValues.certificate != null && certificateValues.certificateAuthority != null
									&& certificateValues.privateKey != null) {
								Notification.get().reportInfo("Certificate has been imported");
							}
							if (!securityContext && !(certificateValues.certificate == null
									&& certificateValues.certificateAuthority == null
									&& certificateValues.privateKey == null)) {
								Window.Location.reload();
							}
							break;
						default:
							Notification.get().reportError("Invalid SSL certif engine");
							break;
						}

						if (securityContext) {
							Actions.get().showWithParams2("security", null);
						}
					}

					@Override
					public void failure(Throwable e) {
						Notification.get().reportError(e);
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

	public void init(boolean securityContext) {
		this.securityContext = securityContext;
		disablePanel.setVisible(false);
		letsEncryptPanel.setVisible(false);
		certFilesPanel.setVisible(false);
		fileBtn.setVisible(false);
		acceptTos.setVisible(false);
		tos.setVisible(false);
		generateBtn.setVisible(false);
		certificateEndDate.setVisible(false);
		disableInfo.setVisible(false);
		disableBtn.setVisible(false);
		sslCertEngineTypeSel = new ListBox();
		sslCertEngineTypeSel.addItem(constants.fileItem());
		certFilesIndex = 0;
		sslCertEngineTypeSel.addItem(constants.letsEncryptItem());
		letsEncryptIndex = 1;
		if (!securityContext) {
			sslCertEngineTypeSel.addItem(constants.disableItem());
			disableIndex = 2;
		}
		choicePanel.add(new Label(CertificateEditorComponentConstants.INST.sslCertifEngine()), "label");
		choicePanel.add(sslCertEngineTypeSel);
	}

	private void updateSettings(CertificateDomainEngine sslEngine, JavaScriptObject model) {
		if (!securityContext) {
			SettingsModel.domainSettingsFrom(model).putString(DomainSettingsKeys.ssl_certif_engine.name(),
					sslEngine.name());
		} else {
			SysConfModel.from(model).putString(SysConfKeys.ssl_certif_engine.name(), sslEngine.name());
		}
	}

	private boolean confirmReloadAction() {
		boolean confirmReload = true;
		if (!securityContext) {
			confirmReload = Window.confirm(constants.reloadConfirmation());
		}
		return confirmReload;
	}

	public void load(CertificateDomainEngine sslEngine, String domainUid, Domain domain, JavaScriptObject model,
			String externalUrl) {
		loadListBoxDefaultSelection(sslEngine, domain, model, domainUid, externalUrl);
		acceptTos.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				approveLetsEncryptTos(domainUid);
			}
		});
		generateBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (confirmReloadAction()) {
					updateSettings(sslEngine, model);
					generateLetsEncrypt(domainUid);
				}
			}
		});
		disableBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (confirmReloadAction()) {
					updateSettings(sslEngine, model);
					disableCertificate(domainUid);
				}
			}
		});
		fileBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (confirmReloadAction()) {
					updateSettings(sslEngine, model);
					generateFileCertificate(domainUid);
				}
			}
		});

		sslCertEngineTypeSel.addChangeHandler(event -> {
			if (sslCertEngineTypeSel.getSelectedIndex() == certFilesIndex) {
				loadCertFilesPanel();
			} else if (sslCertEngineTypeSel.getSelectedIndex() == letsEncryptIndex) {
				loadLetsEncryptPanel(domain, domainUid);
			} else if (sslCertEngineTypeSel.getSelectedIndex() == disableIndex) {
				loadDisablePanel(externalUrl, sslEngine);
			} else {
				loadCertFilesPanel();
			}
		});
	}

	private void generateFileCertificate(String domainUid) {
		saveCertificate(CertificateDomainEngine.FILE, !securityContext ? domainUid : "global.virt");
	}

	private void loadListBoxDefaultSelection(CertificateDomainEngine sslCertEngine, Domain domain,
			JavaScriptObject model, String domainUid, String externalUrl) {
		if (sslCertEngine != null) {
			switch (sslCertEngine) {
			case FILE:
				loadCertFilesPanel();
				break;
			case LETS_ENCRYPT:
				loadLetsEncryptPanel(domain, domainUid);
				break;
			case DISABLED:
				loadDisablePanel(externalUrl, sslCertEngine);
				break;
			default:
				if (!securityContext) {
					loadDisablePanel(externalUrl, sslCertEngine);
				} else {
					loadCertFilesPanel();
				}
			}
		} else {
			if (!securityContext) {
				loadDisablePanel(externalUrl, sslCertEngine);
			} else {
				loadCertFilesPanel();
			}
		}
	}

	private void loadDisablePanel(String externalUrl, CertificateDomainEngine sslCertEngine) {
		certFilesPanel.setVisible(false);
		letsEncryptPanel.setVisible(false);
		disablePanel.setVisible(true);
		disableBtn.setVisible(true);
		disableBtn.setEnabled(true);

		if (sslCertEngine == CertificateDomainEngine.DISABLED) {
			disableBtn.setEnabled(false);
		}

		if (!securityContext && (externalUrl == null || externalUrl.isEmpty())) {
			choicePanel.setVisible(false);
			disableInfo.setVisible(true);
			disableBtn.setVisible(false);
		} else {
			choicePanel.setVisible(true);
			sslCertEngineTypeSel.setSelectedIndex(disableIndex);
		}
	}

	private void loadLetsEncryptPanel(Domain domain, String domainUid) {
		sslCertEngineTypeSel.setVisible(true);
		sslCertEngineTypeSel.setSelectedIndex(letsEncryptIndex);
		certFilesPanel.setVisible(false);
		letsEncryptPanel.setVisible(true);
		disablePanel.setVisible(false);
		acceptTos.setVisible(true);
		tos.setVisible(true);
		generateBtn.setVisible(true);

		getLetsEncryptTos(domainUid);
		boolean tosApproved = "true".equals(domain.properties.get("TOS_APPROVAL"));
		acceptTos.setEnabled(!tosApproved);
		generateBtn.setEnabled(tosApproved);

		certificateEndDate.setVisible(true);
		String certifEndDate = domain.properties.get("CERTIFICATE_END_DATE");
		if (certifEndDate != null && !certifEndDate.isEmpty()) {
//			try {
//				certificateEndDate.setText(DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_FULL)
//						.format(new SimpleDateFormat("YYYY-MM-dd").parse(certifEndDate)));
//			} catch (ParseException e) {
			certificateEndDate.setText(certifEndDate);
//			}
			generateBtn.setText(constants.renewBtn());
		} else {
			generateBtn.setText(constants.generateBtn());
		}

		String emailAddress = domain.properties.get("LETS_ENCRYPT_CONTACT");
		if (emailAddress != null && !emailAddress.isEmpty()) {
			email.setText(emailAddress);
		}
	}

	private void loadCertFilesPanel() {
		sslCertEngineTypeSel.setVisible(true);
		sslCertEngineTypeSel.setSelectedIndex(certFilesIndex);
		certFilesPanel.setVisible(true);
		letsEncryptPanel.setVisible(false);
		disablePanel.setVisible(false);
		fileBtn.setVisible(false);

		if (!securityContext) {
			fileBtn.setVisible(true);
		}

		setupUploadForms();
		enableCheckBoxes(false);
	}

	private void generateLetsEncrypt(String domainUid) {
		SecurityMgmtGwtEndpoint securityEndpoint = new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
		CertData certData = CertData.createForLetsEncrypt(domainUid, email.getText());
		securityEndpoint.generateLetsEncrypt(certData, new DefaultAsyncHandler<TaskRef>() {

			@Override
			public void success(TaskRef value) {
				TaskWatcher.track(value.id);
				CompletableFuture<Void> status = TaskWatcher.track(value.id, false);
				status.thenRun(() -> {
					Notification.get().reportInfo("SSL Certificate generated with Let's Encrypt");
					if (!securityContext) {
						Window.Location.reload();
					}
				});
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	private void disableCertificate(String domainUid) {
		SecurityMgmtGwtEndpoint securityEndpoint = new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
		CertData certData = CertData.createForDisable(domainUid);
		securityEndpoint.updateCertificate(certData, new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				Notification.get().reportInfo("SSL Certificate disabled");
				if (!securityContext) {
					Window.Location.reload();
				}
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	private void getLetsEncryptTos(String domainUid) {
		SecurityMgmtGwtEndpoint settings = new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
		settings.getLetsEncryptTos(new DefaultAsyncHandler<String>() {

			@Override
			public void success(String value) {
				tos.setHref(value);
				tos.setTarget("_blank");
				tos.setEnabled(true);
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	private void approveLetsEncryptTos(String domainUid) {
		SecurityMgmtGwtEndpoint settings = new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
		settings.approveLetsEncryptTos(domainUid, new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				acceptTos.setEnabled(false);
				generateBtn.setEnabled(true);
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	public CertificateDomainEngine getSelectedSslCertificateEngine() {
		int selectedIndex = sslCertEngineTypeSel.getSelectedIndex();
		if (selectedIndex == certFilesIndex) {
			return CertificateDomainEngine.FILE;
		} else if (selectedIndex == letsEncryptIndex) {
			return CertificateDomainEngine.LETS_ENCRYPT;
		} else if (selectedIndex == disableIndex) {
			return CertificateDomainEngine.DISABLED;
		} else {
			return null;
		}
	}

}
