<template>
    <div class="default-fallback">
        <div><bm-icon :icon="icon" size="4xl" /></div>
        <span class="text">{{ text }}</span>
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";

export default {
    name: "DefaultFallback",
    components: { BmIcon },
    props: {
        text: {
            type: String,
            default: ""
        },
        icon: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.default-fallback {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    .bm-icon {
        color: $lightest;
        background-color: $darkest;
        opacity: 0.7;
    }

    .text {
        color: $lightest;
    }
}
</style>
