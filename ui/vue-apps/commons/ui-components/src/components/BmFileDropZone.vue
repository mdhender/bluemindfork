<template>
    <div class="bm-file-drop-zone d-flex flex-column" :class="getClassForDropzone" @drop="onFilesDrop">
        <global-events
            @dragover="onDragover"
            @drop="dropzoneStyle = 'none'"
            @dragleavedocument="dropzoneStyle = 'none'"
            @dragstart="internalDrag = true"
            @dragend="internalDrag = false"
        />
        <div
            v-show="showDropzone"
            ref="dropZone"
            class="flex-grow-1 align-items-center justify-content-center flex-column bm-dropzone-active-content"
            :class="{ 'd-flex': showDropzone }"
        >
            <slot name="dropZone" />
        </div>
        <!-- in order to have efficient drag events use v-show instead of v-if since the modification of the DOM has an impact -->
        <div
            v-show="!showDropzone"
            ref="contentZone"
            class="flex-grow-1"
            :class="{
                'bm-dropzone-inline-hover ': inline && dropzoneStyle === 'hover',
                'd-flex flex-column': !showDropzone
            }"
        >
            <slot />
        </div>
    </div>
</template>
<script>
import GlobalEvents from "vue-global-events";

export default {
    name: "BmFileDropZone",
    components: {
        GlobalEvents
    },
    props: {
        shouldActivateFn: {
            type: Function,
            default: () => true
        },
        inline: {
            type: Boolean,
            required: false,
            default: false
        },
        alwaysShowDropzone: {
            type: Boolean,
            default: false
        },
        adaptToContent: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            dropzoneStyle: "none",
            internalDrag: false,
            externalDrag: false
        };
    },
    computed: {
        isDropzoneActive() {
            return this.dropzoneStyle === "active";
        },
        showDropzone() {
            return this.inline
                ? this.isDropzoneActive
                : this.isDropzoneActive || this.dropzoneStyle === "hover" || this.alwaysShowDropzone;
        },
        getClassForDropzone() {
            return {
                "bm-dropzone-show-dropzone": this.isDropzoneActive || this.alwaysShowDropzone,
                "bm-dropzone-active": this.isDropzoneActive,
                "bm-dropzone-hover": this.dropzoneStyle === "hover" && !this.inline
            };
        }
    },
    watch: {
        showDropzone(value) {
            if (this.adaptToContent && value) {
                const height = this.$refs.contentZone.clientHeight;
                if (height > 0) {
                    this.$nextTick(() => (this.$refs.dropZone.style.height = `${height}px`));
                }
            }
        }
    },
    methods: {
        onDragover(event) {
            this.$emit("files-count", this.draggedFilesCount(event));
            if (this.shouldActivateFn(event)) {
                this.externalDrag = true;
                this.dropzoneStyleOnDragover(event);
            }
        },
        onFilesDrop(dropEvent) {
            if (!this.internalDrag) {
                dropEvent.preventDefault();
            }
            if (this.showDropzone && this.externalDrag) {
                this.$emit("drop-files", this.extractFiles(dropEvent));
            }
            this.externalDrag = false;
        },
        containsFilesOrFolders(dragEvent) {
            return dragEvent.dataTransfer.types.includes("Files");
        },
        isFile(dataTransferItem) {
            // detecting a file is not a directory seems faisible only by using DataTransferItem.webkitGetAsEntry
            // /!\ webkitGetAsEntry works only once 'drop' has been done and only for supported browsers
            const entry = dataTransferItem.webkitGetAsEntry();
            return !entry || entry.isFile;
        },
        extractFiles(dropEvent) {
            const files = [];
            if (this.containsFilesOrFolders(dropEvent)) {
                for (let i = 0; i < dropEvent.dataTransfer.items.length; i++) {
                    const item = dropEvent.dataTransfer.items[i];
                    if (this.isFile(item)) {
                        files.push(item.getAsFile());
                    }
                }
            }
            return files;
        },
        dropzoneStyleOnDragover(event) {
            const containsFilesOrFolders = this.containsFilesOrFolders(event);
            if (containsFilesOrFolders) {
                const boundaries = this.$el.getBoundingClientRect();
                const outsideDropZone =
                    event.x < boundaries.x ||
                    event.x > boundaries.x + boundaries.width ||
                    event.y < boundaries.y ||
                    event.y > boundaries.y + boundaries.height;

                outsideDropZone ? (this.dropzoneStyle = "active") : (this.dropzoneStyle = "hover");
            }
        },

        draggedFilesCount(dragEvent) {
            return dragEvent.dataTransfer.items ? dragEvent.dataTransfer.items.length : -1;
        }
    }
};

// detect the drag action leaves the document (cross browsers solution)
let lastTarget;

window.addEventListener("dragenter", function (e) {
    if (e.dataTransfer.types.includes("Files")) {
        lastTarget = e.target;
    }
});

window.addEventListener("dragleave", function (e) {
    e.preventDefault();
    if (e.target === document || e.target === lastTarget) {
        document.dispatchEvent(new Event("dragleavedocument"));
    }
});
</script>
