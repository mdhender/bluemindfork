import store from "@bluemind/store";
import { chooserStore } from "./store/store";
export { default as ChooserModal } from "./ChooserModal";

store.registerModule("chooser", chooserStore);
