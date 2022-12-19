<template>
    <div class="contact-card-skeleton">
        <div class="d-flex"><bm-skeleton-avatar /><bm-skeleton class="ml-2" width="30%" /></div>
        <div class="p-3">
            <bm-skeleton width="85%" />
            <bm-skeleton width="55%" />
            <bm-skeleton width="70%" />
        </div>
        <div class="flex-fill d-flex flex-column p-3">
            <bm-skeleton width="50%" />
            <bm-skeleton width="95%" />
            <bm-skeleton width="45%" />
        </div>
    </div>
</template>

<script>
import { BmSkeleton, BmSkeletonAvatar } from "@bluemind/ui-components";

export default {
    name: "ContactCardSkeleton",
    components: { BmSkeleton, BmSkeletonAvatar }
};
</script>
