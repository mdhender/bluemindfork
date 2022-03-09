<template>
    <div class="mail-folder-item-shadow d-flex py-1 align-items-center">
        <bm-icon icon="6dots-v" class="bm-drag-handle px-2" />
        <mail-folder-icon :folder="folder" :inline="false" class="font-weight-bold pl-2" />
    </div>
</template>
<script>
import { BmIcon } from "@bluemind/styleguide";
import MailFolderIcon from "../MailFolderIcon";

export default {
    name: "MailFolderItemShadow",
    components: { BmIcon, MailFolderIcon },
    props: {
        folder: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/mixins";
@import "~@bluemind/styleguide/css/variables";

$shadow-width: 200px;

.mail-folder-item-shadow {
    width: $shadow-width;
    background-color: $surface-bg;
    .mail-folder-icon {
        @include text-overflow;
    }
}
</style>
