<template>
    <div
        class="bm-dropzone"
        @mousedown.prevent
        @dragstart="emit"
        @drag="emit"
        @dragenter="emit"
        @dragover="emit"
        @drop="emit"
        @dragleave="emit"
        @dragend="emit"
        @holdover="emit"
        @dropactivate="emit"
        @dropdeactivate="emit"
    >
        <slot />
    </div>
</template>
<script>
import interact from "interactjs";
import { base } from "../js/dragAndDrop/plugins/base";

interact.use(base);

export default {
    name: "BmDropzone",
    props: {
        accept: {
            type: [String, Array],
            required: true
        },
        value: {
            type: [String, Object, Array],
            required: false,
            default: ""
        },
        states: {
            type: Object,
            required: false,
            default: () => ({})
        }
    },
    data() {
        return {
            dropzone: {
                accept: Array.isArray(this.accept) ? this.accept : [this.accept],
                data: this.value
            }
        };
    },
    watch: {
        value() {
            this.dropzone.data = this.value;
            this.$_Dropzone_interactable.data = this.dropzone.data;
        },
        accept() {
            this.dropzone.accept = Array.isArray(this.accept) ? this.accept : [this.accept];
            this.$_Dropzone_interactable.options.drop.accept = this.dropzone.accept;
        }
    },
    mounted() {
        this.$_Dropzone_interactable = interact(this.$el, {
            data: this.dropzone.data
        });

        this.$_Dropzone_interactable.dropzone({ accept: this.dropzone.accept });

        Object.keys(this.states).forEach(name => {
            const state = this.states[name];
            if (!state) {
                this.$_Dropzone_interactable.disableState(name);
            } else {
                this.$_Dropzone_interactable.addState(name, state);
            }
        });
    },
    destroyed() {
        this.$_Dropzone_interactable.unset();
    },
    methods: {
        emit(event) {
            this.$emit(event.type, event);
        },
        refresh() {
            this.$_Dropzone_interactable.refresh();
        }
    }
};
</script>
