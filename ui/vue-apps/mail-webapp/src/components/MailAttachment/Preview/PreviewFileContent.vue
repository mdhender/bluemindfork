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
        <div v-else-if="!isViewable(file)" class="no-preview center">
            <div class="file-type"><bm-icon :icon="matchingIcon" size="3xl" /></div>
            <span class="text">{{ $t("mail.preview.nopreview.type") }}</span>
        </div>
        <div v-else-if="isLarge" class="no-preview center">
            <div class="mb-3"><bm-icon icon="weight" size="3xl" /></div>
            <span class="text">{{ $t("mail.preview.nopreview.large") }}</span>
        </div>
        <div v-else-if="hasBlockedRemoteContent" class="blocked-preview center">
            <bm-icon icon="exclamation-circle" size="3xl" />
        </div>

        <div v-else class="no-preview center">
            <div class="mb-3"><bm-icon icon="spam" size="3xl" /></div>
            <span class="text">{{ $t("mail.preview.nopreview") }}</span>
        </div>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { BmExtension } from "@bluemind/extensions.vue";
import { BmAlertArea, BmIcon } from "@bluemind/styleguide";
import { partUtils } from "@bluemind/mail";
import { REMOVE } from "@bluemind/alert.store";
import { MimeType } from "@bluemind/email";

import { PreviewMixin } from "~/mixins";
import FileViewerFacade from "../../MailViewer/FilesViewer/FileViewerFacade";

const { isViewable } = partUtils;

export default {
    name: "PreviewFileContent",
    components: { BmAlertArea, BmExtension, BmIcon, FileViewerFacade },
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
        blockedRemoteContent: {
            async handler() {
                this.src = await this.getSrc(this.file);
            },
            immediate: true
        }
    },

    methods: {
        ...mapActions("alert", { REMOVE }),
        async getBlobUrl(url) {
            try {
                const res = await fetch(url);
                return URL.createObjectURL(await res.blob());
            } catch (err) {
                return null;
            }
        },
        async getSrc(file) {
            const url = file.url;
            if (!url || this.hasBlockedRemoteContent || !this.isAllowedToPreview) {
                return null;
            } else {
                return this.hasRemoteContent && !MimeType.isImage(file) ? this.getBlobUrl(url) : url;
            }
        },
        isViewable
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

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
    .bm-alert-area {
        position: absolute;
        width: 100%;
    }

    .blocked-preview {
        color: $lowest;
        background-color: $neutral-bg;
        height: 100%;
    }
    .center {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
    }
    .no-preview {
        color: $fill-neutral-fg-lo1;
        height: 100%;
        .text {
            color: $fill-neutral-fg;
        }
        .file-type > svg {
            color: $highest;
            background-color: $neutral-bg;
        }
    }
}
</style>
