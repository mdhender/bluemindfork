<template>
    <div
        class="conversation-list-item-left d-flex flex-column align-items-center justify-content-between"
        :class="{ full: isMessageListStyleFull }"
    >
        <conversation-avatar
            v-if="isConversation"
            :class="[selectionMode === SELECTION_MODE.MONO ? '' : 'd-none']"
            :text="conversationSize > 99 ? '+99' : conversationSize"
            :font-size="conversationSize > 99 ? 'smaller' : 'unset'"
            :title="$t('mail.conversation.icon.title', { count: conversationSize })"
        />
        <bm-avatar v-else :class="[selectionMode === SELECTION_MODE.MONO ? '' : 'd-none']" :alt="fromOrTo" />
        <bm-check
            v-if="multiple"
            :class="[selectionMode === SELECTION_MODE.MONO ? 'd-none' : 'd-block']"
            :checked="selectionMode === SELECTION_MODE.MULTI && isSelected"
            @change="$emit('check')"
            @click.exact.native.stop
            @keyup.native.space.stop
        />

        <template v-if="!isConversation && isMessageListStyleFull">
            <bm-icon v-if="conversation.hasAttachment" class="mail-attachment-icon" icon="paper-clip" />
            <bm-icon v-if="conversation.hasICS" class="mail-attachment-icon" icon="calendar" />
        </template>
        <template v-else-if="!isConversation || isMessageListStyleFull">
            <mail-attachment-icon :message="conversation" />
        </template>
    </div>
</template>

<script>
import { BmAvatar, BmCheck, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { CONVERSATIONS_ACTIVATED, MY_DRAFTS, MY_SENT } from "~/getters";
import ConversationAvatar from "./ConversationAvatar";
import MailAttachmentIcon from "../MailAttachmentIcon";
import { SELECTION_MODE } from "./ConversationList";

export default {
    name: "ConversationListItemLeft",
    components: {
        BmAvatar,
        BmCheck,
        BmIcon,
        ConversationAvatar,
        MailAttachmentIcon
    },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        conversationSize: {
            type: Number,
            required: true
        },
        isSelected: {
            type: Boolean,
            required: true
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
        return { SELECTION_MODE };
    },
    computed: {
        ...mapState("mail", ["activeFolder", "folders"]),
        ...mapGetters("mail", { MY_DRAFTS, MY_SENT }),
        isMessageListStyleFull() {
            return this.$store.state.settings.mail_message_list_style === "full";
        },
        fromOrTo() {
            const conversationFolder = this.conversation.folderRef.key;
            const isSentOrDraftBox = [this.MY_DRAFTS.key, this.MY_SENT.key].includes(conversationFolder);
            if (isSentOrDraftBox) {
                const firstRecipient = this.conversation.to[0];
                return firstRecipient ? (firstRecipient.dn ? firstRecipient.dn : firstRecipient.address) : "";
            } else {
                return this.conversation.from.dn ? this.conversation.from.dn : this.conversation.from.address;
            }
        },
        isConversation() {
            return (
                this.$store.getters[`mail/${CONVERSATIONS_ACTIVATED}`] && this.conversation && this.conversationSize > 1
            );
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.conversation-list-item-left {
    min-width: $sp-2 + 1.3rem;

    $avatar-height: 2em;
    $icons-height: 1em;

    height: calc(#{$avatar-height} + #{$icons-height});

    &.full {
        height: calc(#{$avatar-height} + 2 * #{$icons-height});
    }

    .bm-avatar,
    .bm-check {
        height: $avatar-height;
    }

    .bm-check {
        // align with avatar
        transform: translateX(4px);
    }

    .mail-attachment-icon {
        color: $neutral-fg;
    }

    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }
}
</style>
