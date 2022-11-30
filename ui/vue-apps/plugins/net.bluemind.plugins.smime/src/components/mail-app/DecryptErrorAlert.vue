<template>
    <div class="decrypt-error-alert">
        <!-- FIXME i18n -->
        Impossible d'afficher le message chiffré
        <span v-if="code" class="code">(Code erreur {{ code }})</span>
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
