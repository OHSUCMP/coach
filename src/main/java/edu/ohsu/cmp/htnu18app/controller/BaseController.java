package edu.ohsu.cmp.htnu18app.controller;

import org.springframework.beans.factory.annotation.Value;

public abstract class BaseController {
    @Value("${application.name}")
    protected String applicationName;
}
