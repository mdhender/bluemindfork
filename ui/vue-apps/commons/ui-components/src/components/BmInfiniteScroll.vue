<template>
    <div class="bm-infinite-scroll">
        <div
            :ref="makeUniq('scroller-y')"
            class="scroller-y"
            :class="{ 'scrollbar-hidden': !_scrollbar }"
            @scroll="onScroll"
            @keydown.prevent
        >
            <div :style="{ height: height + 'px' }" />
            <div :ref="makeUniq('items')" class="d-flex flex-column items" :style="{ top: top + 'px' }">
                <div
                    v-for="item in _items"
                    :key="item[itemKey]"
                    :style="{ order: item.order }"
                    :data-list-position="item.position"
                >
                    <slot v-if="!item.loaded" name="loading"> ... </slot>
                    <slot v-else :item="item.value" name="item">
                        <div>{{ item.value }}</div>
                    </slot>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
const LIST_SIZE = 20;

import MakeUniq from "../mixins/MakeUniq";
import throttle from "lodash.throttle";

export default {
    name: "BmInfiniteScroll",
    mixins: [MakeUniq],
    props: {
        items: {
            type: Array,
            required: true
        },
        total: {
            type: Number,
            required: false,
            default: Infinity
        },
        scrollbar: {
            type: Boolean,
            required: false,
            default: false
        },
        itemKey: {
            type: String,
            required: false,
            default: ""
        },
        itemSize: {
            type: String,
            requeried: false,
            default: "static"
        }
    },
    data() {
        let strategy;
        if (parseInt(this.itemSize) > 0) {
            strategy = new FixedSizeStrategy(this, parseInt(this.itemSize));
        } else if (this.itemSize === "dynamic") {
            strategy = new DynamicSizeStrategy(this);
        } else {
            strategy = new StaticSizeStrategy(this);
        }
        return {
            offset: 0,
            position: 0,
            top: 0,
            height: 0,
            strategy: strategy
        };
    },
    computed: {
        _scrollbar() {
            return this.total !== Infinity && this.scrollbar;
        },
        _items() {
            const size = Math.min(LIST_SIZE, this.total - this.position);
            return new Array(size).fill(undefined).map((unused, index) => {
                const order = (((index - this.position) % size) + size) % size;
                const item = {};
                item.value = this.items[order + this.position];
                item.loaded = item.value !== undefined;
                item.order = order;
                item.position = this.position + order;
                if (item.loaded && this.itemKey) {
                    item[this.itemKey] = item.value[this.itemKey];
                }
                return item;
            });
        }
    },
    watch: {
        offset(offset) {
            this.position = this.strategy.getPosition(offset);
            this.top = this.strategy.getOffset(this.position);
            this.$nextTick(() => {
                this.height = this.strategy.getOffset(this.total - 1) + this.$el.offsetHeight;
            });
        },
        position() {
            this.$emit("scroll", { start: this.position, end: this.position + LIST_SIZE });
        },
        height(value, old) {
            if (old !== 0) {
                const scroller = this.$refs[this.makeUniq("scroller-y")];
                scroller.scrollTop = (scroller.scrollTop / old) * value;
                this.scroll();
            }
        },
        total() {
            if (this.total < this.position) {
                this.position = this.total;
            }
        }
    },
    mounted() {
        this.strategy.initialize();
        this.height = this.strategy.getOffset(this.total - 1) + this.$el.offsetHeight;
        this.$refs[this.makeUniq("scroller-y")].firstElementChild.style.height = this.height + "px";
    },
    methods: {
        goto(value, forceTop) {
            const offset = this.strategy.getOffset(value);
            const lineHeight = this.strategy.getLineHeight();
            const marginBottom = 2.5 * lineHeight;
            if (forceTop || offset < this.offset || this._items.findIndex(item => item.position === value) < 0) {
                this.$refs[this.makeUniq("scroller-y")].scrollTop = offset;
            } else if (offset + marginBottom >= this.$el.offsetHeight + this.offset) {
                this.$refs[this.makeUniq("scroller-y")].scrollTop = offset - this.$el.offsetHeight + marginBottom;
            }
        },
        onScroll: throttle(function () {
            this.scroll();
        }, 25),
        scroll() {
            const offset = this.$refs[this.makeUniq("scroller-y")].scrollTop;
            this.offset = Math.min(Math.max(0, offset), this.strategy.getOffset(this.total - 1));
        }
    }
};

class StaticSizeStrategy {
    constructor(vm) {
        this.vm = vm;
    }

    initialize() {
        this.lineHeight = this.getLineHeight();
    }

    getLineHeight() {
        const first = this.vm._items.findIndex(item => item.loaded);
        if (first >= 0) {
            const item = this.vm.$refs[this.vm.makeUniq("items")].children[first];
            this.lineHeight = item.offsetHeight;
        }
        return this.lineHeight;
    }

    getOffset(position) {
        return position * this.getLineHeight();
    }

    getPosition(offset) {
        return Math.floor(offset / this.getLineHeight());
    }
}

class FixedSizeStrategy extends StaticSizeStrategy {
    constructor(vm, size) {
        super(vm);
        this.lineHeight = size;
    }

    getLineHeight() {
        return this.lineHeight;
    }
}

class DynamicSizeStrategy {
    constructor(vm) {
        this.vm = vm;
    }

    initialize() {}

    getLineHeight() {
        const items = this.vm.$refs[this.vm.makeUniq("items")];
        return items.offsetHeight / items.childElementCount;
    }

    getOffset(position) {
        if (position >= this.vm.position && position <= this.vm.position + LIST_SIZE) {
            return this.getRealOffset(position);
        }
        return this.guessOffset(position);
    }

    getRealOffset(position) {
        return this.guessOffset(position);
    }

    guessOffset(position) {
        return Math.ceil(position * this.getLineHeight());
    }

    getPosition(offset) {
        const items = this.vm.$refs[this.vm.makeUniq("items")];
        if (offset >= items.offsetTop && offset <= items.offsetTop + items.offsetHeight) {
            return this.getRealPosition(offset);
        } else {
            return this.guessPositon(offset);
        }
    }

    getRealPosition(offset) {
        return this.guessPositon(offset);
    }

    guessPositon(offset) {
        return Math.floor(offset / this.getLineHeight());
    }
}
</script>
<style scoped>
.bm-infinite-scroll {
    overflow: hidden;
    position: relative;
}

.bm-infinite-scroll .scroller-y {
    position: absolute;
    width: 100%;
    height: 100%;
}

.bm-infinite-scroll .scrollbar-hidden {
    box-sizing: content-box;
    padding-right: 17px;
}

.bm-infinite-scroll .items {
    position: absolute;
    width: 100%;
}
</style>
