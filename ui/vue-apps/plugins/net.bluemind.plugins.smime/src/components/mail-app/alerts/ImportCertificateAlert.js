export default {
    watch: {
        "$store.mail.preview.fileKey": {
            handler() {
                console.error(this.$store.mail.files[this.$store.mail.preview.fileKey]);
            }
        }
    }
};
