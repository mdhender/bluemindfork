<template functional>
    <img class="image-part-viewer d-block m-auto" :src="props.value" />
</template>

<script>
export default {
    name: "ImagePartViewer",
    props: {
        value: {
            type: String,
            required: true
        }
    }
};
</script>
