CREATE TABLE status (
    id UUID PRIMARY KEY,
    sykmelding_id UUID NOT NULL,
    org_nr VARCHAR NOT NULL,
    fnr VARCHAR NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR NOT NULL
);
CREATE INDEX idx_status_org_nr_fnr ON status (org_nr, fnr);
CREATE INDEX idx_sykmelding_id ON status(sykmelding_id);