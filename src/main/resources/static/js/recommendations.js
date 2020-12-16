function populateRecommendations(_callback) {
    $.ajax({
        "url": "http://localhost:8082/recommendation/list",
        "type": "GET",
        "dataType": "json",
        "contentType": "application/json; charset=utf-8",
//        "data": buildRulerRequest(fhirServer, bearerToken),
        "success": function (data) {
            populateRecommendationsSelect(data);
            _callback();
        }
    });
}

function populateRecommendationsSelect(arr) {
    let options = "<option value='' selected>-- Select Recommendation --</option>\n";
    let info = '';

    arr.forEach(function (item) {
        let trimmedTitle = item.title;
        if (trimmedTitle.indexOf("Recommendation - ") === 0) {
            trimmedTitle = trimmedTitle.substring(17);
        }

        options += "<option value='" + item.id + "'>" + item.title + "</option>\n";
        info += "<div class='recommendation hidden' data-recommendation-id='" + item.id +
            "'>\n<span class='recommendationTitle'>" + item.title +
            "</span>\n<span class='recommendationDesc'>" + item.description +
            "</span></div>\n";
    });

    $('#recommendationsSelect').html(options);
    $('#recommendationInfo').html(info);
}

function recommendationChanged() {
    let button = $('#executeRecommendationButton');
    let recId = $('#recommendationsSelect').children('option:selected').attr('value');
    if (recId === '') {
        $(button).prop('disabled', true);
        $('div.recommendation').not('.hidden').each(function () {
            $(this).addClass('hidden');
        });

    } else {
        $(button).prop('disabled', false);
        $('div.recommendation').each(function () {
            if ($(this).attr('data-recommendation-id') === recId) {
                $(this).removeClass('hidden');
            } else {
                $(this).addClass('hidden');
            }
        });
    }

    $('#cards').html('');
}

function executeSelectedRecommendation() {
    let fhirServer = getFHIRServer();
    let bearerToken = getFHIRBearerToken();
    let user = getFHIRUser();
    let patient = getFHIRPatient();
    let observations = getFHIRObservations();
    let planId = $('#planSelect').children("option:selected").attr("value");

    let data = buildHTNRulerRequest("55284-4"); // todo: don't hardcode this

    let dataStr = JSON.stringify(data, null, 2);
    console.log(dataStr);

    let button = $('#executeHookButton');

    $('body').css('cursor', 'wait');
    $(button).attr('disabled', true);

    $.ajax({
        "url": CDS_SERVICES_URL + "/" + planId,
        "type": "POST",
        "dataType": "json",
        "contentType": "application/json; charset=utf-8",
        "data": dataStr,
        "success": function (obj) {
            $(button).removeAttr('disabled');
            $('body').css('cursor', 'auto');

            populateCards(obj.cards);
        }
    });
}

async function executeSelectedRecommendation2() {
    let recommendationId = $('#recommendationsSelect').children("option:selected").attr("value");
    let button = $('#executeRecommendationButton');
    let body = $('body');

    $(body).css('cursor', 'wait');
    $(button).attr('disabled', true);

    let formData = new FormData();
    formData.append("id", recommendationId);
    let response = await fetch("/recommendation/execute", {
        method: "POST",
        body: formData
    });

    if (response.status === 200) {
        $(button).removeAttr('disabled');

        $(body).css('cursor', 'auto');
        console.log("Success - received response: " + response.body);

        populateCards(response.body);

    } else {
        $(body).html("Error - received response " + response.status + " status");
    }
}

function populateCards(cards) {
    let html = "";
    cards.forEach(function (card) {
        html += "<div class='card " + card.indicator +
            "'>\n<span class='cardTitle'>" + card.summary +
            "</span>\n<span class='cardDetail'>" + card.detail +
            "</span>\n";

        if (card.source.label !== undefined && card.source.url !== undefined) {
            html += "See: <a href='" + card.source.url + "' target='_blank' rel='noopener noreferrer'>" +
                card.source.label + "</a>\n";
        }

        html += "</div>\n";
    });
    $('#cards').html(html);
}

function genUUID() {
    return "test";
//    import { v4 as uuidv4 } from 'uuid';
//    return uuidv4();
}

function buildHTNRulerRequest(code) {
    let user = getFHIRUser();
    let patient = getFHIRPatient();
    let observations = getFHIRObservations();

    return {
        "hookInstance": genUUID(),
        "fhirServer": getFHIRServer(),
        "hook": "patient-view",
        "applyCql": true,
        "fhirAuthorization": {
            "access_token": getFHIRBearerToken(),
            "token_type": "Bearer",
            "expires_in": 300,
            "scope": "patient/Patient.read patient/Observation.read",
            "subject": "cds-service4"
        },
        "context": {
            "userId": "Practitioner/" + user.id,
            "patientId": "Patient/" + patient.id,
            "code": "http://loinc.org|" + code
        },
        "prefetch": {
            "item1": {
                "response": {
                    "status": "200 OK"
                },
                "resource": patient
            },
            "item2": {
                "response": {
                    "status": "200 OK"
                },
                "resource": {
                    "resourceType": "Bundle",
                    "id": "ohsu-htn-u18-observations-bundle",
                    "type": "collection",
                    "entry": toBundle(getFHIRObservations())
                }
            }
        }
    };
}

function toBundle(arr) {
    let newArr = arr.map(item => {
        let r = {};
        r.resource = item;
        return r;
    });
    return newArr;
}


// see https://cds-hooks.hl7.org/1.0/#http-request_1 for details
function buildOpioidRulerRequest(fhirServer, bearerToken, patientId) {
    // opioidJson taken from http://build.fhir.org/ig/cqframework/opioid-cds/requests/request-example-rec-10-patient-view-illicit-drugs.json
    // see http://build.fhir.org/ig/cqframework/opioid-cds/quick-start.html section 8.3.1
    return {
        "hookInstance": "31c74cfc-747c-4afc-82e4-bdd3b7a0a58c",
        "fhirServer": "http://localhost:8080/cqf-ruler-stu3/fhir",
        "hook": "patient-view",
        "applyCql": true,
        "context": {
            "user": "Practitioner/example",
            "patientId": "Patient/example-rec-10-illicit-drugs",
            "encounterId": "Encounter/example-rec-10-illicit-drugs-context"
        },
        "prefetch": {
            "item1": {
                "response": {
                    "status": "200 OK"
                },
                "resource": {
                    "resourceType": "Patient",
                    "id": "example-rec-10-illicit-drugs",
                    "gender": "female",
                    "birthDate": "1982-01-07",
                    "name": [
                        {
                            "family": "Smith",
                            "given": [
                                "John",
                                "A."
                            ]
                        }
                    ]
                }
            },
            "item2": {
                "response": {
                    "status": "200 OK"
                },
                "resource": {
                    "resourceType": "Observation",
                    "id": "example-rec-10-illicit-drugs-prefetch",
                    "status": "final",
                    "code": {
                        "coding": [
                            {
                                "system": "http://loinc.org",
                                "code": "3426-4",
                                "display": "Tetrahydrocannabinol [Presence] in Urine"
                            }
                        ]
                    },
                    "subject": {
                        "reference": "Patient/example-rec-10-illicit-drugs"
                    },
                    "_effectiveDateTime": {
                        "extension": [
                            {
                                "url": "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression",
                                "valueString": "Today() - 28 days"
                            }
                        ]
                    },
                    "interpretation": {
                        "coding": [
                            {
                                "system": "http://hl7.org/fhir/v2/0078",
                                "version": "v2.8.2",
                                "code": "POS",
                                "display": "Tetrahydrocannabinol [Presence] in Urine"
                            }
                        ]
                    }
                }
            },
            "item3": null,
            "item4": {
                "response": {
                    "status": "200 OK"
                },
                "resource": {
                    "resourceType": "Bundle",
                    "entry": [
                        {
                            "resource": {
                                "resourceType": "MedicationRequest",
                                "id": "example-rec-10-illicit-drugs-prefetch",
                                "status": "active",
                                "intent": "order",
                                "category": {
                                    "coding": [
                                        {
                                            "system": "http://hl7.org/fhir/medication-request-category",
                                            "code": "community"
                                        }
                                    ]
                                },
                                "medicationCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                                            "code": "1049502",
                                            "display": "12 HR Oxycodone Hydrochloride 10 MG Extended Release Oral Tablet"
                                        }
                                    ]
                                },
                                "subject": {
                                    "reference": "Patient/example-rec-10-illicit-drugs"
                                },
                                "context": {
                                    "reference": "Encounter/example-rec-10-illicit-drugs-prefetch"
                                },
                                "_authoredOn": {
                                    "extension": [
                                        {
                                            "url": "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression",
                                            "valueString": "Today() - 90 days"
                                        }
                                    ]
                                },
                                "dosageInstruction": [
                                    {
                                        "timing": {
                                            "repeat": {
                                                "frequency": 3,
                                                "period": 1.0,
                                                "periodUnit": "d"
                                            }
                                        },
                                        "asNeededBoolean": false,
                                        "doseQuantity": {
                                            "value": 1.0,
                                            "unit": "tablet"
                                        }
                                    }
                                ],
                                "dispenseRequest": {
                                    "validityPeriod": {
                                        "extension": [
                                            {
                                                "url": "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression",
                                                "valueString": "FHIR.Period { start: FHIR.dateTime { value: Today() - 90 days }, end: FHIR.dateTime { value: Today() } }"
                                            }
                                        ]
                                    },
                                    "numberOfRepeatsAllowed": 3,
                                    "expectedSupplyDuration": {
                                        "value": 30.0,
                                        "unit": "d"
                                    }
                                }
                            }
                        },
                        {
                            "resource": {
                                "resourceType": "MedicationRequest",
                                "id": "example-rec-10-illicit-drugs-context",
                                "status": "active",
                                "intent": "order",
                                "category": {
                                    "coding": [
                                        {
                                            "system": "http://hl7.org/fhir/medication-request-category",
                                            "code": "community"
                                        }
                                    ]
                                },
                                "medicationCodeableConcept": {
                                    "coding": [
                                        {
                                            "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                                            "code": "197696",
                                            "display": "72 HR Fentanyl 0.075 MG/HR Transdermal System"
                                        }
                                    ]
                                },
                                "subject": {
                                    "reference": "Patient/example-rec-10-illicit-drugs"
                                },
                                "context": {
                                    "reference": "Encounter/example-rec-10-illicit-drugs-context"
                                },
                                "_authoredOn": {
                                    "extension": [
                                        {
                                            "url": "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression",
                                            "valueString": "Today()"
                                        }
                                    ]
                                },
                                "dosageInstruction": [
                                    {
                                        "timing": {
                                            "repeat": {
                                                "frequency": 1,
                                                "period": 12.0,
                                                "periodUnit": "d"
                                            }
                                        },
                                        "asNeededBoolean": false,
                                        "doseQuantity": {
                                            "value": 1.0,
                                            "unit": "patch"
                                        }
                                    }
                                ],
                                "dispenseRequest": {
                                    "validityPeriod": {
                                        "extension": [
                                            {
                                                "url": "http://hl7.org/fhir/StructureDefinition/cqif-cqlExpression",
                                                "valueString": "FHIR.Period { start: FHIR.dateTime { value: Today() }, end: FHIR.dateTime { value: Today() + 3 months } }"
                                            }
                                        ]
                                    },
                                    "numberOfRepeatsAllowed": 3,
                                    "expectedSupplyDuration": {
                                        "value": 30.0,
                                        "unit": "d"
                                    }
                                }
                            }
                        }
                    ]
                }
            }
        }
    };
}