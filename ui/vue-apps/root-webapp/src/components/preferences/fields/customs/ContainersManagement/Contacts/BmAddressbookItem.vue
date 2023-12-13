<template>
    <div class="d-flex align-items-center">
        <bm-icon icon="address-book" class="mr-4" />
        <div class="text-truncate">{{ addressbook.name }}</div>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";

export default {
    name: "BmAddressbookItem",
    components: { BmIcon },
    props: {
        addressbook: {
            type: Object,
            required: true
        }
    }
};
</script>
