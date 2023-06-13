<template>
    <div class="conversation-list-item-left">
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
        <div class="icon-wrapper">
            <message-icon :message="conversation" />
        </div>
    </div>
</template>

<script>
import { BmAvatar, BmCheck } from "@bluemind/ui-components";
import { mapGetters, mapState } from "vuex";
import { CONVERSATIONS_ACTIVATED, MY_DRAFTS, MY_SENT } from "~/getters";
import MessageIcon from "../MessageIcon/MessageIcon";
import { SELECTION_MODE } from "./ConversationList";

export default {
    name: "ConversationListItemLeft",
    components: {
        BmAvatar,
        BmCheck,
        MessageIcon
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
        messageListStyle: {
            type: String,
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
        },
        isMessageListStyleFull() {
            return this.messageListStyle === "full";
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables";

.conversation-list-item-left {
    display: flex;
    flex-direction: column;
    align-items: center;
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

    .icon-wrapper {
        width: base-px-to-rem(16);
        height: base-px-to-rem(16);
        display: flex;
        align-items: center;
        .message-icon {
            margin: auto;
        }
    }
}
</style>
