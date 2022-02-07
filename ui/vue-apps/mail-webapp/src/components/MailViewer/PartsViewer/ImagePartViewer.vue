<template>
    <img v-if="src" class="image-part-viewer d-block m-auto" :src="src" />
    <bm-skeleton-img v-else />
</template>

<script>
import { BmSkeletonImg } from "@bluemind/styleguide";
import { getPartPreviewUrl } from "@bluemind/email";

import PartViewerMixin from "./PartViewerMixin";
export default {
    name: "ImagePartViewer",
    components: { BmSkeletonImg },
    mixins: [PartViewerMixin],
    computed: {
        src() {
            return getPartPreviewUrl(this.message.folderRef.uid, this.message.remoteRef.imapUid, this.part);
        }
    }
};
</script>
