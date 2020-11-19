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
            if (Array.isArray(this.alert.payload)) {
                return this.alert.payload[0].subject;
            } else {
                return this.alert.payload.subject;
            }
        }
    }
};
</script>
