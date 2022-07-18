<script>
export default {
    name: "DecoratedFileItem",
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            size_: null
        };
    },
    computed: {
        decorated() {
            return { ...this.file, size: this.size_ };
        }
    },
    watch: {
        "file.url": {
            async handler() {
                this.size_ = this.file.size;
                if (!this.size_ && this.file.url) {
                    const res = await fetch(this.file.url, { method: "HEAD" });
                    this.size_ = res.headers && res.headers["Content-Length"];
                }
            },
            immediate: true
        }
    },

    render() {
        return this.$scopedSlots.default({ file: this.decorated });
    }
};
</script>
