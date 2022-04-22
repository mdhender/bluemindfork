<script>
import PrefReadMoreLabel from "./PrefReadMoreLabel";
export default {
    name: "PrefFieldLabel",
    components: { PrefReadMoreLabel },
    props: {
        label: {
            type: [Object, String],
            required: true
        },
        tag: {
            type: String,
            default: "span"
        }
    },
    computed: {
        isComponent() {
            return typeof this.label === "object";
        }
    },
    render(h) {
        if (this.isComponent) {
            return h(this.label.component, { props: this.label.options });
        }
        return h(this.tag, this.label);
    }
};
</script>
