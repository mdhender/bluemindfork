import registerDependencies from "./registerDependencies";
registerDependencies();
import { Chooser } from "./Chooser/index.js";
import Contact from "./Contact";
import ContactActionShow from "./actions/ContactActionShow";
import ContactInput from "./ContactInput";
export { Contact, ContactActionShow, ContactInput, Chooser };
