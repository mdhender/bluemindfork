<template>
    <div class="container">
        <bm-container class="mail-attachment-item bg-white border border-light text-condensed py-1 px-2 mt-2">
            <bm-row class="pt-1">
                <bm-col cols="12" class="px-1 text-center">
                    <img
                        v-if="hasPreview && attachment.contentUrl"
                        :src="attachment.contentUrl"
                        class="preview mb-1 mw-100"
                        :alt="$tc('common.attachmentPreview')"
                    />
                    <div v-else class="preview w-100 text-center mb-1 bg-light p-1">
                        <bm-icon :icon="fileTypeIcon" size="6x" class="m-auto bg-white preview-file-type" />
                    </div>
                </bm-col>
            </bm-row>
            <bm-row class="no-gutters align-items-center">
                <bm-col
                    class="col-auto align-self-start"
                    :class="{ muted: !isUploaded }"
                    :title="$t('mail.content.file-type', { fileType: $t('mail.content.' + fileTypeIcon) })"
                >
                    <bm-icon :icon="fileTypeIcon" size="2x" class="align-bottom pt-1" />
                </bm-col>
                <bm-col class="text-nowrap text-truncate flex-grow-1 px-1" :class="{ muted: !isUploaded }">
                    <span v-bm-tooltip :title="attachment.fileName" class="font-weight-bold">{{ fileName }} </span>
                    <br />
                    {{ fileSize }}
                </bm-col>
                <bm-col class="col-auto py-1">
                    <bm-button
                        v-if="isDownloadable"
                        v-bm-tooltip
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
                        @click="download"
                    >
                        <bm-icon icon="download" size="2x" class="p-1" />
                    </bm-button>
                    <bm-button-close
                        v-if="isRemovable"
                        v-bm-tooltip
                        variant="light"
                        class="p-0"
                        size="md"
                        :title="isCancellable ? $tc('common.cancel') : $tc('common.removeAttachment')"
                        :aria-label="isCancellable ? $tc('common.cancel') : $tc('common.removeAttachment')"
                        @click="isCancellable ? cancel() : removeAttachment()"
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
        <a
            ref="download-attachment-link"
            class="d-none"
            :download="attachment.fileName"
            :href="attachment.contentUrl"
        ></a>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";

import { MimeType } from "@bluemind/email";
import { computeUnit } from "@bluemind/file-utils";
import { inject } from "@bluemind/inject";
import global from "@bluemind/global";
import {
    BmButton,
    BmCol,
    BmContainer,
    BmIcon,
    BmRow,
    BmTooltip,
    BmProgress,
    BmButtonClose,
    BmNotice
} from "@bluemind/styleguide";

import { AttachmentStatus } from "../../model/attachment";
import actionTypes from "../../store/actionTypes";
import mutationTypes from "../../store/mutationTypes";
import { fetch } from "../../model/message";

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
    directives: { BmTooltip },
    props: {
        attachment: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["MY_DRAFTS"]),
        ...mapState("mail", ["messageCompose"]),
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
            return MimeType.previewAvailable(this.attachment.mime);
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
        }
    },
    watch: {
        attachment: {
            handler: function () {
                if (this.hasPreview && this.isUploaded) {
                    this.setContentUrl();
                }
            },
            immediate: true
        }
    },
    methods: {
        ...mapActions("mail", [actionTypes.REMOVE_ATTACHMENT]),
        ...mapMutations("mail", [mutationTypes.SET_ATTACHMENT_CONTENT_URL]),
        cancel() {
            global.cancellers[this.attachment.address + this.message.key].cancel();
        },
        async setContentUrl() {
            if (!this.attachment.contentUrl) {
                const contentUrl = URL.createObjectURL(
                    await fetch(
                        this.message.remoteRef.imapUid,
                        inject("MailboxItemsPersistence", this.message.folderRef.uid),
                        this.attachment,
                        true
                    )
                );
                this.SET_ATTACHMENT_CONTENT_URL({
                    messageKey: this.message.key,
                    address: this.attachment.address,
                    url: contentUrl
                });
            }
        },
        removeAttachment() {
            // FIXME userPrefTextOnly setting
            this.REMOVE_ATTACHMENT({
                messageKey: this.message.key,
                attachmentAddress: this.attachment.address,
                userPrefTextOnly: false,
                myDraftsFolderKey: this.MY_DRAFTS.key,
                messageCompose: this.messageCompose
            });
        },
        async download() {
            await this.setContentUrl();
            await this.$nextTick();
            this.$refs["download-attachment-link"].click();
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.mail-attachment-item {
    position: relative;

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
}

.preview-file-type {
    color: $light !important;
}
</style>
