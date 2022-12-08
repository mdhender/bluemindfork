<template>
    <div class="preview-file-content">
        <bm-alert-area v-if="hasBlockedRemoteContent && !isLarge && isViewable(file)" :alerts="alerts" @remove="REMOVE">
            <template v-slot="slotProps">
                <component :is="slotProps.alert.renderer" :alert="slotProps.alert" />
            </template>
        </bm-alert-area>
        <file-viewer-facade
            v-if="isAllowedToPreview && !hasBlockedRemoteContent && src"
            :message="message"
            :file="{ ...file, url: src }"
        />
        <bm-extension v-else id="webapp.mail" type="decorator" path="file.preview.fallback" :file="file">
            <preview-file-content-fallback :file="file" />
        </bm-extension>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { BmAlertArea, BmIcon } from "@bluemind/ui-components";
import { partUtils, fileUtils } from "@bluemind/mail";
import { REMOVE } from "@bluemind/alert.store";
import { MimeType } from "@bluemind/email";

import { PreviewMixin } from "~/mixins";
import FileViewerFacade from "../../MailViewer/FilesViewer/FileViewerFacade";
import PreviewFileContentFallback from "./PreviewFileContentFallback/PreviewFileContentFallback";

const { isViewable } = partUtils;
const { FileStatus } = fileUtils;

export default {
    name: "PreviewFileContent",
    components: { BmAlertArea, BmExtension, BmIcon, FileViewerFacade, PreviewFileContentFallback },
    mixins: [PreviewMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: {
                alert: {
                    name: "mail.BLOCK_REMOTE_CONTENT",
                    uid: "BLOCK_REMOTE_CONTENT_PREVIEW",
                    payload: this.message
                },
                options: { area: "preview-right-panel", renderer: "BlockedRemoteContent" }
            },
            src: null
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "preview-right-panel") }),
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        }
    },
    watch: {
        "file.url": {
            async handler() {
                if (this.hasRemoteContent) {
                    this.$emit("remote-content");
                }
                this.src = await this.getSrc(this.file);
            },
            immediate: true
        },
        src(newSrc, oldSrc) {
            this.revokeObjectURL(oldSrc);
        },
        blockedRemoteContent: {
            async handler() {
                this.src = await this.getSrc(this.file);
            },
            immediate: true
        }
    },
    beforeDestroy() {
        this.revokeObjectURL(this.src);
    },
    methods: {
        ...mapActions("alert", { REMOVE }),
        async getBlobUrl(url) {
            try {
                const res = await fetch(url);
                if (!res.ok) {
                    return null;
                }
                const blob = await res.blob();
                return URL.createObjectURL(blob);
            } catch (err) {
                return null;
            }
        },
        async getSrc(file) {
            const url = file.url;
            if (!url || this.hasBlockedRemoteContent || !this.isAllowedToPreview) {
                return null;
            } else {
                return this.hasRemoteContent && !(MimeType.isImage(file) && file.status !== FileStatus.INVALID)
                    ? this.getBlobUrl(url)
                    : url;
            }
        },
        revokeObjectURL(url) {
            if (url && url.startsWith("blob")) {
                URL.revokeObjectURL(url);
            }
        },
        isViewable
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.preview-file-content {
    background-color: $highest;

    .file-viewer-facade {
        width: 100%;
        height: 100%;
        overflow: auto;

        .image-file-viewer {
            height: 100%;
        }
        .text-html-file-viewer,
        .text-plain-file-viewer {
            background-color: $surface-bg;
            width: 80%;
            min-height: 100%;
            margin: 0 auto;
        }

        .text-plain-file-viewer {
            padding: $sp-4;
        }
    }
    .bm-extension {
        height: 100%;
    }
    .bm-alert-area {
        position: absolute;
        width: 100%;
    }
}
</style>
