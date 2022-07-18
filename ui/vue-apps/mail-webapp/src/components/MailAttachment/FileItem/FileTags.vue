<template>
    <bm-extension id="webapp.mail" class="file-tags" path="attachment.infos.tags" :file="file" />
</template>

<script>
import { BmExtension } from "@bluemind/extensions.vue";
export default {
    name: "FileTags",
    components: { BmExtension },
    props: {
        file: {
            type: Object,
            required: true
        }
    }
};
</script>
