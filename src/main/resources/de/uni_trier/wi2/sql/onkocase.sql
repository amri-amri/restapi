DROP TABLE IF EXISTS trace;
DROP TABLE IF EXISTS log;
DROP TABLE IF EXISTS metadata;
DROP TABLE IF EXISTS metadataType;
DROP TABLE IF EXISTS metadata_belongsTo_trace;
DROP TABLE IF EXISTS metadata_belongsTo_log;
DROP TABLE IF EXISTS metadata_hasType;


CREATE TABLE log (
    logID VARCHAR(36) NOT NULL,
    header MEDIUMTEXT NOT NULL,
    removed BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (logID)
);

CREATE TABLE trace (
    traceID VARCHAR(36) NOT NULL,
    logID VARCHAR(36) REFERENCES log.logID,
    xes MEDIUMTEXT NOT NULL,
    removed BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (traceID)
);

CREATE TABLE metadata (
    metadataID int NOT NULL AUTO_INCREMENT,
    value TEXT NOT NULL,
    PRIMARY KEY (metadataID)
);

CREATE TABLE metadataType (
    metadataTypeID int NOT NULL AUTO_INCREMENT,
    name TEXT NOT NULL,
    PRIMARY KEY (metadataTypeID)
);

CREATE TABLE metadata_belongsTo_log (
    metadataID int NOT NULL REFERENCES metadata.metadataID,
    logID VARCHAR(36) NOT NULL REFERENCES log.logID
);

CREATE TABLE metadata_belongsTo_trace (
    metadataID int NOT NULL REFERENCES metadata.metadataID,
    traceID VARCHAR(36) NOT NULL REFERENCES trace.traceID
);

CREATE TABLE metadata_hasType (
    metadataID int NOT NULL REFERENCES metadata.metadataID,
    metadataTypeID int NOT NULL REFERENCES metadataType.metadataTypeID
);

