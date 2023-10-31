export default {
    methods: {
        menu() {
            return this.$refs.b_dropdown.$refs.menu;
        },
        show(bvEvent) {
            return this.$refs.b_dropdown.show(bvEvent);
        },
        hide(bvEvent) {
            return this.$refs.b_dropdown.hide(bvEvent);
        }
    }
};
