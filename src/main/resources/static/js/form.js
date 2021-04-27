function resetForm(form) {
    $(form).find('input[type=number]').each(function() {
        $(this).val('');
    });

    $(form).find('input[type=text]').each(function() {
        $(this).val('');
    });

    $(form).find('input[type=radio]').each(function() {
        $(this).prop('checked', false);
    })

    $(form).find('select').each(function() {
        $(this).prop('selectedIndex', 0);
    })
}

function validateForm(form, outputContainer) {
    let pass = true;
    $(form).find('.field').each(function() {
        if ( ! validateField(this) ) {
            pass = false;
        }
    });

    let el = $(outputContainer);
    $(el).removeClass();
    if (pass) {
        $(el).addClass('hidden');
        $(el).html('');

    } else {
        $(el).addClass('error');
        $(el).html("Please fill missing fields and try again.");
    }

    return pass;
}

function validateField(field) {
    let pass = true;

    $(field).find('input[type=number]').each(function() {
        if ($(this).val().trim() === '') {
            pass = false;
        }
    });

    $(field).find('input[type=text]').each(function() {
        if ($(this).val().trim() === '') {
            pass = false;
        }
    });

    $(field).find('select').each(function() {
        if ($(this).prop('selectedIndex') === 0) {
            pass = false;
        }
    });

    let radio = $(field).find('input[type=radio]');
    if (radio.length > 0 && $(radio).filter(':checked').length === 0) {
        pass = false;
    }

    if (pass) {
        $(field).removeClass('error');
    } else {
        $(field).addClass('error');
    }

    return pass;
}
