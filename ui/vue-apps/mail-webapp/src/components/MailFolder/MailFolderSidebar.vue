<template>
    <bm-col cols="10" lg="12" class="mail-folder-sidebar-wrapper bg-surface h-100 d-flex flex-column">
        <mail-folder-sidebar-header />
        <nav class="mail-folder-sidebar scroller-y scroller-visible-on-hover flex-grow-1">
            <user-folders v-for="mailbox in USER_MAILBOXES" :key="mailbox.key" :mailbox="mailbox" />
            <mailshare-folders />
        </nav>
        <div v-if="mustDisplayQuota" class="my-1" :class="showQuotaWarning ? 'text-danger' : ''">
            <hr class="my-1" />
            <bm-icon v-if="showQuotaWarning" icon="exclamation-circle" class="ml-2 align-middle" />
            <span :class="showQuotaWarning ? 'pl-1' : 'pl-2'" class="align-middle">{{
                $t("mail.mailbox.quota.used", { usedQuotaPercentage })
            }}</span>
        </div>
    </bm-col>
</template>

<script>
import { mapGetters, mapState } from "vuex";

import { BmCol, BmIcon } from "@bluemind/styleguide";
import { USED_QUOTA_PERCENTAGE_WARNING } from "@bluemind/email";

import { MAILSHARES, MY_MAILBOX, USER_MAILBOXES } from "~/getters";
import MailFolderSidebarHeader from "./MailFolderSidebarHeader";
import UserFolders from "./UserFolders";
import MailshareFolders from "./MailshareFolders";

export default {
    name: "MailFolderSidebar",
    components: { BmCol, BmIcon, MailFolderSidebarHeader, UserFolders, MailshareFolders },
    computed: {
        ...mapGetters("mail", { MAILSHARES, MY_MAILBOX, USER_MAILBOXES }),
        ...mapState("root-app", ["quota"]),
        ...mapState("session", { settings: ({ settings }) => settings.remote }),
        usedQuotaPercentage() {
            return Math.ceil((this.quota.used / this.quota.total) * 100);
        },
        mustDisplayQuota() {
            return (
                this.quota.used &&
                this.quota.total &&
                (this.settings.always_show_quota === "true" || this.showQuotaWarning)
            );
        },
        showQuotaWarning() {
            return this.usedQuotaPercentage >= USED_QUOTA_PERCENTAGE_WARNING;
        }
    }
};
</script>
<style>
.mail-folder-sidebar {
    min-width: 100%;
}
</style>
