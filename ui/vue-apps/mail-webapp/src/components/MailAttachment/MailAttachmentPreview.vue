<template>
    <bm-modal id="mail-attachment-preview" ref="modal" centered size="fluid" hide-footer hide-header>
        <preview-header :part="part" :expanded.sync="expanded" @close="$refs.modal.hide()" />
        <div class="content">
            <bm-collapse v-model="expanded">
                <preview-message :message="message" />
            </bm-collapse>
            <preview-attachment :message="message" :part="part" />
        </div>
    </bm-modal>
</template>

<script>
import { BmCollapse, BmModal } from "@bluemind/styleguide";

import PreviewAttachment from "./Preview/PreviewAttachment";
import PreviewMessage from "./Preview/PreviewMessage";
import PreviewHeader from "./Preview/PreviewHeader";

export default {
    name: "MailAttachmentPreview",
    components: { BmCollapse, BmModal, PreviewAttachment, PreviewMessage, PreviewHeader },
    data() {
        return { expanded: true };
    },
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
@import "~@bluemind/styleguide/css/variables";
#mail-attachment-preview .modal-body {
    .content {
        display: flex;
        flex: 1 1 auto;
        min-height: 0;
    }
    height: 80vh;
    flex-direction: column;
    display: flex;
    padding: 0;
    .preview-message-header,
    .collapse {
        flex-basis: 25%;
        max-width: 25%;
        flex-grow: 0;
        flex-shrink: 0;
    }
    .collapse {
        overflow: auto;
    }
    .preview-attachment-header,
    .preview-attachment {
        flex: 1 1 auto;
        min-height: 0;
    }
}
</style>
