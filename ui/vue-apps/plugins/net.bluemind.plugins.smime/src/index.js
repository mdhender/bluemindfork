import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import store from "@bluemind/store";
import SmimeBodyWrapper from "./components/mail-app/SmimeBodyWrapper";
import TrustedSender from "./components/mail-app/TrustedSender";
import UntrustedSenderAlert from "./components/mail-app/UntrustedSenderAlert";
import UntrustedSenderTrigger from "./components/mail-app/UntrustedSenderTrigger";
import PrefSmime from "./components/preferences/PrefSmime";
import { SMIMEPrefKeys } from "./lib/constants";
import SmimeStore from "./store";
import { SMIME_AVAILABLE } from "./store/getterTypes";

store.registerModule("smime", SmimeStore);

Vue.component("PrefSmime", PrefSmime);
Vue.component("SmimeBodyWrapper", SmimeBodyWrapper);
Vue.component("TrustedSender", TrustedSender);
Vue.component("UntrustedSenderAlert", UntrustedSenderAlert);
Vue.component("UntrustedSenderTrigger", UntrustedSenderTrigger);

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
                name: "Sécurité", // FIXME i18n
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
            name: "Chiffrement S/MIME", // FIXME i18n
            fields: [
                {
                    id: "field",
                    component: { name: "PrefSmime" }
                }
            ]
        },
        {
            id: "smime_encrypt_pref",
            name: "Chiffrement des messages par défaut", // FIXME i18n
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
                            label: "Par défaut, chiffrer mes messages à l'envoi" // FIXME i18n
                        }
                    }
                }
            ]
        },
        {
            id: "smime_signature_pref",
            name: "Signature des messages par défaut", // FIXME i18n
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
                            label: "Par défaut, signer mes messages à l'envoi" // FIXME i18n
                        }
                    }
                }
            ]
        }
    ];
}
