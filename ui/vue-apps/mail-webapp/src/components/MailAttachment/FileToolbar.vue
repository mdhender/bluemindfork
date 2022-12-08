<template>
    <bm-button-toolbar key-nav class="file-toolbar">
        <preview-button
            v-if="hasButton(ActionButtons.PREVIEW) && isViewable(file)"
            :file="file"
            :disabled="!isAllowedToPreview(file)"
            @preview="openPreview(file)"
        />
        <download-button v-if="hasButton(ActionButtons.DOWNLOAD)" :ref="`download-button-${file.key}`" :file="file" />
        <other-button v-if="hasButton(ActionButtons.OTHER)" :file="file" :message="message" />
        <template v-if="hasButton(ActionButtons.REMOVE)">
            <cancel-button v-if="isUploading(file)" @cancel="cancel(file)" />
            <remove-button v-else @remove="removeAttachment(file)" />
        </template>
    </bm-button-toolbar>
</template>

<script>
import { mapMutations } from "vuex";
import global from "@bluemind/global";
import { BmButtonToolbar } from "@bluemind/ui-components";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";
import { RemoveAttachmentCommand } from "~/commands";
import { fileUtils, partUtils } from "@bluemind/mail";
import PreviewButton from "./ActionButtons/PreviewButton";
import DownloadButton from "./ActionButtons/DownloadButton";
import OtherButton from "./ActionButtons/OtherButton";
import RemoveButton from "./ActionButtons/RemoveButton";
import CancelButton from "./ActionButtons/CancelButton";

const { isAllowedToPreview, ActionButtons, isUploading } = fileUtils;
const { isViewable } = partUtils;

export default {
    name: "FileToolbar",
    components: {
        BmButtonToolbar,
        PreviewButton,
        DownloadButton,
        OtherButton,
        RemoveButton,
        CancelButton
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
        ...mapMutations("mail", { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY }),
        openPreview(file) {
            this.SET_PREVIEW_MESSAGE_KEY(this.message.key);
            this.SET_PREVIEW_FILE_KEY(file.key);
            this.$bvModal.show("preview-modal");
        },
        download(file) {
            this.$refs[`download-button-${file.key}`].clickButton();
        },
        removeAttachment({ key }) {
            const attachment = this.message.attachments.find(attachment => attachment.fileKey === key);
            this.$execute("remove-attachment", { attachment, message: this.message });
        },
        cancel(file) {
            global.cancellers[file.key].cancel();
        },
        hasButton(button) {
            return this.buttons.length === 0 || this.buttons.includes(button);
        },
        isViewable,
        isAllowedToPreview,
        isUploading
    }
};
</script>
