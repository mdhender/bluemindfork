<template>
    <bm-icon
        v-if="message.hasAttachment || message.hasICS"
        class="mail-icon"
        :icon="message.hasICS ? 'calendar' : 'paper-clip'"
    />
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";

export default {
    name: "MailIcon",
    components: {
        BmIcon
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    }
};
</script>
