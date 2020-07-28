<template>
    <div class="image-part-viewer" align="center"><img :src="src" /></div>
</template>

<script>
export default {
    name: "ImagePartViewer",
    props: {
        value: {
            type: Blob,
            required: true
        }
    },
    computed: {
        src() {
            return URL.createObjectURL(this.value);
        }
    }
};
</script>
