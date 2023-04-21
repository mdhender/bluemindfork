import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import i18n, { TranslationRegistry } from "@bluemind/i18n";
import BmRoles from "@bluemind/roles";
import store from "@bluemind/store";
import AddCertificateAlert from "./components/mail-app/alerts/AddCertificateAlert";
import CertificateViewer from "./components/mail-app/CertificateViewer";
import ContactWithCertificate from "./components/mail-app/ContactWithCertificate";
import DecryptErrorAlert from "./components/mail-app/alerts/DecryptErrorAlert";
import DecryptErrorTrigger from "./components/mail-app/alerts/DecryptErrorTrigger";
import EncryptAndSignButton from "./components/mail-app/EncryptAndSignButton";
import EncryptErrorAlert from "./components/mail-app/alerts/EncryptErrorAlert";
import InvalidIdentityAlert from "./components/mail-app/alerts/InvalidIdentityAlert";
import LockIcon from "./components/mail-app/LockIcon";
import CertificateFileItem from "./components/mail-app/CertificateFileItem";
import PrefSMime from "./components/preferences/PrefSMime";
import SignErrorAlert from "./components/mail-app/alerts/SignErrorAlert";
import SMimeBodyWrapper from "./components/mail-app/SMimeBodyWrapper";
import SMimeMailRenderlessStore from "./components/mail-app/SMimeMailRenderlessStore";
import SMimeRootRenderlessStore from "./components/root-app/SMimeRootRenderlessStore";
import TrustedSender from "./components/mail-app/TrustedSender";
import UntrustedSenderAlert from "./components/mail-app/alerts/UntrustedSenderAlert";
import UntrustedSenderTrigger from "./components/mail-app/alerts/UntrustedSenderTrigger";
import AddCertificateButton from "./components/mail-app/AddCertificateButton";
import { SMIMEPrefKeys } from "./lib/constants";
import SmimeL10N from "./l10n/";
import { SMIME_AVAILABLE } from "./store/root-app/types";
import GetMailTipsHandler from "./commands/GetMailTipsHandler";

TranslationRegistry.register(SmimeL10N);

Vue.component("SMimeRootRenderlessStore", SMimeRootRenderlessStore);
extensions.register("webapp", "net.bluemind.plugins.smime", {
    component: {
        name: "SMimeRootRenderlessStore",
        path: "app.header"
    }
});

Vue.component("SMimeMailRenderlessStore", SMimeMailRenderlessStore);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "SMimeMailRenderlessStore",
        path: "app.header"
    }
});

Vue.component("SMimeBodyWrapper", SMimeBodyWrapper);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        path: "viewer.body",
        name: "SMimeBodyWrapper"
    }
});

Vue.component("CertificateFileItem", CertificateFileItem);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "CertificateFileItem",
        path: "message.file",
        priority: 2
    }
});

Vue.component("AddCertificateAlert", AddCertificateAlert);
Vue.component("CertificateViewer", CertificateViewer);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "CertificateViewer",
        path: "file.preview",
        priority: 10,
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("LockIcon", LockIcon);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "LockIcon",
        path: "message.icon",
        priority: 50,
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("TrustedSender", TrustedSender);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "TrustedSender",
        path: "viewer.sender.suffix",
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("UntrustedSenderAlert", UntrustedSenderAlert);
Vue.component("UntrustedSenderTrigger", UntrustedSenderTrigger);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        path: "viewer.header",
        name: "UntrustedSenderTrigger",
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("DecryptErrorAlert", DecryptErrorAlert);
Vue.component("DecryptErrorTrigger", DecryptErrorTrigger);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        path: "viewer.header",
        name: "DecryptErrorTrigger",
        role: BmRoles.CAN_USE_SMIME
    }
});

extensions.register("webapp", "net.bluemind.plugins.smime", {
    command: {
        name: "get-mail-tips",
        fn: GetMailTipsHandler,
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("InvalidIdentityAlert", InvalidIdentityAlert);
Vue.component("SignErrorAlert", SignErrorAlert);
Vue.component("EncryptErrorAlert", EncryptErrorAlert);
Vue.component("EncryptAndSignButton", EncryptAndSignButton);
extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "EncryptAndSignButton",
        path: "composer.footer.toolbar",
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("ContactWithCertificate", ContactWithCertificate);
extensions.register("webapp", "net.bluemind.plugins.smime", {
    component: {
        name: "ContactWithCertificate",
        path: "contact.chip.mail.composer.recipients",
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("AddCertificateButton", AddCertificateButton);
extensions.register("webapp", "net.bluemind.plugins.smime", {
    component: {
        name: "AddCertificateButton",
        path: "file.actions",
        role: BmRoles.CAN_USE_SMIME
    }
});

Vue.component("PrefSMime", PrefSMime);
extensions.register("webapp.preferences", "net.bluemind.plugins.smime", {
    section: {
        id: "mail",
        categories: [
            {
                id: "security",
                visible: { name: "RoleCondition", args: [BmRoles.CAN_USE_SMIME] },
                name: i18n.t("common.security"),
                icon: "key",
                groups: prefSmimeGroups()
            }
        ]
    }
});

function prefSmimeGroups() {
    return [
        {
            id: "smime",
            name: i18n.t("smime.preferences.import_field.title"),
            fields: [
                {
                    id: "field",
                    component: { name: "PrefSMime" }
                }
            ]
        },
        {
            id: "smime_encrypt_pref",
            name: i18n.t("smime.preferences.encrypt_field.title"),
            visible: () => store.getters["smime/" + SMIME_AVAILABLE],
            fields: [
                {
                    id: "field",
                    component: {
                        name: "PrefFieldSwitch",
                        options: {
                            setting: SMIMEPrefKeys.ENCRYPTION,
                            default: "true",
                            autosave: true,
                            label: i18n.t("smime.preferences.encrypt_field.label")
                        }
                    }
                }
            ]
        },
        {
            id: "smime_signature_pref",
            name: i18n.t("smime.preferences.signature_field.title"),
            visible: () => store.getters["smime/" + SMIME_AVAILABLE],
            fields: [
                {
                    id: "field",
                    component: {
                        name: "PrefFieldSwitch",
                        options: {
                            setting: SMIMEPrefKeys.SIGNATURE,
                            default: "false",
                            autosave: true,
                            label: i18n.t("smime.preferences.signature_field.label")
                        }
                    }
                }
            ]
        }
    ];
}
