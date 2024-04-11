<template>
    <bm-col cols="10" lg="12" class="mail-folder-sidebar-wrapper h-100 p-0 d-flex flex-column">
        <mail-folder-sidebar-header />
        <nav id="folder-sidebar" class="mail-folder-sidebar scroller-y-stable flex-grow-1">
            <folder-list />
        </nav>
        <MailFolderQuotaSidebar v-if="quota.isAboveWarningThreshold() || alwaysShowQuotaSetting" :quota="quota" />
    </bm-col>
</template>

<script>
import MailFolderQuotaSidebar from "./MailFolderQuotaSidebar";

import { BmCol } from "@bluemind/ui-components";

import MailFolderSidebarHeader from "./MailFolderSidebarHeader";
import FolderList from "./FolderList";
import { Quota } from "@bluemind/quota";

export default {
    name: "MailFolderSidebar",
    components: { BmCol, FolderList, MailFolderSidebarHeader, MailFolderQuotaSidebar },
    computed: {
        alwaysShowQuotaSetting() {
            return this.$store.state.settings.always_show_quota === "true";
        },
        quota() {
            return new Quota(this.$store.state["root-app"].quota);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-folder-sidebar {
    min-width: 100%;
    background-color: $surface;
}
</style>
