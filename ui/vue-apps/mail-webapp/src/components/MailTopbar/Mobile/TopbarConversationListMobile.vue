<template>
    <div class="topbar-conversation-list-mobile flex-fill" :class="{ darkened }">
        <div class="main w-100">
            <bm-dropdown
                v-if="CURRENT_MAILBOX"
                variant="text-on-fill-primary"
                class="folder-menu-mobile"
                size="lg"
                @show.prevent="$emit('showFolders')"
            >
                <template #button-content>
                    <bm-avatar :alt="userSession.formatedName" />
                    <bm-label-icon v-if="MY_MAILBOX === CURRENT_MAILBOX" :icon="folderIcon" class="ml-4 flex-fill">
                        {{ currentFolder.name }}
                    </bm-label-icon>
                    <template v-else>
                        <mail-mailbox-icon class="ml-4" :mailbox="CURRENT_MAILBOX" />
                        <span class="ml-3">{{ currentFolder.name }}</span>
                    </template>
                </template>
            </bm-dropdown>
            <div class="d-flex align-items-center toolbar">
                <mail-search-box />
                <div class="options-for-mobile">
                    <messages-options-for-mobile @shown="darkened = true" @hidden="darkened = false" />
                </div>
            </div>
        </div>

        <new-message class="new-mobile" :template="activeFolder === MY_TEMPLATES.key" mobile />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { folderUtils } from "@bluemind/mail";
import { BmAvatar, BmDropdown, BmLabelIcon } from "@bluemind/ui-components";
import { CURRENT_MAILBOX, MY_MAILBOX, MY_TEMPLATES } from "~/getters";
import MailMailboxIcon from "../../MailMailboxIcon";
import MailSearchBox from "../../MailSearch/MailSearchBox";
import MessagesOptionsForMobile from "../../MessagesOptionsForMobile";
import NewMessage from "../../NewMessage";
const { folderIcon } = folderUtils;

export default {
    components: {
        BmAvatar,
        BmDropdown,
        BmLabelIcon,
        MailMailboxIcon,
        MessagesOptionsForMobile,
        NewMessage,
        MailSearchBox
    },
    data() {
        return {
            userSession: inject("UserSession"),
            darkened: false
        };
    },
    computed: {
        ...mapState("mail", {
            currentConversation: ({ conversations }) => conversations.currentConversation,
            activeFolder: "activeFolder",
            folders: "folders",
            conversations: "conversations"
        }),
        ...mapGetters("mail", {
            CURRENT_MAILBOX,
            MY_MAILBOX,
            MY_TEMPLATES
        }),
        currentFolder() {
            return this.folders[this.activeFolder];
        },
        folderIcon() {
            return folderIcon(this.currentFolder.imapName, this.CURRENT_MAILBOX.type);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "~@bluemind/ui-components/src/css/mixins/_responsiveness";
@import "~@bluemind/ui-components/src/css/_type";
.topbar-conversation-list-mobile {
    .main {
        display: flex;
        justify-content: space-between;
        .toolbar {
            gap: $sp-4;
        }
        .bm-avatar {
            font-weight: $font-weight-normal;
        }
        .bm-dropdown {
            max-width: 65%;
            .dropdown-toggle {
                padding: $sp-4 base-px-to-rem(6) !important;
                flex: 1 1 auto;
                min-width: 0;
                .bm-label-icon > div {
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }
        }
    }

    &.darkened::before {
        position: fixed;
        content: "";
        background: $modal-backdrop;
        top: 0;
        bottom: 0;
        width: 100%;
        z-index: 1;
    }
}
</style>
