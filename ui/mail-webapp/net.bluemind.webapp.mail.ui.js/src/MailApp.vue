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
                <div class="d-flex align-items-center justify-content-center">
                    <bm-form-input
                        :placeholder="$t('main.main.search.placeholder')"
                        type="search"
                        class="rounded-0 border-secondary"
                    />
                    <bm-icon icon="search" class="searchIcon" />
                </div>
            </bm-col>
            <bm-col class="d-none d-lg-block h-100" cols="12" xl="7" order="last">  
                <mail-toolbar class="mx-auto mx-xl-0" />
            </bm-col>
        </bm-row>
        <bm-row class="flex-fill">
            <bm-col cols="12" md="3" lg="1" xl="2" class="d-lg-block collapse px-0 bg-surface">
                <mail-folder-tree />
            </bm-col>
            <bm-col cols="12" md="3" class="pl-2 py-1 pr-0">
                <mail-message-list class="h-100" />
            </bm-col>
            <bm-col cols="12" md="9" lg="8" xl="7" class="d-md-block collapse pl-2 pr-0 h-100">
                <router-view />
            </bm-col>
        </bm-row>
        <bm-application-alert :errors="getErrorAlerts()" :successes="getSuccessAlerts()">
            <template v-slot="f">
                <div v-if="f.alert.subject">{{ f.alert.subject }}</div>
                {{ f.alert.message }}
            </template>
        </bm-application-alert>
    </bm-container>
</template>

<script>
import { BmApplicationAlert, BmLabelIcon, BmButton, BmCol, BmContainer, BmFormInput, BmIcon, BmRow, MakeUniq }
    from "@bluemind/styleguide"; 
import MailAppL10N from "@bluemind/webapp.mail.l10n";
import MailFolderTree from "./MailFolderTree";
import MailMessageList from "./MailMessageList/MailMessageList";
import MailToolbar from "./MailToolbar/";

export default {
    name: "MailApp",
    components: {
        BmApplicationAlert,
        BmButton,
        BmCol,
        BmContainer,
        BmFormInput,
        BmLabelIcon,
        BmIcon,
        BmRow,
        MailFolderTree,
        MailMessageList,
        MailToolbar
    },
    mixins: [MakeUniq],
    i18n: { messages: MailAppL10N },
    created: function() {
        this.$store.dispatch("backend.mail/folders/bootstrap").then(() => {
            this.$store.dispatch("backend.mail/items/all", this.$store.state["backend.mail/folders"].settings.current);
        });
    },
    methods: {
        getErrorAlerts() {
            return this.$store.state["alert"].errors;
        },
        getSuccessAlerts() {
            return this.$store.state["alert"].successes;
        },
        composeNewMessage() {
            this.$store.commit("backend.mail/items/setCurrent", null);
            this.$router.push({ name: 'newMessage' });
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

.mail-app .searchIcon {
    margin-left: -25px;
}

.mail-app .topbar {
    flex: 0 0 4em;
}
</style>
