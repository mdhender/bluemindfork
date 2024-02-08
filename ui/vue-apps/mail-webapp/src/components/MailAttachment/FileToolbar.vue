<template>
    <div class="file-toolbar">
        <bm-toolbar
            key-nav
            extension-id="webapp.mail"
            extension="file.actions"
            class="d-flex align-items-center"
            :file="file"
            :message="message"
            menu-icon="3dots-v"
            menu-icon-variant="compact"
            :max-items="2"
        >
            <preview-button
                v-if="hasButton(ActionButtons.PREVIEW) && isViewable(file)"
                :file="file"
                :disabled="!isAllowedToPreview(file)"
                @preview="preview(file)"
            />
            <download-button
                v-if="hasButton(ActionButtons.DOWNLOAD)"
                :ref="`download-button-${file.key}`"
                :file="file"
            />
        </bm-toolbar>
        <remove-button v-if="hasButton(ActionButtons.REMOVE)" @remove="removeAttachment(file)" />
    </div>
</template>

<script>
import { mapActions, mapMutations } from "vuex";
import { BmToolbar } from "@bluemind/ui-components";
import { SET_PREVIEW } from "~/actions";
import { RemoveAttachmentCommand } from "~/commands";
import { fileUtils, partUtils } from "@bluemind/mail";
import PreviewButton from "./ActionButtons/PreviewButton";
import DownloadButton from "./ActionButtons/DownloadButton";
import RemoveButton from "./ActionButtons/RemoveButton";

const { isAllowedToPreview, ActionButtons } = fileUtils;
const { isViewable } = partUtils;

export default {
    name: "FileToolbar",
    components: {
        BmToolbar,
        DownloadButton,
        PreviewButton,
        RemoveButton
    },
    mixins: [RemoveAttachmentCommand],
    props: {
        file: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        },
        buttons: {
            type: Array,
            default: () => [],
            validator: function (buttons) {
                return buttons.length === 0 || buttons.every(button => Object.values(ActionButtons).includes(button));
            }
        }
    },
    data() {
        return {
            ActionButtons
        };
    },
    methods: {
        ...mapActions("mail", { SET_PREVIEW }),
        download(file) {
            this.$refs[`download-button-${file.key}`].download();
        },
        removeAttachment({ address }) {
            this.$execute("remove-attachment", { address, message: this.message });
        },
        hasButton(button) {
            return this.buttons.length === 0 || this.buttons.includes(button);
        },
        preview(file) {
            this.SET_PREVIEW({ messageKey: this.message.key, fileKey: file.key });
            this.$bvModal.show("preview-modal");
        },
        isViewable,
        isAllowedToPreview
    }
};
</script>
