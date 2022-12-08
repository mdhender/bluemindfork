<template>
    <bm-modal id="preview-modal" ref="modal" class="preview-modal" centered size="fluid" hide-footer hide-header>
        <global-events @keydown.left="previous" @keydown.up="previous" @keydown.down="next" @keydown.right="next" />

        <preview-header
            :file="file"
            :files-count="filesCount"
            :message="message"
            :expanded.sync="expanded"
            @close="$refs.modal.hide()"
            @previous="previous"
            @next="next"
        />
        <div class="content">
            <bm-collapse v-model="expanded" :class="{ 'd-none': true, 'd-lg-block': expanded }">
                <preview-message :message="message" :active-file="file" />
            </bm-collapse>
            <preview-file :message="message" :file="file" />
        </div>
        <preview-file-header :file="file" class="d-lg-none d-flex bottom-file-info" />
    </bm-modal>
</template>

<script>
import { mapMutations } from "vuex";
import { BmCollapse, BmModal } from "@bluemind/ui-components";

import { SET_PREVIEW_FILE_KEY } from "~/mutations";
import PreviewFile from "./Preview/PreviewFile";
import PreviewMessage from "./Preview/PreviewMessage";
import PreviewHeader from "./Preview/PreviewHeader";
import GlobalEvents from "vue-global-events";
import PreviewFileHeader from "./Preview/PreviewFileHeader";

export default {
    name: "PreviewModal",
    components: {
        BmCollapse,
        BmModal,
        PreviewFile,
        PreviewMessage,
        PreviewHeader,
        GlobalEvents,
        PreviewFileHeader
    },
    data() {
        return { expanded: true };
    },
    computed: {
        message() {
            const message = this.$store.state.mail.conversations.messages[this.$store.state.mail.preview.messageKey];

            return message
                ? {
                      ...message,
                      composing: false
                  }
                : undefined;
        },
        file() {
            return this.files.find(({ key }) => this.$store.state.mail.preview.fileKey === key);
        },
        fileIndex() {
            return this.files.findIndex(({ key }) => this.$store.state.mail.preview.fileKey === key);
        },
        filesCount() {
            return this.files.length;
        },
        files() {
            if (this.message && this.message.attachments) {
                return this.message.attachments.map(({ fileKey }) => {
                    return this.$store.state.mail.files[fileKey];
                });
            }
            return [];
        }
    },
    methods: {
        ...mapMutations("mail", { SET_PREVIEW_FILE_KEY }),
        next() {
            this.selectPreview(this.fileIndex, index => index + 1);
        },
        previous() {
            this.selectPreview(this.fileIndex, index => index + this.filesCount - 1);
        },
        selectPreview(current, iterator) {
            const index = iterator(current) % this.filesCount;
            if (index !== this.fileIndex) {
                const file = this.files[index];
                this.SET_PREVIEW_FILE_KEY(file.key);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
#preview-modal .modal-body {
    .content {
        display: flex;
        flex: 1 1 auto;
        min-height: 0;
        height: 80vh;
    }
    padding: 0;
    .preview-message-header,
    .collapse {
        flex-basis: 25%;
        max-width: 25%;
        flex-grow: 0;
        flex-shrink: 0;
    }
    .collapse {
        overflow: auto;
    }
    .preview-file {
        flex: 1 1 auto;
        min-height: 0;
        max-height: 100%;
    }
    .bottom-file-info {
        flex: none;
        height: base-px-to-rem(24);
        align-items: center;
    }
}
</style>
