<template functional>
    <div class="event-header">
        <div class="label pl-3">
            <slot name="default" />
        </div>
        <div class="actions">
            <slot name="actions" />
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-header {
    @include until-lg {
        display: grid;
        gap: $sp-4;
    }
    @include from-lg {
        display: flex;
        align-items: center;
        padding: $sp-5 $sp-5 $sp-5 $sp-6;
        .label {
            text-align: center;
        }
    }
    border-width: 1px 0 1px 0;
    border-style: solid;
    border-color: $neutral-fg-lo3;
    padding: $sp-5;
    justify-content: space-between;
    background-color: $neutral-bg;
    min-height: 50px;

    .label {
        line-height: $line-height-sm;
    }
}
</style>
