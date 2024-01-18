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
        },

        /** Hack BTable for overriding the "range" selection mode behavior. */
        selectionHandler(item, index, event) {
            // /!\ BM: copy/paste from https://github.com/bootstrap-vue/bootstrap-vue/blob/dev/src/components/table/helpers/mixin-selectable.js
            /* istanbul ignore if: should never happen */
            if (!this.isSelectable || this.noSelectOnClick) {
                // Don't do anything if table is not in selectable mode
                this.clearSelected();
                return;
            }
            const { selectMode, selectedLastRow } = this;
            let selectedRows = this.selectedRows.slice();
            let selected = !selectedRows[index];
            // Note 'multi' mode needs no special event handling
            if (selectMode === "single") {
                selectedRows = [];
            } else if (selectMode === "range") {
                if (selectedLastRow > -1 && event.shiftKey) {
                    // range
                    for (let idx = Math.min(selectedLastRow, index); idx <= Math.max(selectedLastRow, index); idx++) {
                        selectedRows[idx] = true;
                    }
                    selected = true;
                } else {
                    // /!\ BM: de-activate clearing of previous selection with "range" mode
                    // if (!(event.ctrlKey || event.metaKey)) {
                    //     // Clear range selection if any
                    //     selectedRows = [];
                    //     selected = true;
                    // }
                    if (selected) this.selectedLastRow = index;
                }
            }
            selectedRows[index] = selected;
            this.selectedRows = selectedRows;
        }
    }
};
</script>
