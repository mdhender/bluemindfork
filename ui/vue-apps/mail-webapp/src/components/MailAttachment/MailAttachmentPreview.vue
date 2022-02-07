<template>
    <bm-modal id="mail-attachment-preview" centered size="xl" hide-footer hide-header class="d-flex">
        <preview-message :message="message" />
        <preview-attachment :message="message" :part="part" @close="$bvModal.hide('mail-attachment-preview')" />
    </bm-modal>
</template>

<script>
import { BmModal } from "@bluemind/styleguide";

import PreviewAttachment from "./Preview/PreviewAttachment";
import PreviewMessage from "./Preview/PreviewMessage";

export default {
    name: "MailAttachmentPreview",
    components: { BmModal, PreviewAttachment, PreviewMessage },
    computed: {
        message() {
            return this.$store.state.mail.conversations.messages[this.$store.state.mail.preview.messageKey];
        },
        part() {
            return this.message?.attachments.find(
                ({ address }) => this.$store.state.mail.preview.partAddress === address
            );
        }
    }
};
</script>

<style lang="scss">
#mail-attachment-preview {
    .modal-body {
        height: 80vh;
        display: flex;
        padding: 0;
    }

    .preview-message {
        flex-basis: 25%;
        max-width: 25%;
        flex-grow: 0;
        flex-shrink: 0;
    }
    .preview-attachment {
        flex: 1 1 auto;
    }
}
</style>
