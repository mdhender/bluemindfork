<script>
import { computed, h, useAttrs, useListeners, useSlots } from "vue";
import BmButton from "../buttons/BmButton";
import BmCaptionedIconButton from "../buttons/BmCaptionedIconButton";
import BmDropdownItemButton from "../dropdown/BmDropdownItemButton";
import BmDropdown from "../dropdown/BmDropdown";
import BmToolbarElement from "./BmToolbarElement";

export default {
    name: "BmToolbarCaptionedIconButton",
    components: {
        BmButton,
        BmDropdown,
        BmDropdownItemButton,
        BmToolbarElement
    },
    inheritAttrs: false,
    props: {
        extension: {
            type: String,
            default: undefined
        },
        caption: {
            type: String,
            required: true
        },
        icon: {
            type: String,
            required: true
        }
    },
    setup(props) {
        const attrs = useAttrs();
        const options = computed(() => ({
            on: useListeners(),
            class: "bm-toolbar-captioned-icon-button"
        }));
        const slots = useSlots();

        return function render() {
            return h(BmToolbarElement, {
                props,
                scopedSlots: {
                    toolbar: () => h(BmCaptionedIconButton, { ...options.value, attrs, props, scopedSlots: slots }),
                    menu: () =>
                        h(
                            BmDropdownItemButton,
                            { ...options.value, attrs: { ...attrs, variant: undefined }, props: { icon: props.icon } },
                            props.caption
                        ),
                    "menu-with-extensions": ({ extensions }) =>
                        h(
                            BmDropdown,
                            {
                                ...options.value,
                                attrs: { ...attrs, variant: undefined },
                                props: { icon: props.icon, text: props.caption, split: true }
                            },
                            extensions
                        )
                }
            });
        };
    }
};
</script>
