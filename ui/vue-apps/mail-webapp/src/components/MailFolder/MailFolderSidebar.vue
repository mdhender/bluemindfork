<template>
    <bm-col cols="10" lg="12" class="mail-folder-sidebar-wrapper h-100 p-0 d-flex flex-column">
        <mail-folder-sidebar-header />
        <nav id="folder-sidebar" class="mail-folder-sidebar scroller-y scroller-visible-on-hover flex-grow-1">
            <folder-list />
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
import { mapState } from "vuex";

import { BmCol, BmIcon } from "@bluemind/styleguide";
import { USED_QUOTA_PERCENTAGE_WARNING } from "@bluemind/email";

import MailFolderSidebarHeader from "./MailFolderSidebarHeader";
import FolderList from "./FolderList";

export default {
    name: "MailFolderSidebar",
    components: { BmCol, BmIcon, FolderList, MailFolderSidebarHeader },
    computed: {
        ...mapState("root-app", ["quota"]),
        alwaysShowQuotaSetting() {
            return this.$store.state.settings.always_show_quota;
        },
        usedQuotaPercentage() {
            return Math.ceil((this.quota.used / this.quota.total) * 100);
        },
        mustDisplayQuota() {
            return (
                this.quota.used && this.quota.total && (this.alwaysShowQuotaSetting === "true" || this.showQuotaWarning)
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
