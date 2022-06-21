<template>
    <div v-if="src" class="image-part-viewer"><img :src="src" /></div>
    <bm-skeleton-img v-else />
</template>
<script>
import { BmSkeletonImg } from "@bluemind/styleguide";

import PartViewerMixin from "./../PartViewerMixin";
export default {
    name: "ImagePartViewer",
    components: { BmSkeletonImg },
    mixins: [PartViewerMixin],
    $capabilities: ["image/*"]
};
</script>
<style lang="scss">
.image-part-viewer {
    display: flex;
    align-items: center;
    img {
        max-height: 100%;
        max-width: 100%;
        margin: 0 auto;
    }
}
</style>
