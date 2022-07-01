<template>
    <div class="preview-header flex-column flex-lg-row">
        <preview-attachment-header :part="part" :message="message" class="d-none d-lg-flex" />
        <bm-button-toolbar class="order-0 order-lg-2 justify-content-around justify-content-lg-start">
            <bm-button
                variant="simple-neutral"
                :title="
                    $t('mail.content.print', {
                        fileType: $t('mail.content.' + fileTypeIcon),
                        name: part.fileName
                    })
                "
                @click="print"
            >
                <bm-icon icon="printer" size="lg" />
            </bm-button>
            <bm-button
                :href="downloadUrl"
                variant="simple-neutral"
                :download="part.fileName"
                class="d-flex align-items-center"
                :title="
                    $t('mail.content.download', {
                        fileType: $t('mail.content.' + fileTypeIcon),
                        name: part.fileName
                    })
                "
            >
                <bm-icon icon="download" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :title="$t('mail.content.open-new-tab', { name: part.fileName })"
                @click="open"
            >
                <bm-icon icon="popup" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :disabled="message.attachments.length <= 1"
                tab-index="0"
                :title="$t('mail.preview.previous')"
                @click="$emit('previous')"
            >
                <bm-icon icon="chevron-left" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :title="$t('mail.preview.next')"
                :disabled="message.attachments.length <= 1"
                @click="$emit('next')"
            >
                <bm-icon icon="chevron-right" size="lg" />
            </bm-button>
            <bm-button-close class="mx-2" size="lg" :title="$t('common.close_window')" @click="$emit('close')" />
        </bm-button-toolbar>
        <preview-message-header
            class="bg-surface"
            :expanded="expanded"
            @click.native="$emit('update:expanded', !expanded)"
        />
    </div>
</template>

<script>
import { BmButtonClose, BmIcon, BmButton, BmButtonToolbar } from "@bluemind/styleguide";
import { MimeType, getPartDownloadUrl } from "@bluemind/email";
import PreviewAttachmentHeader from "./PreviewAttachmentHeader";
import PreviewMessageHeader from "./PreviewMessageHeader";

export default {
    name: "PreviewHeader",
    components: { PreviewMessageHeader, PreviewAttachmentHeader, BmButtonClose, BmIcon, BmButton, BmButtonToolbar },
    props: {
        part: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        },
        expanded: { type: Boolean, required: true }
    },
    computed: {
        fileTypeIcon() {
            return MimeType.matchingIcon(this.part.mime);
        },
        downloadUrl() {
            return (
                this.part.url ||
                getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part)
            );
        }
    },
    methods: {
        open() {
            window.open(this.downloadUrl);
        },

        print() {
            const win = window.open(this.downloadUrl);
            win.addEventListener("afterprint", () => win.close());
            win.addEventListener("load", () => win.print());
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.preview-header {
    display: flex;
    background-color: $neutral-bg;
    & > .preview-attachment-header {
        order: 1;
    }
    .preview-attachment-header {
        flex: 1 1 auto;
        min-height: 0;
    }
}
</style>
