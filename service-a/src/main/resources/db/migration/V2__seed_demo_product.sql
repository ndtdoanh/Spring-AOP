INSERT INTO products (id, name, description, price_cents, version)
VALUES (1, 'Demo keyboard', 'Mechanical, hot-swappable', 1299900, 0);

SELECT setval(pg_get_serial_sequence('products', 'id'), (SELECT MAX(id) FROM products));
