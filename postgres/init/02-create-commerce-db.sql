DO
$$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'commerce') THEN
        CREATE ROLE commerce LOGIN PASSWORD 'commerce';
    END IF;
END
$$;

SELECT 'CREATE DATABASE commercedb OWNER commerce'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'commercedb')\gexec
