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
    var newWindow = window.open();
    newWindow.document.write(validationResults);
    newWindow.document.close();
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

function showAllDetails() {
    $("details").attr("open", "open");
}

function hideAllDetails() {
    $("details").removeAttr("open");
}

$(function($) {
    $("#show_all_details").on("click", showAllDetails);
    $("#hide_all_details").on("click", hideAllDetails);
})

function createXMLValidationLine(line_number, line, message, index) {
    var error = $("<span>");

    var error_message = $("<h4>");
    error_message.text("(#" + (index + 1) + ") " + message);

    var line_pre = $("<pre>");
    line_pre.addClass("ewp-manifest-validator-error-box");

    var line_number_span = $("<span>");
    line_number_span.text(line_number + ". ");
    line_number_span.addClass("ewp-manifest-validator-line-number");

    var line_span = $("<span>");
    line_span.text(line);
    line_span.addClass("ewp-manifest-validator-line");

    line_pre.append(line_number_span);
    line_pre.append(line_span);

    error.append(error_message);
    error.append(line_pre);
    return error;
}

function showXMLValidationOK(data) {
    $("#result_ok").show();
    $("#result_error").hide();
    $("#errors").hide();
    $("#correct").hide();
    $("#server_error").hide();
    $("#correct_element_name").text(data.rootLocalName);
    $("#correct_namespace").text(data.rootNamespaceUri);
    $("#correct").show();
}

function showXMLValidationFailed(data) {
    $("#result_ok").hide();
    $("#result_error").show();
    $("#errors").show();
    $("#correct").hide();

    for (var i = 0; i < data.errors.length; i++) {
        var line_number = data.errors[i].lineNumber;
        var line = data.prettyLines[line_number - 1];
        var message = data.errors[i].message;
        $("#errors").append(createXMLValidationLine(line_number, line, message, i));
    }
}

$(function() {
    $("#validate_button").on("click", function() {
        $.post("/validate", $("#xml_form").serialize())
         .done(function(data) {
            $("#errors").empty();
            $("#server_error").hide();

            if (data.isValid) {
                showXMLValidationOK(data);
            } else {
                showXMLValidationFailed(data);
            }
         })
         .fail(function(data) {
            $("#errors").empty();
            $("#server_error").show();
            $("#result_error").show();
            $("#result_ok").hide();
          })
          .always(function() {
            $("#result_section").show();
            window.location.hash = "#result_section";
          })
    });
})
