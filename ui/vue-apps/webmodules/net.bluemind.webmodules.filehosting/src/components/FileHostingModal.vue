<template>
    <bm-modal
        id="file-hosting-modal"
        :title="$tc('mail.filehosting.add.large', fhFiles.length)"
        title-class="ml-2"
        dialog-class="modal-dialog-centered"
        no-fade
    >
        <div class="mr-4 ml-2">
            <div v-if="hasSomeErrorStatus" class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="xl" class="mr-2 text-danger" />
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <bm-icon icon="exclamation" size="xl" class="text-danger" />
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <span class="mr-1 font-size-h1 text-neutral-fg-lo2">&#8226;</span>
                <bm-icon icon="chevron-right" class="text-neutral-fg-lo2" size="lg" />
                <bm-icon icon="cloud" class="ml-2 text-danger" size="xl" />
            </div>
            <div v-else class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="xl" class="mr-2 text-secondary" />
                <span :class="dotsClass">
                    <span class="font-size-h1">&#8226; &#8226; &#8226; &#8226; &#8226; &#8226;</span>
                </span>
                <bm-icon
                    icon="chevron-right"
                    size="lg"
                    :class="!isUploading ? 'text-secondary' : 'text-neutral-fg-lo2'"
                />
                <bm-icon icon="cloud" class="ml-2 text-secondary" size="xl" />
            </div>
            <div v-if="hasSomeErrorStatus" class="mb-4 text-danger">
                {{ $tc("mail.filehosting.share.failure", fhFiles.length) }}
            </div>
            <div v-else class="mb-4">
                {{ $tc("mail.filehosting.share.pending", fhFiles.length) }}
            </div>

            <div v-for="(file, idx) in fhFiles" :key="idx" class="position-relative mb-3">
                <detachment-item :file="file">
                    <template #item-actions>
                        <bm-label-icon
                            v-if="hasErrorStatus(file)"
                            class="text-danger ml-2 text-nowrap"
                            icon="exclamation-circle-fill"
                        >
                            {{ $t("mail.filehosting.import.failed") }}
                        </bm-label-icon>
                        <bm-button-close v-else-if="hasNotLoadedStatus(file)" class="ml-2" @click="cancel(file.key)" />
                        <span v-else class="text-neutral ml-2 text-nowrap">
                            {{ $t("mail.filehosting.import.successful") }}
                        </span>
                    </template>
                </detachment-item>
            </div>
        </div>
        <template #modal-footer>
            <bm-button variant="simple-inline" :disabled="!isUploading" @click="cancelAll">
                {{ $t("mail.filehosting.share.stop") }}
            </bm-button>
            <bm-button variant="outline-secondary" @click="hideModal">
                {{ isUploading ? $t("common.hide") : $t("common.done") }}
            </bm-button>
        </template>
    </bm-modal>
</template>
<script>
import { mapGetters } from "vuex";
import global from "@bluemind/global";
import { BmModal, BmButtonClose, BmButton, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";
import { fileUtils } from "@bluemind/mail";
import DetachmentItem from "./DetachmentItem";

const { FileStatus, isUploading } = fileUtils;

export default {
    name: "FileHostingModal",
    components: { BmModal, BmButtonClose, BmButton, BmIcon, DetachmentItem, BmLabelIcon },
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
        return { fhFileKeys: [] };
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_FILE"]),
        isUploading() {
            return this.fhFiles.filter(({ status }) => isUploading({ status })).length === this.fhFiles.length;
        },
        attachments() {
            return this.$store.state.mail.conversations.messages[this.message.key]?.attachments || [];
        },
        hasSomeErrorStatus() {
            return this.fhFiles.some(this.hasErrorStatus);
        },
        dotsClass() {
            return this.isUploading ? "dots" : "text-secondary";
        },
        fhFiles() {
            return this.fhFileKeys.map(key => {
                return { ...this.$store.state.mail.files[key], ...this.GET_FH_FILE({ key }) };
            });
        }
    },
    watch: {
        attachments(value) {
            value.forEach(attachment => {
                const fhFile = this.GET_FH_FILE({ key: attachment.fileKey });
                if (fhFile && !this.fhFiles.includes(fhFile)) {
                    const file = this.$store.state.mail.files[attachment.fileKey];
                    if (file.status === FileStatus.NOT_LOADED) {
                        this.fhFileKeys.push(file.key);
                    }
                }
            });
        },
        fhFiles(value) {
            if (value.length === 0) {
                this.hideModal();
            }
        }
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        },
        cancel(key) {
            global.cancellers[key].cancel();
            const index = this.fhFileKeys.findIndex(fileKey => {
                return fileKey === key;
            });
            index > 1 ? this.fhFileKeys.splice(index) : this.fhFileKeys.shift();
        },
        cancelAll() {
            this.fhFiles.slice().forEach(file => file.status === FileStatus.NOT_LOADED && this.cancel(file.key));
            this.hideModal();
        },
        hideModal() {
            this.$bvModal.hide("file-hosting-modal");
        },
        hasErrorStatus({ status }) {
            return status === FileStatus.ERROR;
        },
        hasNotLoadedStatus({ status }) {
            return status === FileStatus.NOT_LOADED;
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
