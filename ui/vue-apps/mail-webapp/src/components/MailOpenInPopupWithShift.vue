<script>
import MailOpenInPopup from "./MailOpenInPopup.vue";
export default {
    name: "MailOpenInPopupWithShift",
    extends: MailOpenInPopup,

    data() {
        return { shift: false, hover: false, listeners: [] };
    },
    computed: {
        withShiftOnly() {
            return this.$store.state.settings.mail_compose_in_new_window !== "true";
        },
        active() {
            return this.enabled && (this.preference || (this.shift && this.hover));
        }
    },
    mounted() {
        document.addEventListener("keyup", this.activate);
        document.addEventListener("keydown", this.activate);
        this.$el?.addEventListener("mouseover", this.activate);
        this.$el?.addEventListener("mouseleave", this.activate);
    },
    beforeDestroy() {
        document.removeEventListener("keyup", this.activate);
        document.removeEventListener("keydown", this.activate);
        this.$el?.removeEventListener("mouseover", this.activate);
        this.$el?.removeEventListener("mouseleave", this.activate);
    },
    methods: {
        activate(event) {
            this.shift = event.shiftKey;
            if (event.type === "mouseover") {
                this.hover = true;
            } else if (event.type === "mouseleave") {
                this.hover = false;
            }
        }
    },
    render() {
        return this.$scopedSlots.default({
            execute: (fallback, event) => {
                const active = !this.withShiftOnly || (event ? event.shiftKey : this.shift);
                this.enabled && active ? this.open() : fallback();
            },
            icon: fallback => (this.enabled && this.withShiftOnly && this.shift && this.hover ? "popup" : fallback),
            label: label => {
                if (!this.enabled || !this.withShiftOnly) {
                    return label;
                }
                if (label?.trim()) {
                    return this.$t("common.open_action_in_window_with_shift", { action: label });
                }
                return this.$t("common.open_in_window_with_shift");
            },
            enabled: this.enabled,
            active: this.active
        });
    }
};
</script>
