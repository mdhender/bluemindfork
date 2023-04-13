<template>
    <div class="mail-composer-attach-zone d-flex flex-column justify-content-center align-items-center">
        <h3 class="p-2">{{ text }}</h3>
        <bm-illustration value="drag-n-drop" size="lg" over-background />
    </div>
</template>

<script>
import { BmIllustration } from "@bluemind/ui-components";

export default {
    name: "MailComposerAttachZone",
    components: { BmIllustration },
    props: {
        text: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
.mail-composer-attach-zone {
    height: 100%;
    & > .bm-illustration {
        position: absolute;
    }
    h3 {
        z-index: 1;
    }
}
</style>
