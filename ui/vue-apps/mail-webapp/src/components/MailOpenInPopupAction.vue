<script>
export default {
    name: "MailOpenInPopupAction",
    props: {
        width: {
            type: Number,
            default: 1100
        },
        height: {
            type: Number,
            default: 800
        },
        name: {
            type: String,
            default: ""
        },
        href: {
            type: [String, Object],
            required: true
        },
        next: {
            type: [String, Object],
            default: undefined
        }
    },
    methods: {
        open() {
            const route = this.$router.resolve(this.href);
            const popup = window.open(route.href, this.name, getWindowFeature(this.height, this.width));
            popup.focus();
            if (this.next) {
                this.$router.push(this.next);
            }
        }
    },
    render() {
        if (!this.$store.state.mail.isPopup) {
            return this.$scopedSlots.default({
                execute: this.open,
                icon: "popup",
                label: this.$t("common.open_in_window")
            });
        }
        return null;
    }
};
function getWindowFeature(height, width) {
    const left = (screen.availWidth - width) / 2 + window.screenLeft;
    const top = (screen.availHeight - height) / 2 + window.screenTop;
    return `popup=true,width=${width},height=${height},top=${top},left=${left}`;
}
</script>
