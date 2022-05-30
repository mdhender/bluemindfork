<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        type="renderless"
        path="message.attachment"
        :attachment="attachment"
        :message="message"
    >
        <bm-container
            class="mail-attachment-item text-condensed py-2 px-2 mt-2"
            :class="isRemovable ? '' : 'cursor-pointer'"
            @click="
                isViewable(context.attachment) ? openPreview(context.attachment, message) : download(context.attachment)
            "
        >
            <attachment-preview
                v-if="!compact"
                :attachment="context.attachment"
                :message="message"
                :class="{ muted: !isUploaded(context.attachment) }"
            />
            <attachment-infos
                :attachment="context.attachment"
                :message="message"
                :class="{ muted: !isUploaded(context.attachment) }"
            >
                <template #actions>
                    <bm-button
                        v-if="isViewable(context.attachment)"
                        variant="inline-neutral"
                        class="p-0"
                        size="md"
                        :title="$t('mail.preview.open')"
                        @click.stop="openPreview(context.attachment, message)"
                    >
                        <bm-icon icon="eye" size="2x" class="p-1" />
                    </bm-button>

                    <bm-button
                        v-if="!isRemovable"
                        variant="inline-neutral"
                        class="p-0 remove-attachment"
                        size="md"
                        :title="$t('common.downloadAttachment')"
                        :aria-label="
                            $t('mail.content.download', {
                                fileType: $t('mail.content.' + fileTypeIcon),
                                name: context.attachment.fileName
                            })
                        "
                        :href="previewUrl(context.attachment)"
                        :download="context.attachment.fileName"
                        @click.stop
                    >
                        <bm-icon icon="download" size="2x" class="p-1" />
                    </bm-button>
                    <bm-button-close
                        v-else-if="isCancellable(context.attachment)"
                        variant="light"
                        class="p-0 remove-attachment"
                        size="md"
                        :title="$tc('common.cancel')"
                        @click.stop="cancel(context.attachment, message)"
                    />
                    <bm-button-close
                        v-else
                        variant="inline-neutral"
                        class="p-0 remove-attachment"
                        size="md"
                        :title="$tc('common.removeAttachment')"
                        @click.stop="$execute('remove-attachment', { attachment: context.attachment, message })"
                    />
                </template>
            </attachment-infos>
            <bm-progress
                v-if="!isUploaded(context.attachment)"
                :value="context.attachment.progress.loaded"
                :max="context.attachment.progress.total"
                :animated="context.attachment.progress.animated"
                :variant="errorMessage ? 'danger' : 'secondary'"
            />
        </bm-container>
        <div v-if="errorMessage(context.attachment)" class="row px-1">
            <bm-notice class="w-100" :text="errorMessage(context.attachment)" />
        </div>
    </bm-extension>
</template>

<script>
import { mapMutations } from "vuex";
import global from "@bluemind/global";
import { getPartDownloadUrl, MimeType } from "@bluemind/email";
import { BmContainer, BmProgress, BmNotice, BmButtonClose, BmIcon, BmButton } from "@bluemind/styleguide";
import { BmExtension } from "@bluemind/extensions.vue";

import { AttachmentStatus } from "~/model/attachment";
import { ComposerActionsMixin } from "~/mixins";
import { isViewable } from "~/model/part";

import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_PART_ADDRESS } from "~/mutations";
import AttachmentPreview from "./MailAttachmentItem/AttachmentPreview";
import AttachmentInfos from "./MailAttachmentItem/AttachmentInfos";

export default {
    name: "MailAttachmentItem",
    components: {
        BmContainer,
        BmProgress,
        BmNotice,
        BmButtonClose,
        BmIcon,
        BmButton,
        BmExtension,
        AttachmentPreview,
        AttachmentInfos
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
            default: false
        }
    },

    computed: {
        isRemovable() {
            return this.message.composing;
        }
    },
    methods: {
        ...mapMutations("mail", {
            SET_PREVIEW_MESSAGE_KEY,
            SET_PREVIEW_PART_ADDRESS
        }),
        isCancellable(attachment) {
            return !this.isUploaded(attachment) && attachment.status !== AttachmentStatus.ERROR;
        },
        isUploaded(attachment) {
            return attachment.progress.loaded === attachment.progress.total;
        },
        download(attachment) {
            location.assign(this.previewUrl(attachment));
        },
        openPreview(attachment, message) {
            this.SET_PREVIEW_MESSAGE_KEY(message.key);
            this.SET_PREVIEW_PART_ADDRESS(attachment.address);
            this.$bvModal.show("mail-attachment-preview");
        },
        cancel(attachment, message) {
            global.cancellers[attachment.address + message.key].cancel();
        },
        errorMessage(attachment) {
            return attachment.status === AttachmentStatus.ERROR
                ? this.$t("alert.mail.message.draft.attach.error")
                : undefined;
        },
        previewUrl(attachment) {
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, attachment);
        },
        fileTypeIcon({ mime }) {
            return MimeType.matchingIcon(mime);
        },
        isViewable
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
        padding: 0;
        margin-left: $sp-2;
    }
}
</style>
