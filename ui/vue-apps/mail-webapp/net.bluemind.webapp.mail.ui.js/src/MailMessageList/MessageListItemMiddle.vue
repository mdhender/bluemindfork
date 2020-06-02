<template>
    <div class="message-list-item-middle d-flex flex-column text-truncate">
        <div v-bm-tooltip.ds500.viewport :title="from" class="mail-message-list-item-sender h3 text-dark text-truncate">
            {{ from }}
        </div>
        <div
            v-bm-tooltip.ds500.bottom.viewport
            :title="message.subject"
            class="mail-message-list-item-subject text-secondary text-truncate"
        >
            {{ message.subject }}
        </div>
        <div
            v-bm-tooltip.ds500.bottom.viewport
            :title="message.preview"
            class="mail-message-list-item-preview text-dark text-condensed text-truncate"
        >
            {{ message.preview || "&nbsp;" }}
        </div>
    </div>
</template>

<script>
import { BmTooltip } from "@bluemind/styleguide";

export default {
    name: "MessageListItemLeft",
    directives: { BmTooltip },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            tooltip: {
                cursor: "cursor",
                text: this.$t("mail.actions.move")
            }
        };
    },
    computed: {
        from() {
            return this.message.from.dn ? this.message.from.dn : this.message.from.address;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item-middle {
    .custom-control-label::after,
    .custom-control-label::before {
        top: 0.2rem !important;
    }
}
</style>
