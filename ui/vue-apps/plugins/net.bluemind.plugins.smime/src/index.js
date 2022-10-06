import Vue from "vue";
import { extensions } from "@bluemind/extensions";
import PrefSmime from "./components/PrefSmime";

Vue.component("PrefSmime", PrefSmime);

// FIXME: when loading app on a preferences route, category is not displayed...
// (because extension is loaded before this register)
extensions.register("webapp.preferences", "net.bluemind.plugins.smime", {
    section: {
        id: "mail",
        categories: [
            {
                id: "security",
                name: "Sécurité",
                // name: inject("i18n").t("common.security"),
                icon: "key",
                groups: [
                    {
                        id: "smime",
                        name: "Chiffrement S/MIME",
                        // name: inject("i18n").t("preferences.mail.security.smime.title"),
                        fields: [
                            {
                                id: "field",
                                component: { name: "PrefSmime" }
                            }
                        ]
                    }
                ]
            }
        ]
    }
});
