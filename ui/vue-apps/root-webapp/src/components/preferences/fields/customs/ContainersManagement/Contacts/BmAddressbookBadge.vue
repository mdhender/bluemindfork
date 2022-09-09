<template>
    <bm-badge pill :closeable="closeable" class="bm-addressbook-badge" @close="$emit('close')">
        {{ addressbook.name }}
    </bm-badge>
</template>

<script>
import { BmBadge } from "@bluemind/styleguide";

export default {
    name: "BmAddressbookBadge",
    components: { BmBadge },
    props: {
        addressbook: {
            type: Object,
            required: true
        },
        closeable: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_type";
@import "~@bluemind/styleguide/css/_variables";

.badge.bm-addressbook-badge {
    padding: $sp-3 $sp-4 $sp-3 $sp-5;
    gap: $sp-4;
    @extend %caption-bold;
}
</style>
