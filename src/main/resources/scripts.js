function replaceWithFade(whatTo, whatWith) {
    $(whatWith).hide();
    $(whatTo).replaceWith(whatWith);
    $(whatWith).fadeIn();
}

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

function swapWithFade(toHide, toShow) {
    $(toHide).hide();
    $(toShow).fadeIn();
}

function validationResultsReceivedCallback(validationResults) {
    var newWindow = window.open()
    newWindow.document.write(validationResults);
}

// Selects first element matching selector from set of parent and its siblings.
function getParentOrItsSibling(element, selector) {
    return $(element).parent().parent().children(selector)[0];
}

function doneClicked() {
    var validate_cell = getParentOrItsSibling(this, ".manifest_validator_validate_cell");
    var done_cell = getParentOrItsSibling(this, ".manifest_validator_done_cell");

    validationResultsReceivedCallback($(this).parent()[0].validationResults);
    swapWithFade(done_cell, validate_cell);
}

function validateClicked() {
    var this_cell = $(this).parent();
    var in_progress_cell = getParentOrItsSibling(this, ".manifest_validator_in_progress_cell");
    var done_cell = getParentOrItsSibling(this, ".manifest_validator_done_cell");
    var error_cell = getParentOrItsSibling(this, ".manifest_validator_error_cell");

    $.ajax({
        url: "/validateApi",
        data: {
            url: $(this).attr("data-api-url"),
            name: $(this).attr("data-api-name"),
            version: $(this).attr("data-api-version"),
            security: $(this).attr("data-api-sec"),
        },
        method: "POST",
        dataType: "text"
    }).done(function(data) {
        done_cell.validationResults = data;
        swapWithFade(in_progress_cell, done_cell);
    }).fail(function(jqXHR, textStatus, errorThrown) {
        swapWithFade(in_progress_cell, error_cell);
    });

    swapWithFade(this_cell, in_progress_cell);
}

jQuery(function($) {
    $(".manifest_validator_validate").on("click", validateClicked);
    $(".manifest_validator_error").on("click", validateClicked);
    $(".manifest_validator_done").on("click", doneClicked);
});

