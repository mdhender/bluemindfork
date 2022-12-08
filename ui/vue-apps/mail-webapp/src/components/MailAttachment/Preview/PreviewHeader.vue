<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="preview-header flex-row"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <preview-file-header :file="context.file" class="d-none d-lg-flex" />
        <bm-button-toolbar>
            <bm-icon-button
                :disabled="filesCount <= 1"
                tab-index="0"
                :title="$t('mail.preview.previous')"
                icon="chevron-left"
                @click="$emit('previous')"
            />
            <bm-icon-button
                :disabled="filesCount <= 1"
                :title="$t('mail.preview.next')"
                icon="chevron-right"
                @click="$emit('next')"
            />
            <bm-icon-button
                :disabled="!isPreviewable(context.file)"
                :title="
                    $t('mail.content.print', {
                        fileType: $t('mail.content.' + matchingIcon),
                        name: file.name
                    })
                "
                icon="printer"
                @click="print(file)"
            />
            <bm-icon-button
                :href="file.url"
                :download="file.name"
                :title="
                    $t('mail.content.download', {
                        fileType: $t('mail.content.' + matchingIcon),
                        name: file.name
                    })
                "
                icon="download"
            />
            <bm-icon-button
                :title="$t('mail.content.open-new-tab', { name: file.name })"
                :disabled="!isPreviewable(context.file)"
                icon="popup"
                @click="open(context.file)"
            />
            <bm-button-close size="lg" class="ml-5" :title="$t('common.close_window')" @click="$emit('close')" />
        </bm-button-toolbar>
        <preview-message-header
            class="bg-surface"
            :expanded="expanded"
            @click.native="$emit('update:expanded', !expanded)"
        />
    </bm-extension>
</template>

<script>
import { BmButtonClose, BmIconButton, BmButtonToolbar } from "@bluemind/ui-components";
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
        BmIconButton,
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
@import "@bluemind/ui-components/src/css/mixins/_responsiveness.scss";
@import "~@bluemind/ui-components/src/css/variables";

.preview-header {
    height: base-px-to-rem(40);
    flex: none;
    display: flex;
    align-items: center;
    padding-right: $sp-4;
    background-color: $neutral-bg;
    & > .preview-file-header {
        order: 1;
    }
    .preview-file-header {
        flex: 1 1 auto;
        min-height: 0;
    }
    & > .btn-toolbar {
        order: 0;
        justify-content: space-around;
        width: 100%;

        @include from-lg {
            order: 2;
            justify-content: flex-start;
            flex: none;
            width: auto;
        }
    }
}
</style>
