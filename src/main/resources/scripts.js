jQuery(function($) {
    $(".ewp-manifest-reloader").on("click", function() {
        var self = $(this);
        $.ajax({
            url: "/reload",
            data: {
                url: $(this).attr("data-manifest-url")
            },
            method: 'POST'
        }).then(function() {
            var info = $("<span>Reload queued</span>");
            info.hide();
            self.replaceWith(info);
            info.fadeIn();
        }).catch(function() {
            var info = $("<span>Reload FAILED</span>");
            info.hide();
            self.replaceWith(info);
            info.fadeIn();
        });
    });
});
