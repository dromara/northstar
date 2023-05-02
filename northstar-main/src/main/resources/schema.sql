CREATE TABLE SIM_ACCOUNT (
    gatewayId VARCHAR(100) PRIMARY KEY,
    dataStr CLOB
);

CREATE TABLE BAR (
    id INT PRIMARY KEY,
    unifiedSymbol VARCHAR(100) NOT NULL,
	tradingDay VARCHAR(20) NOT NULL,
    expiredAt BIGINT,
    barData BLOB
);