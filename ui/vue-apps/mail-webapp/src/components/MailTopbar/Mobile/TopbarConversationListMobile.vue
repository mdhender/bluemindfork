<template>
    <div class="topbar-conversation-list-mobile flex-fill" :class="{ darkened }">
        <bm-navbar class="main w-100">
            <bm-dropdown
                v-if="CURRENT_MAILBOX"
                variant="text-on-fill-primary"
                class="folder-menu-mobile text-truncate"
                size="lg"
                @show.prevent="$emit('showFolders')"
            >
                <template #button-content>
                    <bm-avatar class="session-avatar" :alt="userSession.formatedName" />
                    <bm-icon v-if="MY_MAILBOX === CURRENT_MAILBOX" :icon="folderIcon" />
                    <mail-mailbox-icon v-else :mailbox="CURRENT_MAILBOX" />
                    <div class="bold-tight text-truncate">{{ currentFolder.name }}</div>
                </template>
            </bm-dropdown>
            <div class="toolbar">
                <mail-search-box />
                <div class="options-for-mobile">
                    <messages-options-for-mobile @shown="darkened = true" @hidden="darkened = false" />
                </div>
            </div>
        </bm-navbar>

        <new-message class="new-mobile" :template="activeFolder === MY_TEMPLATES.key" mobile />
    </div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { inject } from "@bluemind/inject";
import { folderUtils } from "@bluemind/mail";
import { BmAvatar, BmDropdown, BmIcon, BmNavbar } from "@bluemind/ui-components";
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
        BmIcon,
        BmNavbar,
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
            return folderIcon(this.currentFolder.path, this.CURRENT_MAILBOX.type);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";
.topbar-conversation-list-mobile {
    .main {
        .folder-menu-mobile {
            flex: 1;
            align-self: stretch;
            .dropdown-toggle {
                min-width: 0;
                justify-content: flex-start;
                padding: 0 !important;
                gap: $sp-4 !important;

                .bm-avatar {
                    font-weight: $font-weight-normal;
                    &.session-avatar {
                        margin: 0 base-px-to-rem(6);
                    }
                }
            }
        }

        .toolbar {
            display: flex;
            gap: $sp-4;
            padding-left: $sp-3;
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
