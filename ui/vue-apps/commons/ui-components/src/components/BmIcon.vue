<template>
    <font-awesome-icon
        v-if="!stacked"
        :icon="icon"
        :fixed-width="fixedWidth"
        class="bm-icon"
        :class="[variantClass, `fa-${size}`]"
        :style="customStyle"
        :rotation="rotation"
        :flip="flip"
        :transform="transform"
        :title="title"
    />
    <font-awesome-layers
        v-else
        class="bm-icon"
        :class="size ? `fa-${size}` : ''"
        :fixed-width="fixedWidth"
        :title="title"
    >
        <font-awesome-icon
            v-for="(stackedIcon, index) in stacked"
            :key="index"
            :icon="stackedIcon.icon || stackedIcon"
            :fixed-width="stackedIcon.fixedWidth"
            class="bm-icon"
            :class="[stackedIcon.class || variantClass, `fa-${stackedIcon.size || size}`]"
            :style="stackedIcon.customStyle || customStyle"
            :rotation="stackedIcon.rotation || rotation"
            :flip="stackedIcon.flip || flip"
            :transform="stackedIcon.transform || transform"
        />
    </font-awesome-layers>
</template>

<script>
import { FontAwesomeIcon, FontAwesomeLayers } from "@fortawesome/vue-fontawesome";
import { library } from "@fortawesome/fontawesome-svg-core";

initIcons();

export { library };

export default {
    name: "BmIcon",
    components: {
        FontAwesomeIcon,
        FontAwesomeLayers
    },
    props: {
        fixedWidth: { type: Boolean, default: false },
        flip: { type: String, default: undefined },
        icon: { type: [Object, Array, String], default: undefined },
        customStyle: { type: [Object, Array, String], default: undefined },
        rotation: { type: String, default: undefined },
        size: {
            type: String,
            default: "md",
            validator: function (value) {
                return ["xs", "sm", "md", "lg", "xl", "2xl", "3xl", "4xl", "5xl"].includes(value);
            }
        },
        stacked: { type: Array, default: undefined },
        title: { type: String, default: undefined },
        transform: { type: String, default: undefined },
        variant: { type: String, default: "" }
    },
    computed: {
        variantClass() {
            if (this.variant) {
                return `text-${this.variant}`;
            }
            return "";
        }
    }
};

function initIcons() {
    const iconsLoader = require.context("../icons", false, /\.js$/);

    iconsLoader.keys().forEach(icon => {
        library.add(iconsLoader(icon));
    });
}
</script>

<style lang="scss">
@import "../css/_variables.scss";

@each $name, $value in $icon-sizes {
    .bm-icon.fa-#{$name} {
        width: $value;
        height: $value;
        flex: none;
    }
}
</style>
