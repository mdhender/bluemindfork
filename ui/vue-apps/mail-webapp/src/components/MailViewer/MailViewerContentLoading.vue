<template>
    <div class="mail-viewer-content-loading">
        <bm-skeleton width="20%" />
        <bm-skeleton width="100%" />
        <bm-skeleton width="100%" />
        <bm-skeleton width="60%" />
        <bm-skeleton width="100%" />
        <bm-skeleton width="100%" />
        <bm-skeleton width="50%" />
        <br />
        <bm-skeleton width="20%" />
        <bm-skeleton width="15%" />
    </div>
</template>

<script>
import { BmSkeleton } from "@bluemind/styleguide";

export default {
    name: "MailViewerContentLoading",
    components: { BmSkeleton }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.text-plain-part-loading {
    margin-top: $sp-3;
    .b-skeleton-text {
        margin-bottom: $sp-2;
    }
}
</style>
