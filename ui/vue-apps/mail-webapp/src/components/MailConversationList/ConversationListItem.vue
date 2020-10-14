<template>
    <bm-list-group-item
        v-touch:touchhold="onTouch"
        class="d-flex conversation-list-item"
        :class="{
            ['conversation-list-item-' + userSettings.mail_message_list_style]: true,
            'not-seen': isUnread,
            'warning-custom': isFlagged,
            active: CONVERSATION_IS_SELECTED(conversation.key) || IS_CURRENT_CONVERSATION(conversation)
        }"
        role="link"
        @click.exact="navigateTo"
        @keyup.enter.exact="navigateTo"
        @mouseenter="mouseIn = true"
        @mouseleave="mouseIn = false"
    >
        <screen-reader-only-conversation-information :conversation="conversation" />
        <conversation-list-item-left
            :conversation="conversation"
            :conversation-size="conversationSize"
            @toggle-select="$emit('toggle-select', conversation.key, true)"
        />
        <conversation-list-item-middle
            class="flex-fill px-2"
            :conversation="conversation"
            :conversation-size="conversationSize"
            :is-important="isFlagged"
            :mouse-in="mouseIn"
        />
        <conversation-list-item-quick-action-buttons v-show="mouseIn" :conversation="conversation" />
    </bm-list-group-item>
</template>

<script>
import { BmListGroupItem } from "@bluemind/styleguide";
import { mapGetters, mapMutations, mapState } from "vuex";
import ConversationListItemLeft from "./ConversationListItemLeft";
import ConversationListItemMiddle from "./ConversationListItemMiddle";
import ConversationListItemQuickActionButtons from "./ConversationListItemQuickActionButtons";
import ScreenReaderOnlyConversationInformation from "./ScreenReaderOnlyConversationInformation";
import { CONVERSATION_IS_SELECTED, CONVERSATION_MESSAGE_BY_KEY, IS_CURRENT_CONVERSATION, MY_SENT } from "~/getters";
import { SET_CURRENT_CONVERSATION } from "~/mutations";
import { isFlagged, isUnread } from "~/model/message";
import { removeSentDuplicates } from "~/model/conversations";

export default {
    name: "ConversationListItem",
    components: {
        BmListGroupItem,
        ConversationListItemLeft,
        ConversationListItemMiddle,
        ConversationListItemQuickActionButtons,
        ScreenReaderOnlyConversationInformation
    },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isMuted: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            mouseIn: false
        };
    },
    computed: {
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapGetters("mail", {
            CONVERSATION_IS_SELECTED,
            CONVERSATION_MESSAGE_BY_KEY,
            IS_CURRENT_CONVERSATION,
            MY_SENT
        }),
        ...mapState("session", { userSettings: ({ settings }) => settings.remote }),
        isUnread() {
            return isUnread(this.conversation);
        },
        isFlagged() {
            return isFlagged(this.conversation);
        },
        conversationSize() {
            return removeSentDuplicates(this.CONVERSATION_MESSAGE_BY_KEY(this.conversation.key), this.MY_SENT).length;
        }
    },
    methods: {
        ...mapMutations("mail", { SET_CURRENT_CONVERSATION }),
        onTouch() {
            this.$emit("toggleSelect", this.conversation.key);
        },
        navigateTo() {
            if (this.conversationSize === 1) {
                this.SET_CURRENT_CONVERSATION(this.conversation);
                this.$router.navigate({
                    name: "v:mail:message",
                    params: { message: this.conversation.messages[0] }
                });
            } else {
                this.$router.navigate({
                    name: "v:mail:conversation",
                    params: { conversation: this.conversation }
                });
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.conversation-list-item {
    border-left: transparent solid 4px !important;
    .states .fa-event {
        color: $calendar-color;
    }
    &.not-seen {
        border-left: theme-color("primary") 4px solid !important;
    }

    &.not-seen .mail-conversation-list-item-sender,
    &.not-seen .mail-conversation-list-item-subject {
        font-weight: $font-weight-bold;
    }

    &.conversation-list-item-full {
        padding-top: $sp-1 !important;
        padding-bottom: $sp-1 !important;
        .mail-conversation-list-item-subject,
        .mail-conversation-list-item-date {
            line-height: $line-height-sm;
        }
        .mail-conversation-list-item-preview {
            display: block;
        }
    }

    &.conversation-list-item-compact {
        padding-top: $sp-1 !important;
        padding-bottom: $sp-1 !important;
        .mail-conversation-list-item-subject,
        .mail-conversation-list-item-date {
            line-height: $line-height-sm;
        }
        .mail-conversation-list-item-preview {
            display: none;
        }
    }

    &.conversation-list-item-normal .mail-conversation-list-item-preview,
    &.conversation-list-item-null .mail-conversation-list-item-preview {
        display: none;
    }

    // obtain the same enlightment that BAlert applies on $warning TODO move to variables.scss in SG
    $custom-warning-color: theme-color-level("warning", $alert-bg-level);

    &.warning-custom:not(.active) {
        background-color: $custom-warning-color;
    }
}
</style>
