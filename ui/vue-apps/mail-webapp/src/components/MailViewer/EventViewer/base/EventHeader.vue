<template functional>
    <div class="event-header">
        <div class="label">
            <slot name="default" />
        </div>
        <slot name="actions" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-header {
    padding: $sp-5;
    @include from-lg {
        padding-left: base-px-to-rem(26);
    }

    @include until-lg {
        display: grid;
    }
    @include from-lg {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
    }

    border-width: 1px 0;
    border-style: solid;
    border-color: $neutral-fg-lo3;
    padding: $sp-5;
    justify-content: space-between;
    background-color: $neutral-bg;
    min-height: base-px-to-rem(38);
    @include from-lg {
        min-height: base-px-to-rem(52);
    }
}
</style>
