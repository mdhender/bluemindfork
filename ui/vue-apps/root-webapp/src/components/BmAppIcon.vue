<template>
    <span class="bm-app-icon">
        <!-- eslint-disable-next-line vue/no-v-html -->
        <span v-if="!!iconApp.svg" v-html="iconApp.svg" />
        <bm-icon v-else-if="!!iconApp.name" :icon="iconApp.name" />
        <img v-else-if="!!iconApp.url" :href="iconApp.url" aria-hidden="true" :alt="iconApp.name" />
    </span>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";
export default {
    name: "BmAppIcon",
    components: { BmIcon },
    props: {
        iconApp: {
            type: Object,
            required: true
        }
    }
};
</script>
