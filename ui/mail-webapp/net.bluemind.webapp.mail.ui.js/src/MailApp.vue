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
            <bm-col cols="12" md="3" lg="1" xl="2" class="d-lg-block collapse px-0 bg-surface h-100">
                <div class="h-100 scroller scroller-visible-on-hover position-relative">
                    <mail-folder-tree class="d-inline-block" />
                </div>
            </bm-col>
            <bm-col cols="12" md="3" class="pl-2 pr-0">
                <mail-message-list class="h-100" />
            </bm-col>
            <bm-col cols="12" md="9" lg="8" xl="7" class="collapse px-0 h-100 d-flex flex-column overflow-auto">
                <router-view />
            </bm-col>
        </bm-row>
        <mail-purge-modal />
        <bm-application-alert :alerts="alerts">
            <template v-slot="slotProps">
                <mail-alert-renderer :alert="slotProps.alert" />
            </template>
        </bm-application-alert>
    </bm-container>
</template>

<script>
import { BmApplicationAlert, BmLabelIcon, BmButton, BmCol, BmContainer, BmRow, MakeUniq } from "@bluemind/styleguide";
import MailAlertRenderer from "./MailAlertRenderer";
import { mapActions, mapState } from "vuex";
import MailAppL10N from "@bluemind/webapp.mail.l10n";
import MailFolderTree from "./MailFolderTree";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailPurgeModal from "./MailPurgeModal";
import MailToolbar from "./MailToolbar/";
import MailSearchForm from "./MailSearchForm";
import injector from "@bluemind/inject";

export default {
    name: "MailApp",
    components: {
        BmApplicationAlert,
        BmButton,
        BmCol,
        BmContainer,
        BmLabelIcon,
        BmRow,
        MailAlertRenderer,
        MailFolderTree,
        MailMessageList,
        MailPurgeModal,
        MailSearchForm,
        MailToolbar
    },
    mixins: [MakeUniq],
    componentI18N: { messages: MailAppL10N },
    computed: {
        ...mapState("alert", ["alerts"])
    },
    created: function() {
        const userSession = injector.getProvider("UserSession").get();
        this.bootstrap(userSession.login);
    },
    methods: {
        ...mapActions("mail-webapp", ["bootstrap"]),
        composeNewMessage() {
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

.flex-fill {
    min-height: 0;
}

.mail-app .topbar {
    flex: 0 0 4em;
    z-index: 1;
}

.mail-app .bm-application-alert {
    position: absolute;
    bottom: 5px;
    z-index: 3;
}

.mail-folder-tree {
    min-width: 100%;
}
</style>
