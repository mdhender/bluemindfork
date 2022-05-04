<template>
    <default-alert
        :alert="alert"
        :options="{ subject: alert.payload.conversations[0].subject || $t('mail.viewer.no.subject') }"
        :count="alert.payload.conversations.length"
    />
</template>
<script>
import { DefaultAlert } from "@bluemind/alert.store";

export default {
    name: "RemoveConversations",
    components: { DefaultAlert },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
