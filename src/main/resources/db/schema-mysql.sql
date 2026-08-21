-- MySQL schema for Smart Syrian Public Transportation (XAMPP)
CREATE TABLE IF NOT EXISTS stations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    aliases         VARCHAR(1000),
    station_kind    VARCHAR(32)  NOT NULL DEFAULT 'STOP',
    governorate     VARCHAR(64)  NOT NULL DEFAULT 'DAMASCUS',
    latitude        DOUBLE NOT NULL,
    longitude       DOUBLE NOT NULL
);

CREATE TABLE IF NOT EXISTS routes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    code            VARCHAR(64) UNIQUE,
    vehicle_type    VARCHAR(32) NOT NULL,
    governorate     VARCHAR(64) NOT NULL DEFAULT 'DAMASCUS',
    current_fare    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status          VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    path_polyline   TEXT
);

CREATE TABLE IF NOT EXISTS route_stations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id        BIGINT NOT NULL,
    station_id      BIGINT NOT NULL,
    sequence_no     INT NOT NULL,
    CONSTRAINT uk_route_sequence UNIQUE (route_id, sequence_no),
    CONSTRAINT uk_route_station UNIQUE (route_id, station_id),
    CONSTRAINT fk_route_stations_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_stations_station FOREIGN KEY (station_id) REFERENCES stations (id)
);

CREATE TABLE IF NOT EXISTS crowdsource_reports (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_type     VARCHAR(64) NOT NULL,
    sent_at         DATETIME(6) NOT NULL,
    reporter_id     VARCHAR(128) NOT NULL,
    route_id        BIGINT,
    station_id      BIGINT,
    new_fare        DECIMAL(12, 2),
    description     TEXT,
    applied         TINYINT(1) NOT NULL DEFAULT 0,
    latitude        DOUBLE,
    longitude       DOUBLE,
    CONSTRAINT fk_reports_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE SET NULL,
    CONSTRAINT fk_reports_station FOREIGN KEY (station_id) REFERENCES stations (id) ON DELETE SET NULL
);
