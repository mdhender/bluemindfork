<script>
import { useExtensions } from "@bluemind/extensions.vue";
import BmButton from "../buttons/BmButton.vue";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton.vue";
import BmDropdown from "../dropdown/BmDropdown.vue";
import { useToolbarContext } from "./toolbar";
import { computed, h, useAttrs, useListeners, useSlots } from "vue";
import VNodes from "../VNodes.vue";

export default {
    name: "BmToolbarButton",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItemButton,
        VNodes
    },
    inheritAttrs: false,
    props: {
        extension: {
            type: String,
            default: undefined
        }
    },
    setup(props) {
        const { renderWebAppExtensions } = useExtensions();
        const attrs = useAttrs();
        const listeners = useListeners();
        const slots = useSlots();
        const { isInToolbar } = useToolbarContext();

        const extensions = computed(() => renderWebAppExtensions(props.extension));
        return { extensions, isInToolbar };
    }
};
</script>

<template>
    <bm-button v-if="isInToolbar" v-bind="$attrs" :extension="extension" v-on="$listeners"><slot /></bm-button>
    <bm-dropdown v-else-if="extensions.length" v-bind="$attrs" :text="$slots.default[0].text.trim()" v-on="$listeners">
        <v-nodes :vnodes="extensions" />
    </bm-dropdown>
    <bm-dropdown-item-button v-else v-bind="$attrs" v-on="$listeners"><slot /> </bm-dropdown-item-button>
</template>
