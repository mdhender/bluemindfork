<template>
    <div class="message-list-item-left d-flex flex-column align-items-center">
        <bm-avatar :alt="fromOrTo" :class="[IS_SELECTION_EMPTY ? '' : 'd-none']" />
        <bm-check
            :checked="IS_MESSAGE_SELECTED(message.key)"
            :class="[IS_SELECTION_EMPTY ? 'd-none' : 'd-block']"
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
import { mapGetters } from "vuex";

import { IS_MESSAGE_SELECTED, IS_SELECTION_EMPTY } from "../../store/types/getters";
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
        ...mapGetters("mail", { IS_MESSAGE_SELECTED, IS_SELECTION_EMPTY }),
        ...mapGetters("mail", ["MY_DRAFTS", "MY_SENT"]),

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
