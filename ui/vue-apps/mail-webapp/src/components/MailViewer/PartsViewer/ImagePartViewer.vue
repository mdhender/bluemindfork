<template>
    <div class="image-part-viewer" align="center"><img :src="blobUrl" /></div>
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
    data() {
        return {
            blobUrl: null
        };
    },
    watch: {
        value: {
            handler: function () {
                if (this.blobUrl) {
                    URL.revokeObjectURL(this.blobUrl);
                }
                this.blobUrl = URL.createObjectURL(this.value);
            },
            immediate: true
        }
    },
    destroyed() {
        URL.revokeObjectURL(this.blobUrl);
    }
};
</script>
