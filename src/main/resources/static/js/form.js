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
        $(el).addClass('error ms-3');
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
        if ($(group).find('input.answered').length === 0) {
            $(group).find('.field input').each(function() {
                $(this).removeClass('is-invalid');    
            }); 
            pass = true;   
        }
    }

    return pass;
}

function validateField(field) {
    let pass = true;
    let notAnswered = undefined;
    let required = $(field).hasClass('required');
    let message = undefined;

    $(field).find('input[type=number]').each(function() {
        let val = $(this).val().trim();
        let regex = new RegExp('^[1-9][0-9]*$');
        notAnswered = val === '';
        pass = notAnswered ? ! required : regex.test(val);
    });

    $(field).find('input[type=text]').each(function() {
        let valStr = $(this).val().trim();
        notAnswered = valStr === '';
        pass = notAnswered ? ! required : true;
        let minStr = $(this).attr('data-min');
        let maxStr = $(this).attr('data-max');
        if (pass && valStr !== '' && (minStr !== undefined || maxStr !== undefined)) {   // validate as a number if either min and / or max is defined
            let val = parseInt(valStr);
            if (minStr !== undefined) {
                let min = parseInt(minStr);
                if (val < min) {
                    pass = false;
                    message = 'Value must be >= ' + min;
                }
            }
            if (maxStr !== undefined) {
                let max = parseInt(maxStr);
                if (val > max) {
                    pass = false;
                    message = 'Value must be <= ' + max;
                }
            }
        }
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

    $(field).find('input').each(function() {
        if (pass) {
            $(this).removeClass('is-invalid');
        } else {
            $(this).addClass('is-invalid');
        }
    
        let answered = ! notAnswered;   // just because I hate double negatives
    
        if (answered) {
            $(this).addClass('answered');
        } else {
            $(this).removeClass('answered');
        }
    });

    let nextEl = $(field).next();
    if (message !== undefined) {
        if (nextEl.is('div.validationMessage')) {
            $(nextEl).html(message);
        } else {
            $(field).after('<div class="validationMessage">' + message + '</div>');
        }
    } else if (nextEl.is('div.validationMessage')) {
        $(nextEl).remove();
    }

    return pass;
}
