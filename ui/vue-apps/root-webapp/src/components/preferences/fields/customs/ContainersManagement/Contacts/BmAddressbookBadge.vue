<template>
    <bm-badge pill :closeable="closeable" class="bm-addressbook-badge" @close="$emit('close')">
        {{ addressbook.name }}
    </bm-badge>
</template>

<script>
import { BmBadge } from "@bluemind/ui-components";

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
@import "~@bluemind/ui-components/src/css/_type";
@import "~@bluemind/ui-components/src/css/variables";

.badge.bm-addressbook-badge {
    padding: $sp-3 $sp-4 $sp-3 $sp-5;
    gap: $sp-4;
    @extend %caption-bold;
}
</style>
