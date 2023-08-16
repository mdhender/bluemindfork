<template>
    <bm-button-toolbar key-nav class="file-toolbar">
        <preview-button
            v-if="hasButton(ActionButtons.PREVIEW) && isViewable(file)"
            :file="file"
            :disabled="!isAllowedToPreview(file)"
            @preview="$emit('preview')"
        />
        <download-button v-if="hasButton(ActionButtons.DOWNLOAD)" :ref="`download-button-${file.key}`" :file="file" />
        <bm-extension id="webapp" type="list" path="file.actions" :file="file" class="d-flex align-items-center" />
        <other-button v-if="hasButton(ActionButtons.OTHER)" :file="file" :message="message" />
        <template v-if="hasButton(ActionButtons.REMOVE)">
            <remove-button @remove="removeAttachment(file)" />
        </template>
    </bm-button-toolbar>
</template>

<script>
import { mapMutations } from "vuex";
import { BmButtonToolbar } from "@bluemind/ui-components";
import { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY } from "~/mutations";
import { RemoveAttachmentCommand } from "~/commands";
import { fileUtils, partUtils } from "@bluemind/mail";
import { BmExtension } from "@bluemind/extensions.vue";
import PreviewButton from "./ActionButtons/PreviewButton";
import DownloadButton from "./ActionButtons/DownloadButton";
import OtherButton from "./ActionButtons/OtherButton";
import RemoveButton from "./ActionButtons/RemoveButton";

const { isAllowedToPreview, ActionButtons } = fileUtils;
const { isViewable } = partUtils;

export default {
    name: "FileToolbar",
    components: {
        BmButtonToolbar,
        BmExtension,
        DownloadButton,
        OtherButton,
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
        ...mapMutations("mail", { SET_PREVIEW_MESSAGE_KEY, SET_PREVIEW_FILE_KEY }),

        download(file) {
            this.$refs[`download-button-${file.key}`].download();
        },
        removeAttachment({ key }) {
            const attachment = this.message.attachments.find(attachment => attachment.fileKey === key);
            this.$execute("remove-attachment", { attachment, message: this.message });
        },
        hasButton(button) {
            return this.buttons.length === 0 || this.buttons.includes(button);
        },
        isViewable,
        isAllowedToPreview
    }
};
</script>
