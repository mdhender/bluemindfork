<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="preview-file"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <preview-file-content :file="context.file" :message="message" />
    </bm-extension>
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
import PreviewFileContent from "./PreviewFileContent";

export default {
    name: "PreviewFile",
    components: { BmExtension, PreviewFileContent },
    props: {
        message: {
            type: Object,
            required: true
        },
        file: {
            type: Object,
            required: true
        }
    }
};
</script>
