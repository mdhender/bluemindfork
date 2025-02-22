<template>
    <main class="flex-fill mail-popup-app scroller-y-stable">
        <bm-extension id="webapp.mail" path="app.header" />
        <router-view class="flex-fill" />
    </main>
</template>

<script>
import { mapGetters } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { ACTIVE_MESSAGE, MY_TEMPLATES } from "~/getters";
import { IS_POPUP } from "~/mutations";
import MailAppMixin from "./MailApp/MailAppMixin";
import MailStore from "../store/";

export default {
    name: "MailPopupApp",
    components: { BmExtension },
    mixins: [MailAppMixin],
    computed: {
        ...mapGetters("mail", { message: ACTIVE_MESSAGE, MY_TEMPLATES })
    },
    watch: {
        "message.subject": {
            handler() {
                if (this.message) {
                    const subject = this.message.subject.trim()
                        ? this.message.subject
                        : this.message.folderRef.key === this.MY_TEMPLATES.key
                        ? this.$t("mail.actions.new_template")
                        : this.$t("mail.main.new");
                    document.title = subject;
                }
            }
        }
    },
    beforeCreate() {
        if (!this.$store.hasModule("mail")) {
            this.$store.registerModule("mail", MailStore);
        }
        this.$store.commit("root-app/HIDE_BANNER");
        this.$store.commit(`mail/${IS_POPUP}`);
    },
    created() {
        const documentTitle = this.$t("mail.application.title") + this.$t("common.product");
        document.title = documentTitle;
        this.$router.beforeEach(({ name, params, query, hash }, from, next) => {
            let route;
            switch (name) {
                case "mail:home":
                case "mail:root":
                    route = { name: "mail:popup:home", params, query, hash };
                    break;
                case "mail:message":
                    route = { name: "mail:popup:message", params, query, hash };
                    break;
                case "mail:conversation":
                    route = { name: "mail:popup:conversation", params, query, hash };
                    break;
                default:
                    route = true;
            }
            next(route);
        });
    },
    destroyed() {
        if (this.$store.hasModule("mail")) {
            this.$store.unregisterModule("mail");
        }
    }
};
</script>

<style>
.mail-popup-app .mail-composer {
    margin: 0 !important;
}
</style>
