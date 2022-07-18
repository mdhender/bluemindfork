export default {
    props: {
        message: {
            type: Object,
            required: true
        },
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        src() {
            return this.file.url;
        }
    }
};
