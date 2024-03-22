export function useCookieSession(name, value) {
    if (getSessionCookie(name) === null) {
        setSessionCookie(value);
    }

    function setSessionCookie(newValue) {
        value = newValue;
        document.cookie = `${name}=${JSON.stringify(newValue)}; path=/`;
    }

    function getSessionCookie() {
        const cookies = document.cookie.split(";");
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i].trim();
            if (cookie.startsWith(`${name}=`)) {
                const cookieValue = cookie.substring(name.length + 1);
                console.log(cookieValue);
                if (cookieValue === "undefined") {
                    return null;
                }
                return JSON.parse(cookieValue);
            }
        }
        return null;
    }
    return { setValue: setSessionCookie, getValue: getSessionCookie };
}
