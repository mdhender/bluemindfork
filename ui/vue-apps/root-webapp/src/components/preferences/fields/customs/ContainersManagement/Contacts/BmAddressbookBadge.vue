<template>
    <h1 class="d-inline">
        <bm-badge pill :closeable="closeable" class="mr-2" @close="$emit('close')">
            {{ addressbook.name }}
        </bm-badge>
    </h1>
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
