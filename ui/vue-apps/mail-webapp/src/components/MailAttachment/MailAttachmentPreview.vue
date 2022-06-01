<template>
    <bm-modal id="mail-attachment-preview" ref="modal" centered size="fluid" hide-footer hide-header>
        <global-events @keydown.left="previous" @keydown.up="previous" @keydown.down="next" @keydown.right="next" />

        <preview-header
            :part="part"
            :message="message"
            :expanded.sync="expanded"
            @close="$refs.modal.hide()"
            @previous="previous"
            @next="next"
            @print="print"
            @download="download"
            @open="open"
        />
        <div class="content">
            <bm-collapse v-model="expanded" :class="{ 'd-none': true, 'd-lg-block': expanded }">
                <preview-message :message="message" :active-part="part" />
            </bm-collapse>
            <preview-attachment :message="message" :part="part" />
        </div>
        <preview-attachment-header :part="part" class="d-lg-none d-flex" />
    </bm-modal>
</template>

<script>
import { mapMutations } from "vuex";
import { BmCollapse, BmModal } from "@bluemind/styleguide";
import { getPartDownloadUrl, getPartPreviewUrl } from "@bluemind/email";
import { SET_PREVIEW_PART_ADDRESS } from "~/mutations";
import PreviewAttachment from "./Preview/PreviewAttachment";
import PreviewMessage from "./Preview/PreviewMessage";
import PreviewHeader from "./Preview/PreviewHeader";
import GlobalEvents from "vue-global-events";
import { isViewable } from "~/model/part";
import PreviewAttachmentHeader from "./Preview/PreviewAttachmentHeader";

export default {
    name: "MailAttachmentPreview",
    components: {
        BmCollapse,
        BmModal,
        PreviewAttachment,
        PreviewMessage,
        PreviewHeader,
        GlobalEvents,
        PreviewAttachmentHeader
    },
    data() {
        return { expanded: true };
    },
    computed: {
        message() {
            const message = this.$store.state.mail.conversations.messages[this.$store.state.mail.preview.messageKey];

            return message
                ? {
                      ...message,
                      composing: false
                  }
                : undefined;
        },
        part() {
            return this.message?.attachments.find(
                ({ address }) => this.$store.state.mail.preview.partAddress === address
            );
        },
        partIndex() {
            return this.message?.attachments.findIndex(
                ({ address }) => this.$store.state.mail.preview.partAddress === address
            );
        },
        attachmentsCount() {
            return this.message?.attachments.length;
        },
        downloadUrl() {
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part);
        },
        previewUrl() {
            return getPartPreviewUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part);
        }
    },
    methods: {
        ...mapMutations("mail", { SET_PREVIEW_PART_ADDRESS }),
        next() {
            this.selectAttachmentHelper(this.partIndex, index => index + 1);
        },
        previous() {
            this.selectAttachmentHelper(this.partIndex, index => index + this.attachmentsCount - 1);
        },
        selectAttachmentHelper(current, iterator) {
            const index = iterator(current) % this.attachmentsCount;
            if (index !== this.partIndex) {
                const attachment = this.message.attachments[index];
                isViewable(attachment)
                    ? this.SET_PREVIEW_PART_ADDRESS(attachment.address)
                    : this.selectAttachmentHelper(index, iterator);
            }
        },
        print() {
            const win = window.open(this.previewUrl);
            win.addEventListener("afterprint", () => win.close());
            win.addEventListener("load", () => win.print());
        },
        download() {
            window.open(this.downloadUrl);
        },
        open() {
            window.open(this.previewUrl);
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
    .preview-attachment {
        flex: 1 1 auto;
        min-height: 0;
    }
}
</style>
