package com.facochico.edu.domain.user;

import lombok.Value;

@Value
public class User {
    private final long id;
    private final UserDetails details;
}
