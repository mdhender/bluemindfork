(function() {
    var toggleIdsList = ["toggle-password"]
    
    if (typeof overrideToggleIdsList !== 'undefined' && overrideToggleIdsList.length > 0) {
        toggleIdsList = overrideToggleIdsList;
    }
    
    var passwordTogglers = []
    for (var i = 0; i < toggleIdsList.length; i++) {
        toggle = document.getElementById(toggleIdsList[i]);
        passwordTogglers[toggle.getAttribute("id")] = new PasswordToggle(toggle);

        toggle.addEventListener("click", function(event) {
            this.toggle = event.target || event.srcElement;
            this.toggler = passwordTogglers[this.toggle.getAttribute("id")];
            
            this.toggler.timeout && clearTimeout(this.toggler.timeout);
            if (this.toggler.isVisible()) {
              this.toggler.hidePassword();
            } else {
              this.toggler.showPassword();
              this.toggler.timeout = setTimeout(hidePassword, 2000, this.toggler);
            }
        });
    }
    
    function hidePassword(toggler) {
        toggler.hidePassword();
    }
})();

function PasswordToggle(toggle) {
    this.toggle = toggle;
    this.passwordField = document.getElementById(this.toggle.getAttribute("toggle"));
    this.timeout;
    
    this.isVisible = function() {
        return this.passwordField.type === "text";
    };

    this.showPassword = function() {
        this.passwordField.type = "text";
        this.toggle.classList.add("fa-eye-slash");
        this.toggle.classList.remove("fa-eye");
    },

    this.hidePassword = function() {
        this.passwordField.type = "password";
        this.toggle.classList.add("fa-eye");
        this.toggle.classList.remove("fa-eye-slash");
    };
}
