<template>
    <bm-extension
        id="webapp.mail"
        v-slot="context"
        class="file-item"
        path="message.file"
        type="renderless"
        :file="file"
    >
        <file-item-content
            :file="context.file"
            :compact="compact"
            @click-item="$emit('click-item', $event)"
            @remote-content="$emit('remote-content')"
        >
            <template #actions="slotProps">
                <slot name="actions" :file="slotProps.file" />
            </template>
            <template #overlay="slotProps">
                <slot name="overlay" :hasPreview="slotProps.hasPreview" :file="slotProps.file" />
            </template>
        </file-item-content>
    </bm-extension>
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
import FileItemContent from "./FileItemContent.vue";

export default {
    name: "FileItem",
    components: {
        FileItemContent,
        BmExtension
    },
    props: {
        file: {
            type: Object,
            required: true
        },
        compact: {
            type: Boolean,
            default: false
        }
    }
};
</script>
