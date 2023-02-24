<template>
    <div class="decrypt-error-alert">
        {{ $t("smime.mailapp.alert.decrypt_failed") }}
        <span v-if="code" class="code"> ({{ $t("common.error.code", { code }) }})</span>
    </div>
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
