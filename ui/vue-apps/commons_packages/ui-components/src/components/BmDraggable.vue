<template>
    <div class="bm-draggable" tabindex="-1">
        <div
            ref="main"
            @mousedown.prevent
            @dragstart="emit"
            @drag="emit"
            @dragenter="emit"
            @dragover="emit"
            @drop="emit"
            @dragleave="emit"
            @dragend="emit"
            @holdover="emit"
        >
            <slot />
        </div>
        <div ref="shadow" class="bm-draggable-shadow">
            <slot name="shadow" />
        </div>
        <div ref="tooltip" class="bm-draggable-tooltip">
            <bm-icon v-if="tooltip_.cursor" :icon="tooltip_.cursor" /> {{ tooltip_.text }}
        </div>
    </div>
</template>
<script>
import interact from "interactjs";
import BmIcon from "./BmIcon";
import { base } from "../js/dragAndDrop/plugins/base";

interact.use(base);

export default {
    name: "BmDraggable",
    components: { BmIcon },
    props: {
        name: {
            type: String,
            required: true
        },
        value: {
            type: [String, Object, Array],
            required: false,
            default: ""
        },
        dropzone: {
            type: Array,
            required: false,
            default: () => []
        },
        disableTouch: {
            type: Boolean,
            required: false,
            default: false
        },
        handle: {
            type: String,
            required: false,
            default: null
        },
        position: {
            type: String,
            required: false,
            default: ".bm-drag-handle"
        },
        states: {
            type: Object,
            required: false,
            default: () => ({})
        },
        tooltip: {
            type: [String, Object],
            required: false,
            default: null
        },
        enabled: {
            type: Boolean,
            required: false,
            default: true
        },
        autoscroll: {
            type: [Boolean, Object],
            default: false
        }
    },
    data() {
        const tooltip = (typeof this.tooltip === "object" && this.tooltip) || { cursor: null, text: this.tooltip };
        return {
            draggable: {
                name: this.name,
                data: this.value
            },
            tooltip_: {
                cursor: tooltip.cursor,
                text: tooltip.text
            }
        };
    },
    watch: {
        tooltip: {
            deep: true,
            handler() {
                const tooltip = (typeof this.tooltip === "object" && this.tooltip) || {
                    cursor: null,
                    text: this.tooltip
                };
                this.tooltip_.text = tooltip.text;
                this.tooltip_.cursor = tooltip.cursor;
            }
        },
        tooltip_: {
            deep: true,
            handler() {
                this.$nextTick(() => this.$_Draggable_interactable.tooltip({ text: this.$refs.tooltip.innerHTML }));
            }
        },
        name() {
            this.draggable.name = this.name;
            this.$_Draggable_interactable.name = this.draggable.name;
        },
        value() {
            this.draggable.data = this.value;
            this.$_Draggable_interactable.data = this.draggable.data;
        }
    },
    mounted() {
        this.$_Draggable_interactable = interact(this.$refs.main, {
            name: this.draggable.name,
            data: this.draggable.data,
            autoScroll: true,
            styleCursor: false
        });
        this.$_Draggable_interactable.draggable({
            allowFrom: this.handle,
            autoScroll: this.autoscroll
        });
        if (this.$slots.shadow) {
            this.$_Draggable_interactable.shadow({ element: this.$refs.shadow });
        } else {
            this.$_Draggable_interactable.clone();
        }
        if (this.position) {
            this.$_Draggable_interactable.position({ position: this.position });
        }
        if (this.disableTouch) {
            this.$_Draggable_interactable.disableTouch();
        }

        this.$_Draggable_interactable.move().remove();

        Object.keys(this.states).forEach(name => {
            const state = this.states[name];
            if (!state) {
                this.$_Draggable_interactable.disableState(name);
            } else {
                this.$_Draggable_interactable.addState(name, state);
            }
        });

        if (this.dropzone.length > 0) {
            this.$_Draggable_interactable.dropzone({ accept: this.dropzone });
        }
        if (this.tooltip_.text || this.tooltip_.cursor) {
            this.$_Draggable_interactable.tooltip({ text: this.$refs.tooltip.innerHTML });
        }
    },
    destroyed() {
        this.$_Draggable_interactable.unset();
    },
    methods: {
        emit(event) {
            this.$emit(event.type, event);
        },
        refresh() {
            this.$_Draggable_interactable.refresh();
        }
    }
};
</script>
<style>
.bm-draggable {
    user-select: none;
    -moz-user-select: none;
}
.bm-drag-handle {
    cursor: grab;
}

.bm-draggable img {
    user-select: none;
    -moz-user-select: none;
}

.bm-draggable-tooltip {
    display: none;
}

.bm-draggable-shadow {
    display: none;
}
</style>
