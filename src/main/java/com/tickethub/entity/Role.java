package com.tickethub.entity;

/**
 * Stored as the enum name (USER, ADMIN), not as 0/1.
 * A new value can be added later without shifting existing rows.
 */
public enum Role {
    USER,
    ADMIN
}
