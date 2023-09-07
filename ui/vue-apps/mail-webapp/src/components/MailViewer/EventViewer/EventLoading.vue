<script setup>
import { BmSkeleton, BmSkeletonImg } from "@bluemind/ui-components";
</script>

<template>
    <div class="event-loading">
        <div class="event-loading-layout">
            <bm-skeleton-img no-aspect height="100px" width="100px" class="m-5" />
            <div class="event-loading-lines">
                <bm-skeleton width="200px" />
                <bm-skeleton width="200px" />
                <bm-skeleton width="200px" />
            </div>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-loading {
    .event-loading-layout {
        display: flex;
        flex-direction: row;
        gap: $sp-6;

        .event-loading-lines {
            display: flex;
            justify-content: center;
            flex-direction: column;
            gap: $sp-4;
        }
    }
}
</style>
