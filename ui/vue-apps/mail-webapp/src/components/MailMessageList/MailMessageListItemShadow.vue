<template>
    <div class="mail-message-list-item-shadow">
        <mail-message-list-item-shadow-single v-if="count <= 1" :message="message" />
        <mail-message-list-item-shadow-multiple v-else :count="count" />
    </div>
</template>
<script>
import MailMessageListItemShadowSingle from "./MailMessageListItemShadowSingle";
import MailMessageListItemShadowMultiple from "./MailMessageListItemShadowMultiple";

export default {
    name: "MailMessageListItemShadow",
    components: { MailMessageListItemShadowSingle, MailMessageListItemShadowMultiple },
    props: {
        message: {
            type: Object,
            required: true
        },
        count: {
            type: Number,
            required: true
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
@import "~@bluemind/styleguide/css/mixins";

$shadow-width: 240px;

.mail-message-list-item-shadow {
    width: $shadow-width;
    background-color: $surface-bg;
}

/** @see dragAndDrop/plugins/tooltip.js */
.bm-drag-tooltip {
    max-width: $shadow-width;
    @include text-overflow;
}
</style>
