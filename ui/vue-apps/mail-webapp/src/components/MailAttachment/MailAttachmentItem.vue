<template>
    <div>
        <bm-container
            class="mail-attachment-item bg-white border border-light text-condensed py-2 px-2 mt-2"
            :class="isDownloadable ? 'cursor-pointer' : ''"
            @click="isDownloadable ? download() : null"
        >
            <div
                v-if="!compact"
                ref="preview-div"
                class="text-center preview overflow-hidden d-flex justify-content-center align-items-center"
            >
                <img
                    v-if="hasPreview"
                    ref="preview-image"
                    :src="previewUrl"
                    class="flex-grow-1"
                    :class="fitPreviewImage ? 'w-100' : ''"
                    :alt="$tc('common.attachmentPreview')"
                    @load="previewImageLoaded"
                />
                <div v-else class="preview w-100 text-center mb-1 bg-light p-1">
                    <bm-icon :icon="fileTypeIcon" size="6x" class="m-auto bg-white preview-file-type" />
                </div>
            </div>
            <bm-row class="no-gutters align-items-center">
                <bm-col
                    class="col-auto"
                    :class="{ muted: !isUploaded }"
                    :title="$t('mail.content.file-type', { fileType: $t('mail.content.' + fileTypeIcon) })"
                >
                    <bm-icon :icon="fileTypeIcon" size="2x" class="align-bottom" />
                </bm-col>
                <bm-col class="text-nowrap text-truncate flex-grow-1 px-1" :class="{ muted: !isUploaded }">
                    <span :title="attachment.fileName" class="font-weight-bold">{{ fileName }} </span>
                    <br />
                    {{ fileSize }}
                </bm-col>
                <bm-col class="col-auto py-1">
                    <bm-button
                        v-if="isDownloadable"
                        variant="light"
                        class="p-0"
                        size="md"
                        :title="$tc('common.downloadAttachment')"
                        :aria-label="
                            $t('mail.content.download', {
                                fileType: $t('mail.content.' + fileTypeIcon),
                                name: attachment.fileName
                            })
                        "
                        :href="previewUrl"
                        :download="attachment.fileName"
                        @click.stop
                    >
                        <bm-icon icon="download" size="2x" class="p-1" />
                    </bm-button>
                    <bm-button-close
                        v-if="isRemovable"
                        variant="light"
                        class="p-0"
                        size="md"
                        :title="isCancellable ? $tc('common.cancel') : $tc('common.removeAttachment')"
                        :aria-label="isCancellable ? $tc('common.cancel') : $tc('common.removeAttachment')"
                        @click="isCancellable ? cancel() : removeAttachment(attachment.address)"
                    />
                </bm-col>
            </bm-row>
            <bm-progress
                v-if="!isUploaded"
                :value="attachment.progress.loaded"
                :max="attachment.progress.total"
                :animated="attachment.progress.animated"
                :variant="errorMessage ? 'danger' : 'primary'"
            />
        </bm-container>
        <div v-if="errorMessage" class="row px-1"><bm-notice class="w-100" :text="errorMessage" /></div>
    </div>
</template>

<script>
import { mapState } from "vuex";

import { MimeType, computePreviewOrDownloadUrl } from "@bluemind/email";
import { computeUnit } from "@bluemind/file-utils";
import global from "@bluemind/global";
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmProgress, BmButtonClose, BmNotice } from "@bluemind/styleguide";

import { AttachmentStatus } from "~model/attachment";
import { ComposerActionsMixin } from "~mixins";

export default {
    name: "MailAttachmentItem",
    components: {
        BmButton,
        BmCol,
        BmContainer,
        BmIcon,
        BmRow,
        BmProgress,
        BmButtonClose,
        BmNotice
    },
    mixins: [ComposerActionsMixin],
    props: {
        attachment: {
            type: Object,
            required: true
        },
        messageKey: {
            type: String,
            required: true
        },
        compact: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            fitPreviewImage: false
        };
    },
    computed: {
        ...mapState("mail", ["messages"]),
        message() {
            return this.messages[this.messageKey];
        },
        isRemovable() {
            return this.message.composing;
        },
        isDownloadable() {
            return !this.message.composing;
        },
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.mime);
        },
        fileName() {
            return this.attachment.fileName
                ? this.attachment.fileName
                : this.$t("mail.attachment.untitled", { mimeType: this.attachment.mime });
        },
        fileSize() {
            return computeUnit(this.attachment.size);
        },
        hasPreview() {
            return (
                MimeType.previewAvailable(this.attachment.mime) && this.attachment.status === AttachmentStatus.LOADED
            );
        },
        errorMessage() {
            return this.attachment.status === AttachmentStatus.ERROR
                ? this.$t("alert.mail.message.draft.attach.error")
                : undefined;
        },
        isCancellable() {
            return !this.isUploaded && this.attachment.status !== AttachmentStatus.ERROR;
        },
        isUploaded() {
            return this.attachment.progress.loaded === this.attachment.progress.total;
        },
        previewUrl() {
            return computePreviewOrDownloadUrl(
                this.message.folderRef.uid,
                this.message.remoteRef.imapUid,
                this.attachment
            );
        }
    },
    methods: {
        cancel() {
            global.cancellers[this.attachment.address + this.message.key].cancel();
        },
        download() {
            location.assign(this.previewUrl);
        },
        previewImageLoaded() {
            const width = this.$refs["preview-image"].width;
            const height = this.$refs["preview-image"].height;
            const divWidth = this.$refs["preview-div"].clientWidth;
            const divHeight = this.$refs["preview-div"].clientHeight;
            const scaleRatio = divWidth / width;
            this.fitPreviewImage = width > height && height * scaleRatio > divHeight;
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-attachment-item {
    position: relative;

    &.cursor-pointer {
        cursor: pointer;
    }

    .progress {
        position: absolute;
        top: 0;
        margin-left: -0.5rem;
        margin-right: -0.5rem;
        height: 0.125rem;
        width: 100%;
        background-color: transparent;
    }

    .preview {
        height: 7em;
    }

    .muted {
        opacity: 0.5;
    }

    .preview-file-type {
        color: $light !important;
    }
}
</style>
