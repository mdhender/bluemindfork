<template>
    <bm-list-group-item
        v-touch:touchhold="() => $emit('check')"
        class="d-flex conversation-list-item"
        :class="{
            ['conversation-list-item-' + messageListStyle]: true,
            'not-seen': isUnread,
            'warning-custom': isFlagged,
            active: isSelected
        }"
        role="link"
    >
        <screen-reader-only-conversation-information :conversation="conversation" />
        <conversation-list-item-left
            :conversation="conversation"
            :conversation-size="conversationSize"
            :is-selected="isSelected"
            :multiple="multiple"
            :selection-mode="selectionMode"
            @check="$emit('check')"
        />
        <conversation-list-item-middle
            class="flex-fill px-2"
            :conversation="conversation"
            :conversation-size="conversationSize"
            :is-important="isFlagged"
            :mouse-in="mouseIn"
        />
        <conversation-list-item-actions>
            <template v-slot:actions> <slot name="actions" /> </template>
        </conversation-list-item-actions>
    </bm-list-group-item>
</template>

<script>
import { BmListGroupItem } from "@bluemind/styleguide";
import { mapState } from "vuex";
import ConversationListItemLeft from "./ConversationListItemLeft";
import ConversationListItemMiddle from "./ConversationListItemMiddle";
import ConversationListItemActions from "./ConversationListItemActions";
import ScreenReaderOnlyConversationInformation from "./ScreenReaderOnlyConversationInformation";
import { isFlagged, isUnread } from "~/model/message";

export default {
    name: "ConversationListItem",
    components: {
        BmListGroupItem,
        ConversationListItemLeft,
        ConversationListItemActions,
        ConversationListItemMiddle,
        ScreenReaderOnlyConversationInformation
    },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isSelected: {
            type: Boolean,
            required: false,
            default: false
        },
        multiple: {
            type: Boolean,
            required: true
        },
        selectionMode: {
            type: String,
            required: true
        }
    },
    data() {
        return { mouseIn: false };
    },
    computed: {
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        isUnread() {
            return isUnread(this.conversation);
        },
        isFlagged() {
            return isFlagged(this.conversation);
        },
        conversationSize() {
            return this.conversation.messages.length;
        },
        messageListStyle() {
            return this.$store.state.settings.mail_message_list_style;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.conversation-list-item {
    cursor: pointer;
    border-left: transparent solid 4px !important;

    &.not-seen {
        border-left: $secondary-fg 4px solid !important;
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

    &.warning-custom:not(.active) {
        background-color: $warning-bg-lo1;
        &:hover {
            background-color: $warning-bg;
        }
    }

    &:hover {
        background-color: $neutral-bg-lo1;
        color: $neutral-fg-hi1;
        &.active {
            background-color: $secondary-bg;
        }
    }
    &.active:focus {
        background-color: $secondary-bg;
    }

    &[tabindex="-1"]:focus {
        outline: $outline !important;
        outline-offset: -1px;
    }
}
</style>
