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
                :selected="currentFolder"
                node-id-key="uid"
                class="text-nowrap text-truncate"
                breakpoint="xl"
                @expand="expand"
                @collapse="collapse"
                @select="onSelect"
            >
                <template v-slot="f">
                    <bm-label-icon :icon="icon(f.value)" breakpoint="xl" class="flex-fill">
                        {{ f.value.name }}
                    </bm-label-icon>
                    <bm-counter-badge
                        v-if="f.value.uid === currentFolder && unreadCount > 0"
                        :value="unreadCount"
                        class="mr-1"
                    />
                </template>
            </bm-tree>
        </bm-collapse>
    </div>
</template>

<script>
import { BmButton, BmCollapse, BmCounterBadge, BmIcon, BmLabelIcon, BmTree } from "@bluemind/styleguide";
import { mapGetters, mapActions, mapMutations, mapState } from "vuex";
import injector from "@bluemind/inject";

export default {
    name: "MailFolderTree",
    components: {
        BmButton,
        BmCollapse,
        BmCounterBadge,
        BmIcon,
        BmLabelIcon,
        BmTree
    },
    data() {
        return {
            isMailboxExpanded: true,
            mailboxEmail: injector.getProvider("UserSession").get().defaultEmail
        };
    },
    computed: {
        ...mapGetters("backend.mail/folders", ["tree", "currentFolder"]),
        ...mapState("backend.mail/items", ["unreadCount"])
    },
    methods: {
        icon(f) {
            if (!f.parent) {
                switch (f.name) {
                    case "INBOX":
                        return "inbox";
                    case "Drafts":
                        return "pencil";
                    case "Trash":
                        return "trash";
                    case "Junk":
                        return "forbidden";
                    case "Outbox":
                        return "clock";
                    case "Sent":
                        return "paper-plane";
                }
            }
            return "folder";
        },
        ...mapActions("backend.mail/folders", ["expand", "collapse"]),
        ...mapMutations("backend.mail/items", ["setCurrent"]),
        onSelect(uid) {
            this.setCurrent(null);
            this.$router.push({ path: "/mail/" + uid + "/" });
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
}

.bm-counter-badge {
    position: absolute;
    right: 0;
}
</style>
