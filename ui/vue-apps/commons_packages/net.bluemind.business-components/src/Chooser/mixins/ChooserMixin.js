export default {
    data() {
        return {
            loading: false,
            error: null,
            items: []
        };
    },
    methods: {
        async getItems(getItemFn) {
            try {
                this.loading = true;
                this.error = null;
                const items = await getItemFn();
                this.loading = false;
                return items;
            } catch (error) {
                this.error = error;
                this.loading = false;
                return [];
            }
        }
    }
};
