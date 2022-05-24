<template>
    <bm-modal id="fh-modal" :title="$tc('mail.filehosting.add.large', fhAttachments.length)" title-class="ml-2" no-fade>
        <div class="mr-4 ml-2">
            <div class="d-flex align-items-center mb-3">
                <bm-icon icon="file" size="2x" class="mr-2 text-primary" />
                <span class="dots font-size-h1">&#8226; &#8226; &#8226; &#8226; &#8226; &#8226;</span>
                <bm-icon icon="chevron-right" size="lg" class="text-secondary" />
                <bm-icon icon="cloud" class="ml-2 text-primary" size="2x" />
            </div>
            <div class="mb-4">
                <i18n path="mail.filehosting.threshold.hit">
                    <template v-slot:hit>
                        {{ $tc("mail.filehosting.threshold.size", attachments.length) }}
                    </template>
                    <template v-slot:size>
                        <strong class="font-weight-bold">{{ displaySize(sizeLimit) }}</strong>
                    </template>
                </i18n>
                <br />
                {{ $tc("mail.filehosting.share.pending", fhAttachments.length) }}
            </div>

            <div v-for="(attachment, idx) in fhAttachments" :key="idx" class="position-relative mt-2">
                <fh-attachment-item :attachment="attachment">
                    <template #item-actions>
                        <bm-button-close
                            v-if="attachment.progress.loaded < attachment.progress.total"
                            class="mt-2"
                            @click="cancel(attachment.address)"
                        />
                        <span v-else class="text-secondary mt-2 float-right">
                            {{ $t("mail.filehosting.import.successful") }}
                        </span>
                    </template>
                </fh-attachment-item>
            </div>
        </div>
        <template #modal-footer>
            <bm-button variant="simple-dark" :disabled="totalLoaded === totalSize" @click="cancelAll">
                {{ $t("mail.filehosting.share.stop") }}
            </bm-button>
            <bm-button variant="outline-primary" @click="$bvModal.hide('fh-modal')">
                {{ $t("common.hide") }}
            </bm-button>
        </template>
    </bm-modal>
</template>
<script>
import { mapGetters } from "vuex";
import global from "@bluemind/global";
import { BmModal, BmButtonClose, BmButton, BmIcon } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";
import FhAttachmentItem from "./FhAttachmentItem";

export default {
    name: "FhModal",
    components: { BmModal, BmButtonClose, BmButton, BmIcon, FhAttachmentItem },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", ["GET_FH_ATTACHMENT"]),
        totalLoaded() {
            return this.fhAttachments.reduce((totalLoaded, attachment) => totalLoaded + attachment.progress.loaded, 0);
        },
        totalSize() {
            return this.fhAttachments.reduce((totalSize, attachment) => totalSize + attachment.progress.total, 0);
        },
        fhAttachments() {
            return this.attachments.filter(attachment => this.GET_FH_ATTACHMENT(this.message, attachment));
        },
        sizeLimit() {
            return this.$store.state.mail.messageCompose.maxMessageSize;
        },
        attachments() {
            return this.$store.state.mail.conversations.messages[this.message.key].attachments;
        }
    },
    watch: {
        totalLoaded(val) {
            if (val === this.totalSize) {
                this.$bvModal.hide("fh-modal");
            }
        }
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        },
        cancel(address) {
            global.cancellers[address + this.message.key].cancel();
        },
        cancelAll() {
            this.fhAttachments.map(attachment => this.cancel(attachment.address));
        }
    }
};
</script>
<style lang="scss" scoped>
@import "@bluemind/styleguide/css/_variables.scss";

#fh-modal {
    .text-light {
        color: $alternate-light;
    }
    .font-size-h1 {
        font-size: $h1-font-size;
    }
    .dots {
        background-image: linear-gradient(90deg, transparent 0%, $primary 15%, $primary 85%, transparent 100%);
        background-clip: text;
        -webkit-background-clip: text;
        color: transparent;
        animation-name: dots;
        animation-duration: 3s;
        animation-iteration-count: infinite;
        animation-timing-function: linear;
        background-size: 20%;
        background-color: $secondary;
        background-repeat: no-repeat;
    }
    @keyframes dots {
        0% {
            background-position: -10px;
        }

        to {
            background-position: 110%;
        }
    }
}
</style>
