<template>
    <img v-if="value" class="image-part-viewer d-block m-auto" :src="value" />
    <bm-skeleton-img v-else />
</template>

<script>
import { BmSkeletonImg } from "@bluemind/styleguide";
export default {
    name: "ImagePartViewer",
    components: { BmSkeletonImg },
    props: {
        value: {
            type: String,
            required: false,
            default: undefined
        }
    }
};
</script>
