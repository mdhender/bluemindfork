<template>
    <span>
        <span v-if="!!iconApp.svg" v-html="iconApp.svg" />
        <bm-icon v-else-if="!!iconApp.name" :name="iconApp.name" />
        <img v-else-if="!!iconApp.url" :href="iconApp.url" aria-hidden="true">
    </span>
</template>

<script>
export default {
    name: "BmBannerAppIcon",
    props: {
        iconApp: {
            type: Object,
            required: true
        }
    }
};
</script>