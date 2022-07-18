<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="preview-header flex-column flex-lg-row"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <preview-file-header :file="context.file" class="d-none d-lg-flex" />
        <bm-button-toolbar class="order-0 order-lg-2 justify-content-around justify-content-lg-start">
            <bm-button
                variant="simple-neutral"
                :disabled="!isPreviewable(context.file)"
                :title="
                    $t('mail.content.print', {
                        fileType: $t('mail.content.' + matchingIcon),
                        name: file.name
                    })
                "
                @click="print(file)"
            >
                <bm-icon icon="printer" size="lg" />
            </bm-button>
            <bm-button
                :href="file.url"
                variant="simple-neutral"
                :download="file.name"
                class="d-flex align-items-center"
                :title="
                    $t('mail.content.download', {
                        fileType: $t('mail.content.' + matchingIcon),
                        name: file.name
                    })
                "
            >
                <bm-icon icon="download" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :title="$t('mail.content.open-new-tab', { name: file.name })"
                :disabled="!isPreviewable(context.file)"
                @click="open(context.file)"
            >
                <bm-icon icon="popup" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :disabled="filesCount <= 1"
                tab-index="0"
                :title="$t('mail.preview.previous')"
                @click="$emit('previous')"
            >
                <bm-icon icon="chevron-left" size="lg" />
            </bm-button>
            <bm-button
                variant="simple-neutral"
                :title="$t('mail.preview.next')"
                :disabled="filesCount <= 1"
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
    </bm-extension>
</template>

<script>
import { BmButtonClose, BmIcon, BmButton, BmButtonToolbar } from "@bluemind/styleguide";
import { MimeType } from "@bluemind/email";
import { BmExtension } from "@bluemind/extensions.vue";
import PreviewFileHeader from "./PreviewFileHeader";
import PreviewMessageHeader from "./PreviewMessageHeader";
import { fileUtils } from "@bluemind/mail";
const { isAllowedToPreview, hasRemoteContent } = fileUtils;

export default {
    name: "PreviewHeader",
    components: {
        BmExtension,
        PreviewMessageHeader,
        PreviewFileHeader,
        BmButtonClose,
        BmIcon,
        BmButton,
        BmButtonToolbar
    },
    props: {
        file: {
            type: Object,
            required: true
        },
        filesCount: {
            type: Number,
            required: true
        },
        expanded: { type: Boolean, required: true }
    },
    computed: {
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        },
        blockedRemoteContent() {
            return this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked;
        }
    },
    methods: {
        open(file) {
            window.open(file.url);
        },
        print(file) {
            const win = window.open(file.url);
            win.addEventListener("afterprint", () => win.close());
            win.addEventListener("load", () => win.print());
        },
        isPreviewable(file) {
            return isAllowedToPreview(file) && !(hasRemoteContent(file) && this.blockedRemoteContent);
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.preview-header {
    display: flex;
    background-color: $neutral-bg;
    & > .preview-file-header {
        order: 1;
    }
    .preview-file-header {
        flex: 1 1 auto;
        min-height: 0;
    }
}
</style>
