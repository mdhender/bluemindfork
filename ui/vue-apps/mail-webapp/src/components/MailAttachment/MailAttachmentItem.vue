<template>
    <div>
        <bm-container
            class="mail-attachment-item text-condensed py-2 px-2 mt-2"
            :class="isRemovable ? '' : 'cursor-pointer'"
            @click="isViewable(attachment) ? openPreview() : download()"
        >
            <component
                :is="component"
                :attachment="attachment"
                :message="message"
                :compact="compact"
                :class="{ muted: !isUploaded }"
            >
                <template #actions>
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
                        class="p-0 remove-attachment"
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
            </component>
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
import { mapMutations } from "vuex";
import { getPartDownloadUrl, MimeType } from "@bluemind/email";
import { BmContainer, BmProgress, BmNotice, BmButtonClose, BmIcon, BmButton } from "@bluemind/styleguide";
import global from "@bluemind/global";

import { AttachmentStatus } from "~/model/attachment";
import { ComposerActionsMixin } from "~/mixins";
import { isViewable } from "~/model/part";

import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_PART_ADDRESS } from "~/mutations";
import DefaultAttachment from "./MailAttachmentItem/DefaultAttachment";
import FileHostingAttachment from "./MailAttachmentItem/FileHostingAttachment";

const strategies = new Map([
    ["filehosting", FileHostingAttachment],
    ["default", DefaultAttachment]
]);

export default {
    name: "MailAttachmentItem",
    components: {
        BmContainer,
        BmProgress,
        BmNotice,
        BmButtonClose,
        BmIcon,
        BmButton
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
        },
        errorMessage() {
            return this.attachment.status === AttachmentStatus.ERROR
                ? this.$t("alert.mail.message.draft.attach.error")
                : undefined;
        },
        isUploaded() {
            return this.attachment.progress.loaded === this.attachment.progress.total;
        },
        previewUrl() {
            return getPartDownloadUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.attachment);
        },
        isCancellable() {
            return !this.isUploaded && this.attachment.status !== AttachmentStatus.ERROR;
        },
        fileTypeIcon() {
            return MimeType.matchingIcon(this.attachment.extra.mime || this.attachment.mime);
        },
        component() {
            return strategies.has(this.attachment.type)
                ? strategies.get(this.attachment.type)
                : strategies.get("default");
        }
    },
    methods: {
        ...mapMutations("mail", {
            SET_PREVIEW_MESSAGE_KEY,
            SET_PREVIEW_PART_ADDRESS
        }),

        download() {
            location.assign(this.previewUrl);
        },
        openPreview() {
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_PART_ADDRESS(this.attachment.address);
            this.$bvModal.show("mail-attachment-preview");
        },
        cancel() {
            global.cancellers[this.attachment.address + this.message.key].cancel();
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
