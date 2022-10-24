<template>
    <div class="mail-composer-attach-zone d-flex flex-column justify-content-center align-items-center">
        <h3 class="p-2">{{ text }}</h3>
        <bm-icon icon="arrow-up" size="xl" />
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/styleguide";

export default {
    name: "MailComposerAttachZone",
    components: { BmIcon },
    props: {
        text: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
$image-height: 181px;

.mail-composer-attach-zone {
    min-height: $image-height;
    background: url("~@bluemind/styleguide/assets/attachment.png") no-repeat center center;
}
</style>
