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

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.file-tags {
    display: flex;
    flex: 0;
    margin-right: $sp-3;
}
</style>
