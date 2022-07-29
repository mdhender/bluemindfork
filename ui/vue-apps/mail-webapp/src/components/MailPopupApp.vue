<template>
    <main class="flex-fill mail-popup-app scroller-y scroller-visible-on-hover"><router-view class="flex-fill" /></main>
</template>
<script>
import { mapGetters } from "vuex";
import { ACTIVE_MESSAGE } from "~/getters";
import { IS_POPUP } from "~/mutations";
import MailAppMixin from "./MailApp/MailAppMixin";

export default {
    name: "MailPopupApp",
    mixins: [MailAppMixin],
    computed: {
        ...mapGetters("mail", { message: ACTIVE_MESSAGE })
    },
    watch: {
        "message.subject": {
            handler() {
                if (this.message) {
                    const subject = this.message.subject.trim() ? this.message.subject : this.$t("mail.main.new");
                    document.title = subject + this.$t("common.product");
                }
            }
        }
    },
    beforeCreate() {
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
    }
};
</script>
<style>
.mail-popup-app .mail-composer {
    margin: 0 !important;
}
</style>
