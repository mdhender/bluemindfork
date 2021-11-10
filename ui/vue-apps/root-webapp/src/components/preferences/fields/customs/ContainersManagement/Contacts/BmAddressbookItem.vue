<template>
    <div><bm-icon icon="addressbook" size="lg" />{{ addressbook.name }}</div>
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
