<template>
    <div class="mail-conversation-viewer-flags">
        <bm-icon v-if="message.flags.includes(Flag.FLAGGED)" class="text-warning" icon="flag-fill" size="lg" />
        <mail-attachment-icon class="ml-2" :message="message" size="lg" />
    </div>
</template>

<script>
import { Flag } from "@bluemind/email";
import { BmIcon } from "@bluemind/styleguide";
import MailAttachmentIcon from "../../MailAttachmentIcon";

export default {
    name: "MailConversationViewerFlags",
    components: { BmIcon, MailAttachmentIcon },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            Flag
        };
    }
};
</script>
