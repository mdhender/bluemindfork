<template>
    <div class="composer-alert">
        <div class="text high">
            <div>{{ text }}</div>
            <bm-read-more :href="doc" />
        </div>
        <slot />
    </div>
</template>

<script>
import { BmReadMore } from "@bluemind/ui-components";

export default {
    name: "ComposerAlert",
    components: { BmReadMore },
    props: {
        code: {
            type: Number,
            default: null
        },
        doc: {
            type: String,
            default: null
        },
        text: {
            type: String,
            required: true
        }
    },
    created() {
        if (this.code) {
            console.warn("[S/MIME] decrypt failed with error code ", this.code);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/type";

.composer-alert {
    line-height: $line-height-medium;
    .text {
        display: flex;
        align-items: center;
        gap: $sp-4;
    }
}
</style>
