<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="link"
            class="collapse-mailbox-btn d-flex align-items-center pb-2 pt-3 border-0 pl-1 w-100"
            aria-controls="collapse-mailbox"
            :aria-expanded="isMailboxExpanded"
            @click="isMailboxExpanded = !isMailboxExpanded"
        >
            <bm-icon :icon="isMailboxExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ mailboxEmail }}</span>
        </bm-button>
        <bm-collapse id="collapse-mailbox" v-model="isMailboxExpanded">
            <bm-tree
                :value="tree.my"
                :selected="currentFolderKey"
                node-id-key="key"
                class="text-nowrap"
                breakpoint="xl"
                @expand="expandFolder"
                @collapse="collapseFolder"
                @select="selectFolder"
            >
                <template v-slot="f">
                    <div class="w-100 d-flex align-items-center">
                        <mail-folder-icon :folder="f.value" breakpoint="xl" class="flex-fill" />
                        <bm-counter-badge
                            v-if="f.value.unread > 0"
                            :value="f.value.unread"
                            :variant="f.value.key != currentFolderKey ? 'secondary' : 'primary'"
                            class="mr-1 position-sticky"
                        />
                    </div>
                </template>
            </bm-tree>
        </bm-collapse>
        <bm-button
            v-if="mailshares.length > 0"
            variant="link"
            class="collapse-mailbox-btn d-flex align-items-center pb-2 pt-3 border-0 pl-1 w-100"
            aria-controls="collapse-mailbox"
            :aria-expanded="areMailsharesExpanded"
            @click="areMailsharesExpanded = !areMailsharesExpanded"
        >
            <bm-icon :icon="areMailsharesExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ $t("common.mailshares") }}</span>
        </bm-button>
        <bm-collapse id="collapse-mailshares" v-model="areMailsharesExpanded">
            <bm-tree
                :value="tree.mailshares"
                :selected="currentFolderKey"
                node-id-key="key"
                class="text-nowrap"
                breakpoint="xl"
                @expand="expandFolder"
                @collapse="collapseFolder"
                @select="selectFolder"
            >
                <template v-slot="f">
                    <div class="w-100 d-flex align-items-center">
                        <mail-folder-icon shared :folder="f.value" breakpoint="xl" class="flex-fill" />
                        <bm-counter-badge
                            v-if="f.value.unread > 0"
                            :value="f.value.unread"
                            :variant="f.value.key != currentFolderKey ? 'secondary' : 'primary'"
                            class="mr-1 position-sticky"
                        />
                    </div>
                </template>
            </bm-tree>
        </bm-collapse>
    </div>
</template>

<script>
import { BmButton, BmCollapse, BmCounterBadge, BmIcon, BmTree } from "@bluemind/styleguide";
import { mapGetters, mapActions, mapState } from "vuex";
import injector from "@bluemind/inject";
import MailFolderIcon from "./MailFolderIcon";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmCounterBadge,
        BmIcon,
        BmTree,
        MailFolderIcon
    },
    data() {
        return {
            isMailboxExpanded: true,
            areMailsharesExpanded: true,
            mailboxEmail: injector.getProvider("UserSession").get().defaultEmail
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["tree", "mailshares"]),
        ...mapState("mail-webapp", ["currentFolderKey"])
    },
    methods: {
        ...mapActions("mail-webapp", ["expandFolder", "collapseFolder"]),
        selectFolder(key) {
            this.$emit("toggle-folders");
            this.$router.push({ path: "/mail/" + key + "/" });
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.bm-tree-node-active,
.bm-tree-node-active .btn {
    color: $info-dark;
}

.mail-folder-tree button.collapse-mailbox-btn {
    color: $info-dark;
    text-decoration-line: none;
    border-bottom: 1px solid $light !important;
}

.bm-counter-badge {
    // work around to avoid parent padding
    margin-top: -($sp-1);
    margin-bottom: -($sp-1);
}
</style>
