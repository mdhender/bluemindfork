<template>
    <div class="message-list-item-left d-flex flex-column align-items-center">
        <bm-avatar :alt="from" :class="[anyMessageSelected ? 'd-none' : '']" />
        <bm-check
            :checked="isMessageSelected(message.key)"
            :class="[anyMessageSelected ? 'd-block' : 'd-none']"
            @click.exact.native.prevent.stop="$emit('toggleSelect', message.key, true)"
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
        from() {
            return this.message.from.dn ? this.message.from.dn : this.message.from.address;
        },
        anyMessageSelected() {
            return this.selectedMessageKeys.length > 0;
        },
        state() {
            return this.message.states
                .map(state => STATE_COMPONENT[state])
                .filter(state => !!state)
                .sort((a, b) => a.order < b.order)
                .shift();
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
