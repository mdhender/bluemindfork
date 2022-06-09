<template>
    <div>
        <bm-container
            class="mail-attachment-item text-condensed py-2 px-2 mt-2"
            :class="isRemovable ? '' : 'cursor-pointer'"
            @click="isViewable(attachment) ? openPreview() : download()"
        >
            <div
                v-if="!compact"
                ref="preview-div"
                class="text-center preview overflow-hidden d-flex justify-content-center align-items-center"
                :class="fileTypeIcon"
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
                <div v-else class="preview w-100 text-center mb-1 p-1">
                    <bm-icon :icon="fileTypeIcon" size="6x" class="m-auto preview-file-type" />
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
                <bm-col
                    class="text-nowrap text-truncate flex-grow-1 px-2 attachment-text"
                    :class="{ muted: !isUploaded }"
                >
                    <span :title="attachment.fileName" class="font-weight-bold">{{ fileName }} </span>
                    <br />
                    {{ fileSize }}
                </bm-col>
                <bm-col class="col-auto py-1">
                    <bm-button
                        v-if="isViewable(attachment)"
                        variant="inline-neutral"
                        class="p-0"
                        size="md"
                        :title="$t('mail.preview.open')"
                        @click.stop="openPreview"
                    >
                        <bm-icon icon="eye" size="2x" class="p-1" />
                    </bm-button>

                    <bm-button
                        v-if="!isRemovable"
                        variant="inline-neutral"
                        class="p-0"
                        size="md"
                        :title="$t('common.downloadAttachment')"
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
                        variant="inline-neutral"
                        class="p-0 remove-attachment"
                        size="md"
                        :title="isCancellable ? $tc('common.cancel') : $tc('common.removeAttachment')"
                        :aria-label="isCancellable ? $tc('common.cancel') : $tc('common.removeAttachment')"
                        @click.stop="isCancellable ? cancel() : removeAttachment(attachment.address)"
                    />
                </bm-col>
            </bm-row>
            <bm-progress
                v-if="!isUploaded"
                :value="attachment.progress.loaded"
                :max="attachment.progress.total"
                :animated="attachment.progress.animated"
                :variant="errorMessage ? 'danger' : 'secondary'"
            />
        </bm-container>
        <div v-if="errorMessage" class="row px-1"><bm-notice class="w-100" :text="errorMessage" /></div>
    </div>
</template>

<script>
import { MimeType, getPartDownloadUrl } from "@bluemind/email";
import { inject } from "@bluemind/inject";
import { computeUnit } from "@bluemind/file-utils";
import global from "@bluemind/global";
import { BmButton, BmCol, BmContainer, BmIcon, BmRow, BmProgress, BmButtonClose, BmNotice } from "@bluemind/styleguide";
import { AttachmentStatus } from "~/model/attachment";
import { ComposerActionsMixin } from "~/mixins";
import { mapMutations } from "vuex";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_PART_ADDRESS } from "~/mutations";
import { isViewable } from "~/model/part";

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
        message: {
            type: Object,
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
        isRemovable() {
            return this.message.composing;
        },
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.mime);
        },
        fileName() {
            return this.attachment.fileName;
        },
        fileSize() {
            return computeUnit(this.attachment.size, inject("i18n"));
        },
        hasPreview() {
            return (
                MimeType.previewAvailable(this.attachment.mime) && this.attachment.status === AttachmentStatus.UPLOADED
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
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.attachment);
        }
    },
    methods: {
        ...mapMutations("mail", {
            SET_PREVIEW_MESSAGE_KEY,
            SET_PREVIEW_PART_ADDRESS
        }),
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
        },
        isViewable,
        openPreview() {
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_PART_ADDRESS(this.attachment.address);
            this.$bvModal.show("mail-attachment-preview");
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";
@import "@bluemind/styleguide/css/_fileTypeIconsColors.scss";

.mail-attachment-item {
    position: relative;

    background-color: $surface;
    border: 1px solid $neutral-fg-lo3;

    &:hover {
        background-color: $neutral-bg;
        border-color: $neutral-fg-lo3;
    }

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
        background-color: $neutral-bg;
        height: 7em;
    }
    .preview-file-type {
        color: $neutral-bg;
        background-color: $lowest;
    }
    &:hover {
        @each $file-type, $color in $file-type-icons-colors {
            .#{$file-type} {
                .preview {
                    background-color: $color;
                }
                .preview-file-type {
                    color: $color;
                }
            }
        }
    }

    .muted {
        opacity: 0.5;
    }

    .attachment-text {
        line-height: 1.085em;
    }
    .remove-attachment {
        margin-left: $sp-2;
    }
}
</style>
