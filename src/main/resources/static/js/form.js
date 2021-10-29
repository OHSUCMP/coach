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
    $(form).find('.validationGroup').each(function() {
        if ( ! validateGroup(this) ) {
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

function validateGroup(group) {
    let pass = true;
    let required = $(group).hasClass('required');

    $(group).find('.field').each(function() {
        if ( ! validateField(this) ) {
            pass = false;
        }
    });

    if ( ! required && ! pass ) {
        // okay so this group isn't required, but something in it didn't pass validation
        // the only circumstance where this should be reversed is if all fields are blank,
        // i.e. not answered.
        // if the user provided answers to any of the fields, then the group must pass validation

        if ($(group).find('.field.answered').length === 0) {
            pass = true;
        }
    }

    if ( ! pass ) {
        $(group).addClass('failsValidation');
    } else {
        $(group).removeClass('failsValidation');
    }

    return pass;
}

function validateField(field) {
    let pass = true;
    let notAnswered = undefined;
    let required = $(field).hasClass('required');

    $(field).find('input[type=number]').each(function() {
        let val = $(this).val().trim();
        let regex = new RegExp('^[1-9][0-9]*$');
        notAnswered = val === '';
        pass = notAnswered ? ! required : regex.test(val);
    });

    $(field).find('input[type=text]').each(function() {
        let val = $(this).val().trim();
        notAnswered = val === '';
        pass = notAnswered ? ! required : true;
    });

    $(field).find('select').each(function() {
        notAnswered = $(this).prop('selectedIndex') === 0;
        pass = notAnswered ? ! required : true;
    });

    // NOTE : this radio validator probably won't work correctly if there are multiple different radio
    //        groups in the same form, since it's not filtering by name or anything.  should be fine
    //        being simple like this for now, but if we make the forms more complex, this will probably
    //        need to change
    let radio = $(field).find('input[type=radio]');
    if (radio.length > 0) {
        notAnswered = $(radio).filter(':checked').length === 0;
        pass = notAnswered ? ! required : true;
    }

    if (pass) {
        $(field).removeClass('failsValidation');
    } else {
        $(field).addClass('failsValidation');
    }

    let answered = ! notAnswered;   // just because I hate double negatives

    if (answered) {
        $(field).addClass('answered');
    } else {
        $(field).removeClass('answered');
    }

    return pass;
}
