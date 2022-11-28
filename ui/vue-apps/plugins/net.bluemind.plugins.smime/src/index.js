import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import i18n, { TranslationRegistry } from "@bluemind/i18n";
import store from "@bluemind/store";
import SmimeBodyWrapper from "./components/mail-app/SmimeBodyWrapper";
import TrustedSender from "./components/mail-app/TrustedSender";
import UntrustedSenderAlert from "./components/mail-app/UntrustedSenderAlert";
import UntrustedSenderTrigger from "./components/mail-app/UntrustedSenderTrigger";
import DecryptErrorAlert from "./components/mail-app/DecryptErrorAlert";
import DecryptErrorTrigger from "./components/mail-app/DecryptErrorTrigger";
import PrefSmime from "./components/preferences/PrefSmime";
import LockIcon from "./components/mail-app/LockIcon";
import { SMIMEPrefKeys } from "./lib/constants";
import SmimeL10N from "./l10n/";
import SmimeStore from "./store";
import { SMIME_AVAILABLE } from "./store/getterTypes";

TranslationRegistry.register(SmimeL10N);

store.registerModule("smime", SmimeStore);

Vue.component("LockIcon", LockIcon);
Vue.component("PrefSmime", PrefSmime);
Vue.component("SmimeBodyWrapper", SmimeBodyWrapper);
Vue.component("TrustedSender", TrustedSender);
Vue.component("UntrustedSenderAlert", UntrustedSenderAlert);
Vue.component("UntrustedSenderTrigger", UntrustedSenderTrigger);
Vue.component("DecryptErrorTrigger", DecryptErrorTrigger);
Vue.component("DecryptErrorAlert", DecryptErrorAlert);

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
            visible: () => store.getters["smime/" + SMIME_AVAILABLE],
            fields: [
                {
                    id: "field",
                    component: {
                        name: "PrefFieldSwitch",
                        options: {
                            setting: SMIMEPrefKeys.ENCRYPTION_PREF,
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
                            setting: SMIMEPrefKeys.SIGNATURE_PREF,
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
