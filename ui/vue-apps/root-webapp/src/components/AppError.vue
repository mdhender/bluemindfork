<template>
    <div class="app-error d-flex flex-fill align-items-center text-danger text-center h2">
        <i18n class="w-100" tag="div" path="common.application.bootstrap.error">
            <template #title>
                <div class="error-line">{{ $t("common.application.bootstrap.error.title") }}</div>
            </template>
            <template #solution1>
                <div class="error-line">{{ $t("common.application.bootstrap.error.solution1") }}</div>
            </template>
            <template #solution2>
                <div class="error-line">{{ $t("common.application.bootstrap.error.solution2") }}</div>
            </template>
        </i18n>
    </div>
</template>

<script>
export default {
    name: "AppError"
};
</script>

<style lang="scss">
.app-error {
    .error-line {
        line-height: 2rem;
    }
}
</style>
