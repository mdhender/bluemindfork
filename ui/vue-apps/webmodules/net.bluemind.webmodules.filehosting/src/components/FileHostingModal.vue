<template>
    <bm-modal
        id="file-hosting-modal"
        :title="$tc('filehosting.add.large', fhFiles.length)"
        dialog-class="modal-dialog-centered"
        variant="advanced"
        size="sm"
        height="lg"
        scrollable
    >
        <div class="mr-4 ml-2">
            <div v-if="hasSomeErrorStatus" class="d-flex align-items-center mb-5">
                <bm-icon icon="file" size="xl" class="mr-2 text-danger" />
                <span class="mr-3 dot-font-size text-neutral-fg-lo2">&#8226;</span>
                <span class="mr-3 dot-font-size text-neutral-fg-lo2">&#8226;</span>
                <bm-icon icon="exclamation" size="xl" class="text-danger" />
                <span class="mr-3 dot-font-size text-neutral-fg-lo2">&#8226;</span>
                <span class="mr-3 dot-font-size text-neutral-fg-lo2">&#8226;</span>
                <bm-icon icon="chevron-right" class="text-neutral-fg-lo2" size="lg" />
                <bm-icon icon="cloud" class="ml-2 text-danger" size="xl" />
            </div>
            <div v-else class="d-flex align-items-center mb-5">
                <bm-icon icon="file" size="xl" class="mr-2 text-secondary" />
                <span :class="dotsClass">
                    <span class="dot-font-size">&#8226; &#8226; &#8226; &#8226; &#8226; &#8226;</span>
                </span>
                <bm-icon
                    icon="chevron-right"
                    size="lg"
                    :class="!isUploading ? 'text-secondary' : 'text-neutral-fg-lo2'"
                />
                <bm-icon icon="cloud" class="ml-2 text-secondary" size="xl" />
            </div>
            <div v-if="hasSomeErrorStatus" class="mb-6 text-danger">
                {{ $tc("filehosting.share.failure", fhFiles.length) }}
            </div>
            <div v-else class="mb-6">
                {{ $tc("filehosting.share.pending", fhFiles.length) }}
            </div>

            <div v-for="(file, idx) in fhFiles" :key="idx" class="position-relative mb-6">
                <detachment-item :file="file">
                    <template #item-actions>
                        <bm-label-icon
                            v-if="hasErrorStatus(file)"
                            class="text-danger ml-2 text-nowrap"
                            icon="exclamation-circle-fill"
                        >
                            {{ $t("filehosting.import.failed") }}
                        </bm-label-icon>
                        <bm-button-close
                            v-else-if="hasNotLoadedStatus(file)"
                            size="sm"
                            class="ml-2"
                            @click="cancel(file.key)"
                        />
                        <div v-else class="text-neutral regular ml-3 text-nowrap">
                            {{ $t("filehosting.import.successful") }}
                        </div>
                    </template>
                </detachment-item>
            </div>
        </div>
        <template #modal-footer>
            <bm-button variant="text" :disabled="!isUploading" @click="cancelAll">
                {{ $t("filehosting.share.stop") }}
            </bm-button>
            <bm-button variant="outline-accent" @click="hideModal">
                {{ isUploading ? $t("common.hide") : $t("common.done") }}
            </bm-button>
        </template>
    </bm-modal>
</template>
<script>
import { mapActions, mapMutations } from "vuex";
import { BmModal, BmButtonClose, BmButton, BmIcon, BmLabelIcon } from "@bluemind/ui-components";
import { computeUnit } from "@bluemind/file-utils";
import { fileUtils } from "@bluemind/mail";
import DetachmentItem from "./DetachmentItem";
import { REMOVE_FH_ATTACHMENT } from "../store/types/actions";
import { getFhInfos } from "../helpers";

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
        isUploading() {
            return this.fhFiles.some(({ status }) => status && isUploading({ status }));
        },
        allFiles() {
            return Object.values(this.$store.state.mail.messageCompose.uploadingFiles) || [];
        },
        hasSomeErrorStatus() {
            return this.fhFiles.some(this.hasErrorStatus);
        },
        dotsClass() {
            return this.isUploading ? "dots" : "text-secondary";
        },
        fhFiles() {
            return this.fhFileKeys.flatMap(key => {
                const file = this.$store.state.mail.messageCompose.uploadingFiles[key];
                const fhFile = file && getFhInfos(file);
                return { ...file, ...fhFile };
            });
        }
    },
    watch: {
        allFiles(values) {
            values.forEach(file => {
                const fhFile = getFhInfos(file);
                if (getFhInfos(file) && !this.fhFileKeys.includes(file.key) && file.status === FileStatus.NOT_LOADED) {
                    this.fhFileKeys.push(file.key);
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
        ...mapActions("mail", [REMOVE_FH_ATTACHMENT]),
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        },
        cancel(key) {
            this.REMOVE_FH_ATTACHMENT({ key });
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
@import "~@bluemind/ui-components/src/css/utils/variables";

#file-hosting-modal {
    .text-neutral-fg-lo2 {
        color: $neutral-fg-lo2;
    }
    .dot-font-size {
        font-size: $h2-font-size;
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
