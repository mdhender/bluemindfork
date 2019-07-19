<template>
    <div class="mail-folder-tree">
        <bm-button 
            variant="link" 
            class="collapse-mailbox-btn d-none d-xl-flex align-items-center pb-1"
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
                class="ml-2 text-nowrap text-truncate"
                node-id-key="uid"
                breakpoint="xl"
                @expand="expand"
                @collapse="collapse"
            >
                <template v-slot="f">
                    <router-link
                        :to="'/mail/' + f.value.uid + '/'"
                        :class="{active: f.value.selected}"
                        active-class="router-link-active"
                        tag="span"
                        style="cursor:pointer;"
                    >
                        <bm-label-icon :icon="icon(f.value)" breakpoint="xl">{{ f.value.name }}</bm-label-icon>
                        <bm-counter-badge 
                            v-if="f.value.uid === currentFolder && countUnreadMessages > 0" 
                            :value="countUnreadMessages" 
                            class="float-right mr-3" 
                        />
                    </router-link>
                </template>
            </bm-tree>
        </bm-collapse>
    </div>
</template>

<script>
import { BmButton, BmCollapse, BmCounterBadge, BmIcon, BmLabelIcon, BmTree } from "@bluemind/styleguide";
import { mapGetters, mapActions } from "vuex";
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
            mailboxEmail: injector.getProvider('UserSession').get().defaultEmail
        };
    },
    computed: {
        ...mapGetters("backend.mail/folders", ["tree"]),
        ...mapGetters("backend.mail/items", ["countUnreadMessages"]),
        ...mapGetters("backend.mail/folders", {currentFolder: "currentFolder"})
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
        ...mapActions("backend.mail/folders", ["expand", "collapse"])
    }
};
</script>
<style lang="scss">
    @import '~@bluemind/styleguide/css/_variables';
    .bm-tree-node-active, .bm-tree-node-active .btn {
        color: $info-dark;
    }

    .mail-folder-tree button.collapse-mailbox-btn {
        color: $info-dark;
        text-decoration-line: none;
    }
</style>
