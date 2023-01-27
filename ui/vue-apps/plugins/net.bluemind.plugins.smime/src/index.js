import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import i18n, { TranslationRegistry } from "@bluemind/i18n";
import store from "@bluemind/store";
import SMimeBodyWrapper from "./components/mail-app/SMimeBodyWrapper";
import TrustedSender from "./components/mail-app/TrustedSender";
import UntrustedSenderAlert from "./components/mail-app/UntrustedSenderAlert";
import UntrustedSenderTrigger from "./components/mail-app/UntrustedSenderTrigger";
import DecryptErrorAlert from "./components/mail-app/DecryptErrorAlert";
import EncryptErrorAlert from "./components/mail-app/EncryptErrorAlert";
import DecryptErrorTrigger from "./components/mail-app/DecryptErrorTrigger";
import SignErrorAlert from "./components/mail-app/SignErrorAlert";
import EncryptAndSignButton from "./components/mail-app/EncryptAndSignButton";
import ContactWithCertificate from "./components/mail-app/ContactWithCertificate";
import PrefSMime from "./components/preferences/PrefSMime";
import LockIcon from "./components/mail-app/LockIcon";
import { SMIMEPrefKeys } from "./lib/constants";
import SmimeL10N from "./l10n/";
import SmimeStore from "./store";
import { CHECK_IF_ASSOCIATED } from "./store/actionTypes";
import { SMIME_AVAILABLE } from "./store/getterTypes";
import GetMailTipsHandler from "./commands/GetMailTipsHandler";

TranslationRegistry.register(SmimeL10N);

store.registerModule(["mail", "smime"], SmimeStore);
store.dispatch("mail/" + CHECK_IF_ASSOCIATED);

Vue.component("LockIcon", LockIcon);
Vue.component("PrefSMime", PrefSMime);
Vue.component("SMimeBodyWrapper", SMimeBodyWrapper);
Vue.component("TrustedSender", TrustedSender);
Vue.component("UntrustedSenderAlert", UntrustedSenderAlert);
Vue.component("UntrustedSenderTrigger", UntrustedSenderTrigger);
Vue.component("DecryptErrorTrigger", DecryptErrorTrigger);
Vue.component("DecryptErrorAlert", DecryptErrorAlert);
Vue.component("EncryptErrorAlert", EncryptErrorAlert);
Vue.component("EncryptAndSignButton", EncryptAndSignButton);
Vue.component("ContactWithCertificate", ContactWithCertificate);
Vue.component("SignErrorAlert", SignErrorAlert);

extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "LockIcon",
        path: "list.conversation.icon",
        priority: 10
    }
});

extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        path: "viewer.sender.suffix",
        name: "TrustedSender"
    }
});

extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        path: "viewer.header",
        name: "UntrustedSenderTrigger"
    }
});

extensions.register("webapp.mail", "net.bluemind.plugins.smime.encryption", {
    component: {
        path: "viewer.header",
        name: "DecryptErrorTrigger"
    }
});

extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        path: "viewer.body",
        name: "SmimeBodyWrapper"
    }
});
extensions.register("webapp", "net.bluemind.plugins.smime", {
    command: {
        name: "get-mail-tips",
        fn: GetMailTipsHandler
    }
});

extensions.register("webapp.preferences", "net.bluemind.plugins.smime", {
    section: {
        id: "mail",
        categories: [
            {
                id: "security",
                name: i18n.t("common.security"),
                icon: "key",
                groups: prefSmimeGroups()
            }
        ]
    }
});

extensions.register("webapp.mail", "net.bluemind.plugins.smime", {
    component: {
        name: "EncryptAndSignButton",
        path: "composer.footer.toolbar"
    }
});

extensions.register("webapp", "net.bluemind.webmodules.smime", {
    component: {
        name: "ContactWithCertificate",
        path: "contact.chip.mail.composer.recipients"
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
                    component: { name: "PrefSmime" }
                }
            ]
        },
        {
            id: "smime_encrypt_pref",
            name: i18n.t("smime.preferences.encrypt_field.title"),
            visible: () => store.getters["mail/" + SMIME_AVAILABLE],
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
            visible: () => store.getters["mail/" + SMIME_AVAILABLE],
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
