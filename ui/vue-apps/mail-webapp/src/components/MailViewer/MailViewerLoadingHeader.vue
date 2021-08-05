<template>
    <div class="d-flex flex-column bg-surface">
        <bm-row class="px-lg-5 px-4 pt-2">
            <bm-col cols="12">
                <bm-skeleton width="15%" />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4 mb-2">
            <bm-col cols="12">
                <h1><bm-skeleton width="30%" /></h1>
            </bm-col>
        </bm-row>
        <bm-row class="d-flex px-lg-5 px-4 justify-content-between">
            <bm-col cols="6" class="d-flex align-items-center">
                <bm-skeleton-avatar />
                <bm-skeleton class="ml-2 flex-fill" />
            </bm-col>
            <bm-col cols="2">
                <bm-skeleton />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5">
            <bm-col cols="12">
                <hr class="my-2" />
            </bm-col>
        </bm-row>
        <bm-row class="px-lg-5 px-4">
            <bm-col cols="12">
                <bm-skeleton width="20%" />
                <bm-skeleton width="15%" />
            </bm-col>
        </bm-row>
    </div>
</template>

<script>
import { BmCol, BmRow, BmSkeleton, BmSkeletonAvatar } from "@bluemind/styleguide";

export default {
    name: "MailVIewerLoadingHeader",
    components: {
        BmCol,
        BmRow,
        BmSkeleton,
        BmSkeletonAvatar
    }
};
</script>
