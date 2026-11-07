CREATE TABLE qr_codes_tbl (
                              id UUID PRIMARY KEY,

                              validation_status VARCHAR(50) NOT NULL,
                              qr_code_value TEXT NOT NULL,

                              ticket_id BIGINT NOT NULL UNIQUE,

                              created_at TIMESTAMP NOT NULL,
                              updated_at TIMESTAMP NOT NULL,

                              CONSTRAINT fk_qr_code_ticket
                                  FOREIGN KEY (ticket_id)
                                      REFERENCES tickets_tbl(id)
);