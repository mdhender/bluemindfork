<template>
    <div v-if="src" class="image-file-viewer"><img :src="src" /></div>
    <bm-skeleton-img v-else />
</template>
<script>
import { BmSkeletonImg } from "@bluemind/styleguide";
import FileViewerMixin from "../FileViewerMixin";

export default {
    name: "ImageFileViewer",
    components: { BmSkeletonImg },
    mixins: [FileViewerMixin],
    $capabilities: ["image/*"]
};
</script>
<style lang="scss">
.image-file-viewer {
    display: flex;
    align-items: center;
    img {
        max-height: 100%;
        max-width: 100%;
        margin: 0 auto;
    }
}
</style>
