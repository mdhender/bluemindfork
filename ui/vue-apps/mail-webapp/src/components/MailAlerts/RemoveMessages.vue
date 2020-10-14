<template>
    <default-alert :alert="alert" :options="{ subject }" />
</template>
<script>
import { DefaultAlert } from "@bluemind/alert.store";

export default {
    name: "RemoveMessages",
    components: { DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        subject() {
            const payload = this.alert.payload;
            const firstItem = payload.conversations?.[0] || payload.messages?.[0] || payload[0] || payload;
            return firstItem.subject;
        }
    }
};
</script>
