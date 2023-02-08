import { VirtualRoutes, MailRoute, MailtoRoute, PopupRoute } from "./routes";

export default [{ path: "/index.html", redirect: "/mail/" }, PopupRoute, MailtoRoute, MailRoute, ...VirtualRoutes];
