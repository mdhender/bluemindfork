<template>
    <div
        v-if="files.length > 0"
        :class="`composer-links ${className}`"
        style="padding: 15px; background-color: #d9edff;"
        contenteditable="false"
    >
        <div style="margin-bottom: 15px;">
            {{ $tc("filehosting.link", files.length) }}
        </div>
        <composer-links :files="files" style="padding: 15px;" />
    </div>
</template>

<script>
import ComposerLinks from "./ComposerLinks";
export default {
    name: "ComposerLinksWithFrame",
    components: { ComposerLinks },
    props: {
        className: {
            type: String,
            required: true
        },
        files: {
            type: Array,
            required: true
        }
    }
};
</script>
