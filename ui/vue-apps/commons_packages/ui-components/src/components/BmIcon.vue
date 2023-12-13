<template>
    <div v-if="stacked" class="bm-icon stacked" :class="`icon-${size}`">
        <bm-icon
            v-for="(layer, index) in stacked"
            :key="index"
            :icon="layer.icon"
            :class="layer.class"
            :style="layer.style"
            :variant="layer.variant"
            :size="size"
        />
    </div>
    <component
        :is="iconComponent"
        v-else
        role="img"
        class="bm-icon"
        :class="`icon-${icon} icon-${size} ${variantClass}`"
    />
</template>

<script>
import useBmIconLibrary from "./useBmIconLibrary";
const library = useBmIconLibrary();

export default {
    name: "BmIcon",
    props: {
        icon: { type: String, default: undefined },
        stacked: { type: Array, default: undefined },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["xs", "sm", "md", "lg", "xl", "2xl", "3xl", "4xl", "5xl"].includes(value);
            }
        },
        variant: { type: String, default: "" }
    },
    computed: {
        iconComponent() {
            return this.icon && library[this.icon] ? { template: library[this.icon] } : null;
        },
        variantClass() {
            if (this.variant) {
                return `text-${this.variant}`;
            }
            return "";
        }
    }
};
</script>

<style lang="scss">
@import "../css/utils/variables.scss";

svg.bm-icon > path {
    fill: currentColor;
}

.bm-icon {
    display: inline-block;
}

.bm-icon.stacked {
    position: relative;
    > .bm-icon {
        position: absolute;
        top: 0;
        left: 0;
    }
}

@each $name, $value in $icon-sizes {
    .bm-icon.icon-#{$name} {
        width: $value;
        height: $value;
        flex: none;
    }
}
</style>
