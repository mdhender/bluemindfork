<template>
    <bm-list-group-item
        v-touch:touchhold="() => $emit('check')"
        class="conversation-list-item"
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
            :message-list-style="messageListStyle"
            @check="$emit('check')"
        />
        <conversation-list-item-middle
            class="flex-fill"
            :conversation="conversation"
            :conversation-size="conversationSize"
            :is-important="isFlagged"
            :mouse-in="mouseIn"
        />
        <conversation-list-item-actions>
            <template #actions> <slot name="actions" /> </template>
        </conversation-list-item-actions>
    </bm-list-group-item>
</template>

<script>
import { BmListGroupItem } from "@bluemind/ui-components";
import { mapState } from "vuex";
import { messageUtils } from "@bluemind/mail";
import ConversationListItemLeft from "./ConversationListItemLeft";
import ConversationListItemMiddle from "./ConversationListItemMiddle";
import ConversationListItemActions from "./ConversationListItemActions";
import ScreenReaderOnlyConversationInformation from "./ScreenReaderOnlyConversationInformation";

const { isFlagged, isUnread } = messageUtils;

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
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "./variables.scss";

.list-group-item.conversation-list-item {
    cursor: pointer;

    border-left: $not-seen-border-width solid transparent !important;
    &.not-seen {
        border-left-color: $secondary-fg !important;
    }

    height: initial;
    align-items: flex-start;
    gap: $sp-5;

    padding: base-px-to-rem(5) $sp-5 base-px-to-rem(7);

    &.conversation-list-item-full,
    &.conversation-list-item-compact {
        padding-top: base-px-to-rem(4);
        padding-bottom: base-px-to-rem(3);
    }

    &.conversation-list-item-normal .conversation-list-item-left {
        gap: base-px-to-rem(1);

        .avatar-or-check-wrapper {
            height: base-px-to-rem(24);
        }
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
