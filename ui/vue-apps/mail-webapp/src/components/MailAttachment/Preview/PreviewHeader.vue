<template>
    <div class="preview-header flex-column flex-lg-row">
        <preview-attachment-header :part="part" class="d-none d-lg-flex" />
        <bm-button-toolbar class="order-0 order-lg-2 justify-content-around justify-content-lg-start">
            <bm-button
                variant="simple-neutral"
                :title="
                    $t('mail.content.print', {
                        fileType: $t('mail.content.' + fileTypeIcon),
                        name: part.fileName
                    })
                "
                @click="$emit('print')"
            >
                <bm-icon icon="printer" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :title="
                    $t('mail.content.download', {
                        fileType: $t('mail.content.' + fileTypeIcon),
                        name: part.fileName
                    })
                "
                @click="$emit('download')"
            >
                <bm-icon icon="download" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :title="$t('mail.content.open-new-tab', { name: part.fileName })"
                @click="$emit('open')"
            >
                <bm-icon icon="popup" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                tab-index="0"
                :title="$t('mail.preview.previous')"
                @click="$emit('previous')"
            >
                <bm-icon icon="chevron-left" size="lg" />
            </bm-button>
            <bm-button variant="simple-neutral" :title="$t('mail.preview.next')" @click="$emit('next')">
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
import { MimeType } from "@bluemind/email";
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
