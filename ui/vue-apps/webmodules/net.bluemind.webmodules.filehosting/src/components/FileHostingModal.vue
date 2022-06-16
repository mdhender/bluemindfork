<template>
    <bm-modal
        id="file-hosting-modal"
        :title="$tc('mail.filehosting.add.large', fhAttachments.length)"
        title-class="ml-2"
        dialog-class="modal-dialog-centered"
        no-fade
    >
        <div class="mr-4 ml-2">
            <div v-if="hasSomeErrorStatus" class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="2x" class="mr-2 text-danger" />
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <bm-icon icon="exclamation" size="2x" class="text-danger" />
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <bm-icon icon="chevron-right" class="text-neutral-fg-lo2" size="lg" />
                <bm-icon icon="cloud" class="ml-2 text-danger" size="2x" />
            </div>
            <div v-else class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="2x" class="mr-2 text-secondary" />
                <span :class="dotsClass">
                    <span class="font-size-h1">&#8226; &#8226; &#8226; &#8226; &#8226; &#8226;</span>
                </span>
                <bm-icon
                    icon="chevron-right"
                    size="lg"
                    :class="isFinished ? 'text-secondary' : 'text-neutral-fg-lo2'"
                />
                <bm-icon icon="cloud" class="ml-2 text-secondary" size="2x" />
            </div>
            <div v-if="hasSomeErrorStatus" class="mb-4 text-danger">
                {{ $tc("mail.filehosting.share.failure", fhAttachments.length) }}
            </div>
            <div v-else class="mb-4">
                {{ $tc("mail.filehosting.share.pending", fhAttachments.length) }}
            </div>

            <div v-for="(attachment, idx) in fhAttachments" :key="idx" class="position-relative mb-3">
                <fh-attachment-item :attachment="attachment">
                    <template #item-actions>
                        <bm-label-icon
                            v-if="hasErrorStatus(attachment)"
                            class="text-danger ml-2 text-nowrap"
                            icon="exclamation-circle-fill"
                        >
                            {{ $t("mail.filehosting.import.failed") }}
                        </bm-label-icon>
                        <bm-button-close
                            v-else-if="hasNotLoadedStatus(attachment)"
                            class="ml-2"
                            @click="cancel(attachment.address)"
                        />
                        <span v-else class="text-neutral ml-2 text-nowrap">
                            {{ $t("mail.filehosting.import.successful") }}
                        </span>
                    </template>
                </fh-attachment-item>
            </div>
        </div>
        <template #modal-footer>
            <bm-button variant="simple-inline" :disabled="isFinished" @click="cancelAll">
                {{ $t("mail.filehosting.share.stop") }}
            </bm-button>
            <bm-button variant="outline-secondary" @click="hideModal">
                {{ $t("common.hide") }}
            </bm-button>
        </template>
    </bm-modal>
</template>
<script>
import { mapGetters } from "vuex";
import global from "@bluemind/global";
import { BmModal, BmButtonClose, BmButton, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";
import { attachment } from "@bluemind/mail";
import FhAttachmentItem from "./AttachmentItem";

const { AttachmentStatus } = attachment;

export default {
    name: "FileHostingModal",
    components: { BmModal, BmButtonClose, BmButton, BmIcon, FhAttachmentItem, BmLabelIcon },
    props: {
        sizeLimit: {
            type: Number,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { fhAttachments: [] };
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_ATTACHMENT"]),
        isFinished() {
            return (
                this.fhAttachments.filter(({ status }) =>
                    [AttachmentStatus.UPLOADED, AttachmentStatus.ERROR].includes(status)
                ).length === this.fhAttachments.length
            );
        },
        attachments() {
            return this.$store.state.mail.conversations.messages[this.message.key]?.attachments || [];
        },
        hasSomeErrorStatus() {
            return this.fhAttachments.some(this.hasErrorStatus);
        },
        dotsClass() {
            return this.isFinished ? "text-secondary" : "dots";
        }
    },
    watch: {
        attachments(value) {
            value.forEach(attachment => {
                if (
                    this.GET_FH_ATTACHMENT(this.message, attachment) &&
                    !this.fhAttachments.includes(attachment) &&
                    attachment.status === AttachmentStatus.NOT_LOADED
                ) {
                    this.fhAttachments.push(attachment);
                }
            });
        },
        fhAttachments(value) {
            if (value.length === 0) {
                this.hideModal();
            }
        }
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        },
        cancel(address) {
            global.cancellers[address + this.$store.state.mail.activeMessage.key].cancel();
            const index = this.fhAttachments.findIndex(attachment => {
                return attachment.address === address;
            });
            index > 1 ? this.fhAttachments.splice(index) : this.fhAttachments.shift();
        },
        cancelAll() {
            this.fhAttachments
                .slice()
                .forEach(
                    attachment => attachment.status === AttachmentStatus.NOT_LOADED && this.cancel(attachment.address)
                );
            this.hideModal();
        },
        hideModal() {
            this.$bvModal.hide("file-hosting-modal");
        },
        hasErrorStatus({ status }) {
            return status === AttachmentStatus.ERROR;
        },
        hasNotLoadedStatus({ status }) {
            return status === AttachmentStatus.NOT_LOADED;
        }
    }
};
</script>
<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

#file-hosting-modal {
    .text-neutral-fg-lo2 {
        color: $neutral-fg-lo2;
    }
    .font-size-h1 {
        font-size: $h1-font-size;
    }
    .dots {
        background-image: linear-gradient(90deg, $fill-secondary-bg 0%, $fill-secondary-bg 100%);
        background-clip: text;
        -webkit-background-clip: text;
        color: transparent;
        animation-name: dots;
        animation-duration: 1s;
        animation-iteration-count: infinite;
        animation-timing-function: linear;
        background-size: 0%;
        background-color: $neutral-fg-lo2;
        background-repeat: no-repeat;
    }
    @keyframes dots {
        0% {
            background-size: 0px;
        }

        to {
            background-size: 110%;
        }
    }
}
</style>
