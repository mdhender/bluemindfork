<template>
    <default-alert
        :alert="alert"
        :options="{ subject: alert.payload.messages[0].subject || $t('mail.viewer.no.subject') }"
        :count="alert.payload.messages.length"
    />
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
    }
};
</script>
