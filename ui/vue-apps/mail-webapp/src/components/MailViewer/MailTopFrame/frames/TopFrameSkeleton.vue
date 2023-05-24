<template>
    <div class="top-frame-skeleton d-flex align-items-center">
        <bm-skeleton-img class="mr-5" no-aspect width="120px" height="100px" :animation="null" />
        <div class="details flex-fill">
            <bm-skeleton width="85%" />
            <bm-skeleton width="55%" />
            <bm-skeleton width="70%" />
        </div>
    </div>
</template>

<script>
import { BmSkeleton, BmSkeletonImg } from "@bluemind/ui-components";

export default {
    name: "TopFrameSkeleton",
    components: { BmSkeleton, BmSkeletonImg }
};
</script>
