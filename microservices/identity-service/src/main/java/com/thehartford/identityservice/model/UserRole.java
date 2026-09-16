package com.thehartford.identityservice.model;

/**
 * Roles supported by the LeaseBond Insurance platform.
 * Embedded in the JWT claim "role".
 */
public enum UserRole {
    PROPERTY_OWNER,
    UNDERWRITER,
    CLAIMS_OFFICER,
    ADMIN
}
