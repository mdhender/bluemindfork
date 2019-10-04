<template>
    <div class="mail-folder-tree">
        <bm-button
            variant="link"
            class="collapse-mailbox-btn d-none d-xl-flex align-items-center pb-2 pt-3 border-0 pl-1 w-100"
            aria-controls="collapse-mailbox"
            :aria-expanded="isMailboxExpanded"
            @click="isMailboxExpanded = !isMailboxExpanded"
        >
            <bm-icon :icon="isMailboxExpanded ? 'caret-down' : 'caret-right'" size="sm" class="bm-icon mr-2" />
            <span class="font-weight-bold">{{ mailboxEmail }}</span>
        </bm-button>
        <bm-collapse id="collapse-mailbox" v-model="isMailboxExpanded">
            <bm-tree
                :value="tree"
                :selected="currentFolderUid"
                node-id-key="uid"
                class="text-nowrap text-truncate"
                breakpoint="xl"
                @expand="expand"
                @collapse="collapse"
                @select="uid => $router.push({ path: '/mail/' + uid + '/' })"
            >
                <template v-slot="f">
                    <mail-folder-icon :folder="f.value" breakpoint="xl" class="flex-fill" />
                    <bm-counter-badge
                        v-if="f.value.unread > 0"
                        :value="f.value.unread"
                        :variant="f.value.uid != currentFolderUid ? 'secondary' : 'primary'"
                        class="mr-1"
                    />
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
            mailboxEmail: injector.getProvider("UserSession").get().defaultEmail
        };
    },
    computed: {
        ...mapGetters("mail-webapp", ["tree"]),
        ...mapState("mail-webapp", ["currentFolderUid"])
    },
    methods: {
        ...mapActions("mail-webapp", { expand: "expandFolder", collapse: "collapseFolder" })
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
}

.bm-counter-badge {
    position: absolute;
    right: 0;
}
</style>
