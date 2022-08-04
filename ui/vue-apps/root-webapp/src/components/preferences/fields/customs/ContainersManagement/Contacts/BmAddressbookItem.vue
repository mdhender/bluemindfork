<template>
    <div><bm-icon icon="addressbook" class="mr-1" />{{ addressbook.name }}</div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

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
