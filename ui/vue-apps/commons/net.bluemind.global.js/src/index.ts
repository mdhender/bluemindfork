type WebAppKey = string;
type WebAppValue = any;

type WebApp = Record<WebAppKey, WebAppValue>;

declare global {
    interface Window {
        WebApp: WebApp;
    }
}

export default self.WebApp || (self.WebApp = {});
