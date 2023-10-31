<template>
    <div class="image-button">
        <bm-icon-button
            variant="compact"
            size="lg"
            icon="image"
            :disabled="disabled"
            :title="$t('styleguide.rich_editor.image.tooltip')"
            @click="openFilePicker"
        />
        <input ref="fileInputAddImage" type="file" hidden :accept="imageMimeType + '*'" @change="insertImage" />
    </div>
</template>

<script>
import { insertImage } from "roosterjs-editor-api";
import { MimeType } from "@bluemind/email";
import BmIconButton from "../../buttons/BmIconButton";

export default {
    components: { BmIconButton },
    props: {
        editor: {
            type: Object,
            required: true
        },
        disabled: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            imageMimeType: MimeType.IMAGE
        };
    },
    methods: {
        openFilePicker() {
            this.$refs.fileInputAddImage.click();
        },
        insertImage(event) {
            if (event.target.files.length === 1) {
                const file = event.target.files[0];
                if (MimeType.typeEquals(file.type, MimeType.IMAGE)) {
                    insertImage(this.editor, file);
                }
            }
        }
    }
};
</script>
