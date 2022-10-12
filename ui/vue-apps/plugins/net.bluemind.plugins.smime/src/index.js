import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import store from "@bluemind/store";
import PrefSmime from "./components/PrefSmime";
import SmimeStore from "./store";
import { SMIME_AVAILABLE } from "./store/getterTypes";

store.registerModule("smime", SmimeStore);

Vue.component("PrefSmime", PrefSmime);

const SIGNATURE_PREF = "sign_message_by_default";
const ENCRYPTION_PREF = "encrypt_message_by_default";

const prefSmimeGroups = [
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
                        setting: ENCRYPTION_PREF,
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
                        setting: SIGNATURE_PREF,
                        autosave: true,
                        label: "Par défaut, signer mes messages à l'envoi" // FIXME i18n
                    }
                }
            }
        ]
    }
];

extensions.register("webapp.preferences", "net.bluemind.plugins.smime", {
    settingDefaultValues: { [SIGNATURE_PREF]: "false", [ENCRYPTION_PREF]: "true" },
    section: {
        id: "mail",
        categories: [
            {
                id: "security",
                name: "Sécurité", // FIXME i18n
                icon: "key",
                groups: prefSmimeGroups
            }
        ]
    }
});
