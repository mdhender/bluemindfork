<template>
    <div class="conversation-list-item-left" :class="{ full: isMessageListStyleFull }">
        <div class="avatar-or-check-wrapper">
            <bm-avatar
                size="sm"
                :class="[selectionMode === SELECTION_MODE.MONO ? '' : 'd-none']"
                :alt="isConversation ? $t('mail.conversation.icon.title', { count: conversationSize }) : fromOrTo"
                :count="isConversation ? conversationSize : null"
            />
            <bm-check
                v-if="multiple"
                :class="[selectionMode === SELECTION_MODE.MONO ? 'd-none' : 'd-block']"
                :checked="selectionMode === SELECTION_MODE.MULTI && isSelected"
                @change="$emit('check')"
                @click.exact.native.stop
                @keyup.native.space.stop
            />
        </div>

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
import { BmAvatar, BmCheck, BmIcon } from "@bluemind/ui-components";
import { mapGetters, mapState } from "vuex";
import { CONVERSATIONS_ACTIVATED, MY_DRAFTS, MY_SENT } from "~/getters";
import MailAttachmentIcon from "../MailAttachmentIcon";
import { SELECTION_MODE } from "./ConversationList";

export default {
    name: "ConversationListItemLeft",
    components: {
        BmAvatar,
        BmCheck,
        BmIcon,
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
                const firstRecipient = this.conversation.to[0] || this.conversation.cc[0] || this.conversation.bcc[0];
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
@use "sass:math";
@import "~@bluemind/ui-components/src/css/variables";

.conversation-list-item-left {
    display: flex;
    flex-direction: column;
    align-items: center;

    .mail-attachment-icon {
        color: $neutral-fg;
    }
}

.conversation-list-item-left {
    gap: 0;

    .avatar-or-check-wrapper {
        display: flex;
        align-items: center;
        justify-content: center;

        width: base-px-to-rem(20);
        height: base-px-to-rem(20);

        .bm-check {
            top: base-px-to-rem(2);
            left: base-px-to-rem(4);
        }
    }
}

.conversation-list-item-normal .conversation-list-item-left {
    gap: base-px-to-rem(1);

    .avatar-or-check-wrapper {
        height: base-px-to-rem(24);
    }
}
</style>
