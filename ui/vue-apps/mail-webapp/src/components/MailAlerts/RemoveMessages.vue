<template>
    <default-alert :alert="alert" :options="{ subject }" :count="count" />
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
            const firstItem = payload.messages?.[0] || payload.conversations?.[0] || payload[0] || payload;
            return firstItem.subject;
        },

        count() {
            const payload = this.alert.payload;
            return payload.messages?.length || payload.conversations?.length || 1;
        }
    }
};
</script>
