<template>
    <div class="message-list-item-left d-flex flex-column align-items-center">
        <bm-avatar :alt="fromOrTo" :class="[anyMessageSelected ? 'd-none' : '']" />
        <bm-check
            :checked="isMessageSelected(message.key)"
            :class="[anyMessageSelected ? 'd-block' : 'd-none']"
            @change="$emit('toggle-select', message.key, true)"
            @click.exact.native.stop
            @keyup.native.space.stop
        />
        <bm-icon v-if="message.hasAttachment" icon="paper-clip" />
        <bm-icon v-if="message.hasICS" icon="event" />
    </div>
</template>

<script>
import { BmAvatar, BmCheck, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";

export default {
    name: "MessageListItemLeft",
    components: {
        BmAvatar,
        BmCheck,
        BmIcon
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapGetters("mail-webapp", ["isMessageSelected"]),
        ...mapGetters("mail", ["MY_DRAFTS", "MY_SENT"]),
        anyMessageSelected() {
            return this.selectedMessageKeys.length > 0;
        },

        fromOrTo() {
            const messageFolder = this.message.folderRef.key;
            const isSentOrDraftBox = [this.MY_DRAFTS.key, this.MY_SENT.key].includes(messageFolder);
            if (isSentOrDraftBox) {
                const firstRecipient = this.message.to[0];
                return firstRecipient ? (firstRecipient.dn ? firstRecipient.dn : firstRecipient.address) : "";
            } else {
                return this.message.from.dn ? this.message.from.dn : this.message.from.address;
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item-left {
    min-width: $sp-2 + 1.3rem;

    $avatar-height: 1.3rem !important;

    .bm-avatar {
        height: $avatar-height;
    }

    .bm-check {
        height: $avatar-height;
        // align with avatar
        transform: translateX(4px);
    }

    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }
}
</style>
