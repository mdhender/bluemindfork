<script>
import { BTable } from "bootstrap-vue";

const CODE_DOWN = 40;
const CODE_END = 35;
const CODE_HOME = 36;
const CODE_UP = 38;

const arrayIncludes = (array, value) => array.indexOf(value) !== -1;

const onTbodyRowKeydown = BTable.options.methods.onTbodyRowKeydown;
export default {
    name: "BmInternalTable",
    extends: BTable,
    props: {
        tabNav: {
            type: Boolean,
            default: false
        }
    },
    methods: {
        async onTbodyRowKeydown(event) {
            onTbodyRowKeydown.call(this, event);
            const trs = this.getTbodyTrs();
            if (
                (event.key === "Tab" || arrayIncludes([CODE_UP, CODE_DOWN, CODE_HOME, CODE_END], event.keyCode)) &&
                !this.tabNav
            ) {
                await this.$nextTick();
                trs.forEach(tr => {
                    const tabindex = this.hasFocus(tr.__vue__.$el) ? "0" : "-1";
                    tr.__vue__.$el.setAttribute("tabindex", tabindex);
                    tr.__vue__.$el.querySelectorAll("input").forEach(input => input.setAttribute("tabindex", tabindex));
                });
            }
        },
        hasFocus(tr) {
            return tr.contains(document.activeElement);
        }
    }
};
</script>
