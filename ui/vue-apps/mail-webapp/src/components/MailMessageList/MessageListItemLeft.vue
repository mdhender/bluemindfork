<template>
    <div class="message-list-item-left d-flex flex-column align-items-center">
        <bm-avatar :alt="fromOrTo" :class="[SELECTION_IS_EMPTY ? '' : 'd-none']" />
        <bm-check
            :checked="MESSAGE_IS_SELECTED(message.key)"
            :class="[SELECTION_IS_EMPTY ? 'd-none' : 'd-block']"
            @change="$emit('toggle-select', message.key, true)"
            @click.exact.native.stop
            @keyup.native.space.stop
        />
        <template v-if="userSettings.mail_message_list_style === 'full'">
            <bm-icon v-if="message.hasAttachment" icon="paper-clip" />
            <bm-icon v-if="message.hasICS" icon="event" />
        </template>
        <template v-else>
            <bm-icon v-if="message.hasAttachment || message.hasICS" :icon="message.hasICS ? 'event' : 'paper-clip'" />
        </template>
    </div>
</template>

<script>
import { BmAvatar, BmCheck, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { MY_DRAFTS, MY_SENT } from "~getters";

import { MESSAGE_IS_SELECTED, SELECTION_IS_EMPTY } from "~getters";
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
        ...mapGetters("mail", { MESSAGE_IS_SELECTED, SELECTION_IS_EMPTY }),
        ...mapGetters("mail", { MY_DRAFTS, MY_SENT }),
        ...mapState("session", { userSettings: ({ settings }) => settings.remote }),

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

    $avatar-height: 2em !important;

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
