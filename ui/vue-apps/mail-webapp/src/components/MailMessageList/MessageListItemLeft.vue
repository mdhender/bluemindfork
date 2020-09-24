<template>
    <div class="message-list-item-left d-flex flex-column align-items-center">
        <bm-avatar :alt="fromOrTo" :class="[anyMessageSelected ? 'd-none' : '']" />
        <bm-check
            :checked="isMessageSelected(message.key)"
            :class="[anyMessageSelected ? 'd-block' : 'd-none']"
            @change="$emit('toggleSelect', message.key, true)"
            @click.exact.native.stop
            @keyup.native.space.stop
        />
        <component :is="state" v-if="!!state" class="states" />
    </div>
</template>

<script>
const STATE_COMPONENT = {
    ["has-attachment"]: {
        components: { BmIcon },
        template: '<bm-icon icon="paper-clip"/>',
        priority: 99
    },
    ["is-ics"]: {
        components: { BmIcon },
        template: '<bm-icon icon="event"/>'
    }
};

import { BmAvatar, BmCheck, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";

export default {
    name: "MessageListItemLeft",
    components: {
        BmAvatar,
        BmCheck
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
        ...mapState("mail", ["messages"]),
        anyMessageSelected() {
            return this.selectedMessageKeys.length > 0;
        },
        state() {
            return this.message.states
                .map(state => STATE_COMPONENT[state])
                .filter(state => !!state)
                .sort((a, b) => a.order < b.order)
                .shift();
        },

        fromOrTo() {
            const messageFolder = this.messages[this.message.key].folderRef.key;
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
