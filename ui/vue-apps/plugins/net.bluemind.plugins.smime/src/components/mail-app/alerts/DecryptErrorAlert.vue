<template>
    <div class="decrypt-error-alert">{{ $t("alert.smime.decrypt_failed") }}</div>
</template>

<script>
export default {
    name: "DecryptErrorAlert",
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        code() {
            return this.alert.payload;
        }
    },
    created() {
        console.warn("[S/MIME] decrypt failed with error code ", this.code);
    }
};
</script>

<style lang="scss">
.decrypt-error-alert {
    .code {
        font-style: italic;
    }
}
</style>
