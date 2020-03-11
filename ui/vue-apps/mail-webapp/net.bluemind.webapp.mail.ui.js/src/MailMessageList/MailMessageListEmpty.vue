<template>
    <div class="h-100 bg-extra-light d-flex flex-column align-items-center justify-content-center">
        <div class="py-5">
            <slot />
        </div>
        <div
            v-if="image"
            class="empty-folder-illustration"
            :style="'background: url(' + image + ') no-repeat center top'"
        />
    </div>
</template>

<script>
export default {
    name: "MailMessageListEmpty",
    props: {
        image: {
            type: String,
            required: false,
            default: null
        }
    }
};
</script>

<style scoped>
.empty-folder-illustration {
    flex: auto 1 1;
    width: 100%;
}
</style>
