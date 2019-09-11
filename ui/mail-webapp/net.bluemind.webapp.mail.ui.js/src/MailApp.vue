<template>
    <bm-container fluid class="flex-fill d-flex flex-column mail-app">
        <bm-row align-v="center" class="shadow-sm bg-surface py-2 py-xl-0 topbar">
            <bm-col cols="4" md="2" order="0">
                <bm-button
                    variant="primary"
                    class="text-nowrap ml-3 d-xl-inline-block d-block"
                    @click="composeNewMessage"
                >
                    <bm-label-icon icon="plus" breakpoint="xl">{{ $t("mail.main.new") }}</bm-label-icon>
                </bm-button>
            </bm-col>
            <bm-col cols="8" md="10" xl="3" order="1">
                <mail-search-form />
            </bm-col>
            <bm-col class="d-none d-lg-block h-100" cols="12" xl="7" order="last">
                <mail-toolbar class="mx-auto mx-xl-0" />
            </bm-col>
        </bm-row>
        <bm-row class="flex-fill">
            <bm-col cols="12" md="3" lg="1" xl="2" class="d-lg-block collapse px-0 bg-surface">
                <mail-folder-tree />
            </bm-col>
            <bm-col cols="12" md="3" class="pl-2 pr-0">
                <mail-message-list class="h-100" />
            </bm-col>
            <bm-col cols="12" md="9" lg="8" xl="7" class="d-md-block collapse px-0 h-100">
                <router-view />
            </bm-col>
        </bm-row>
        <bm-application-alert :errors="errorAlerts" :successes="successAlerts">
            <template v-slot="slotProps">
                <mail-alert-renderer :alert="slotProps.alert" />
            </template>
        </bm-application-alert>
    </bm-container>
</template>

<script>
import { BmApplicationAlert, BmLabelIcon, BmButton, BmCol, BmContainer, BmRow, MakeUniq } from "@bluemind/styleguide";
import { mapActions, mapGetters, mapState, mapMutations } from "vuex";
import MailAppL10N from "@bluemind/webapp.mail.l10n";
import MailFolderTree from "./MailFolderTree";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import MailAlertRenderer from "./MailAlertRenderer";
import { AlertTypes, Alert } from "@bluemind/alert.store";

export default {
    name: "MailApp",
    components: {
        BmApplicationAlert,
        BmButton,
        BmCol,
        BmContainer,
        BmLabelIcon,
        BmRow,
        MailFolderTree,
        MailMessageList,
        MailToolbar,
        MailSearchForm,
        MailAlertRenderer
    },
    mixins: [MakeUniq],
    i18n: { messages: MailAppL10N },
    computed: {
        ...mapGetters("backend.mail/items", ["currentMessage", "messages", "indexOf"]),
        ...mapGetters("backend.mail/folders", ["currentFolderId", "trashFolderId", "currentFolder"]),
        ...mapState("backend.mail/items", ["shouldRemoveItem", "count", "current"]),
        ...mapState("alert", { errorAlerts: "errors", successAlerts: "successes" })
    },
    watch: {
        shouldRemoveItem() {
            if (this.shouldRemoveItem !== null) {
                const index = this.indexOf(this.shouldRemoveItem);
                const message = this.messages[index];
                const subject = message.subject;
                const mailId = message.id;

                this.$store
                    .dispatch("backend.mail/items/remove", {
                        folderId: this.currentFolderId,
                        trashFolderId: this.trashFolderId,
                        mailId
                    })
                    .then(() => {
                        if (this.current !== null) {
                            if (this.count === 1) {
                                this.$router.push("/mail/" + this.currentFolder + "/");
                            } else if (this.count === index + 1) {
                                this.$router.push("/mail/" + this.currentFolder + "/" + this.messages[index - 1].id);
                            } else {
                                this.$router.push("/mail/" + this.currentFolder + "/" + this.messages[index + 1].id);
                            }
                        }
                        this.remove(index);
                        const key = "common.alert.remove.ok";
                        const success = new Alert({
                            type: AlertTypes.SUCCESS,
                            code: "ALERT_CODE_MSG_REMOVED_OK",
                            key,
                            message: this.$t(key, { subject }),
                            props: { subject }
                        });
                        this.addSuccess(success);
                    })
                    .catch(reason => {
                        const key = "common.alert.remove.error";
                        const error = new Alert({
                            code: "ALERT_CODE_MSG_REMOVED_ERROR",
                            key,
                            message: this.$t(key, { subject, reason }),
                            props: { subject, reason }
                        });
                        this.addError(error);
                    });
            }
        }
    },
    created: function() {
        const isRootPath = this.$route.path.endsWith("/mail/");

        this.bootstrap(isRootPath).then(() => {
            if (isRootPath) {
                this.all(this.currentFolder);
            }
        });
    },
    methods: {
        ...mapActions("backend.mail/folders", ["bootstrap"]),
        ...mapActions("backend.mail/items", ["all"]),
        ...mapMutations("alert", ["addError", "addSuccess"]),
        ...mapMutations("backend.mail/items", ["remove", "setCurrent"]),
        composeNewMessage() {
            this.setCurrent(null);
            this.$router.push({ name: "newMessage" });
        }
    }
};
</script>
<style>
body > div {
    display: flex;
    flex-direction: column;
}
.mail-app .row:first-child {
    z-index: 1;
}

.flex-fill {
    min-height: 0;
}

.mail-app .topbar {
    flex: 0 0 4em;
}

.mail-app .bm-application-alert {
    position: absolute;
    bottom: 5px;
    width: 50vw;
}
</style>
