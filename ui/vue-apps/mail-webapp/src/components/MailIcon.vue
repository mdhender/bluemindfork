<template>
    <bm-extension id="webapp.mail" type="chain-of-responsibility" path="list.conversation.icon" :conversation="message">
        <bm-icon
            v-if="message.hasAttachment || message.hasICS"
            class="mail-icon"
            :icon="message.hasICS ? 'calendar' : 'paper-clip'"
        />
    </bm-extension>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
import { BmExtension } from "@bluemind/extensions.vue";

export default {
    name: "MailIcon",
    components: {
        BmIcon,
        BmExtension
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        icon() {
            return this.message.hasAttachment && (this.message.hasICS ? "calendar" : "paper-clip");
        }
    }
};
</script>
