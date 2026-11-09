-- A Keycloak identity must not be linked to multiple Evently users.
CREATE UNIQUE INDEX uk_users_tbl_keycloak_id
    ON users_tbl(keycloak_id);