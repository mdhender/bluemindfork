<template>
    <div class="preview-file-content">
        <bm-alert-area v-if="alerts.length > 0" :alerts="alerts" :file="file" class="shadow" @remove="REMOVE">
            <template #default="slotProps">
                <component :is="slotProps.alert.renderer" :alert="slotProps.alert" />
            </template>
        </bm-alert-area>
        <bm-extension v-if="file" id="webapp.mail" type="chain-of-responsibility" path="file.preview" :file="file_">
            <bm-spinner v-if="loading" />
            <file-viewer-facade v-else-if="isAllowedToPreview" class="scroller-y" :message="message" :file="file_" />
            <no-preview v-else-if="!src" icon="spam" :text="$t('mail.preview.nopreview')" />
            <no-preview v-else :icon="matchingIcon" class="file-type" :text="$t('mail.preview.nopreview.type')" />
        </bm-extension>
    </div>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { mapActions, mapMutations, mapState } from "vuex";
import { WARNING, REMOVE } from "@bluemind/alert.store";
import apiAddressbooks from "~/store/api/apiAddressbooks";
import { SET_BLOCK_REMOTE_IMAGES } from "~/mutations";
import { BmExtension } from "@bluemind/extensions.vue";
import { BmAlertArea, BmIcon, BmSpinner } from "@bluemind/ui-components";
import { fileUtils } from "@bluemind/mail";
import { PreviewMixin } from "~/mixins";
import FileViewerFacade from "../../MailViewer/FilesViewer/FileViewerFacade";
import NoPreview from "./Fallback/NoPreview";

const { FileStatus } = fileUtils;

export default {
    name: "PreviewFileContent",
    components: {
        BmAlertArea,
        BmExtension,
        BmIcon,
        BmSpinner,
        FileViewerFacade,
        NoPreview
    },
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
            src: null,
            loading: false
        };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => area === "preview-right-panel") }),
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        },
        file_() {
            return { ...this.file, url: this.src };
        }
    },
    watch: {
        "file.url": {
            async handler() {
                this.loading = true;
                this.src = null;
                this.src = await this.getFileUrl();
                this.loading = false;
            },
            immediate: true
        },
        src(newSrc, oldSrc) {
            this.revokeObjectURL(oldSrc);
        },
        blockedRemoteContent: {
            async handler() {
                this.loading = true;
                this.src = null;
                this.src = await this.getFileUrl();
                this.loading = false;
            },
            immediate: true
        }
    },
    beforeDestroy() {
        this.revokeObjectURL(this.src);
    },
    methods: {
        ...mapActions("alert", { REMOVE, WARNING }),
        ...mapMutations("mail", {
            SET_BLOCK_REMOTE_IMAGES
        }),
        getFileUrl() {
            if (this.hasBlockedRemoteContent) {
                this.setBlockRemote();
            }
            if (
                this.file.url &&
                !this.hasBlockedRemoteContent &&
                this.hasRemoteContent &&
                !(MimeType.isImage(this.file) && this.file.status !== FileStatus.INVALID)
            ) {
                return this.getBlobUrl(this.file.url);
            } else {
                return this.file.url;
            }
        },
        async getBlobUrl(url) {
            try {
                const res = await fetch(encodeURI(url));
                if (!res.ok) {
                    return null;
                }
                const blob = await res.blob();
                return URL.createObjectURL(blob);
            } catch (err) {
                this.loading = false;
                return null;
            }
        },
        revokeObjectURL(url) {
            if (url && url.startsWith("blob")) {
                URL.revokeObjectURL(url);
            }
        },

        async setBlockRemote() {
            if (this.$store.state.mail.consultPanel.remoteImages.mustBeBlocked) {
                const { total } = await apiAddressbooks.search(this.message.from.address);
                if (total === 0) {
                    this.WARNING(this.alert);
                } else {
                    this.SET_BLOCK_REMOTE_IMAGES(false);
                }
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.preview-file-content {
    background-color: $darkest;
    .bm-spinner {
        text-align: center;
        height: 100%;
        & > .spinner {
            height: 100%;
        }
    }

    .file-viewer-facade {
        width: 100%;
        height: 100%;

        .image-file-viewer {
            height: 100%;
        }
        .text-html-file-viewer,
        .text-plain-file-viewer {
            background-color: $surface;
            width: 80%;
            min-height: 100%;
            margin: 0 auto;
        }

        .text-plain-file-viewer {
            padding: $sp-4;
        }
    }
    .bm-alert-area {
        width: 100%;
        .alert {
            margin-bottom: 0;
        }
    }
    .no-preview {
        .blocked-preview {
            color: $lowest;
            background-color: $neutral-bg;
        }
        &.file-type .bm-icon {
            background-color: $neutral-bg;
            color: $highest;
        }
    }
}
</style>
