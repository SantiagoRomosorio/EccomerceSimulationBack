INSERT INTO categories (id, name, slug, description, active, created_at, updated_at)
VALUES
    ('10000000-0000-4000-8000-000000000001', 'Tecnología', 'demo-tecnologia', 'Audio y dispositivos para el día a día.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-4000-8000-000000000002', 'Accesorios', 'demo-accesorios', 'Complementos prácticos para acompañarte.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-4000-8000-000000000003', 'Deporte', 'demo-deporte', 'Productos cómodos para mantenerte en movimiento.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-4000-8000-000000000004', 'Hogar', 'demo-hogar', 'Objetos funcionales para tus espacios.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO brands (id, name, slug, description, active, created_at, updated_at)
VALUES
    ('20000000-0000-4000-8000-000000000001', 'Plenora Move', 'demo-plenora-move', 'Diseño cómodo para una vida en movimiento.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-4000-8000-000000000002', 'Plenora Sound', 'demo-plenora-sound', 'Audio personal con una estética sencilla.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('20000000-0000-4000-8000-000000000003', 'Plenora Living', 'demo-plenora-living', 'Detalles útiles para disfrutar cada espacio.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;

WITH demo_products (
    id,
    sku,
    name,
    description,
    price,
    currency,
    category_slug,
    brand_slug,
    stock_quantity
) AS (
    VALUES
        ('30000000-0000-4000-8000-000000000001'::uuid, 'PLENORA-BAG-NOMADA-01', 'Mochila urbana Nómada', 'Organización ligera y espacio acolchado para acompañarte todos los días.', 149900.00, 'COP', 'demo-accesorios', 'demo-plenora-move', 18),
        ('30000000-0000-4000-8000-000000000002'::uuid, 'PLENORA-AUDIO-AURA-01', 'Audífonos inalámbricos Aura', 'Sonido envolvente, diseño cómodo y libertad sin cables.', 219900.00, 'COP', 'demo-tecnologia', 'demo-plenora-sound', 12),
        ('30000000-0000-4000-8000-000000000003'::uuid, 'PLENORA-SHOES-IMPULSO-01', 'Tenis deportivos Impulso', 'Amortiguación ligera y tejido transpirable para moverte con comodidad.', 279900.00, 'COP', 'demo-deporte', 'demo-plenora-move', 9),
        ('30000000-0000-4000-8000-000000000004'::uuid, 'PLENORA-LAMP-HALO-01', 'Lámpara de escritorio Halo', 'Luz cálida regulable y diseño flexible para estudiar o trabajar.', 129900.00, 'COP', 'demo-hogar', 'demo-plenora-living', 7),
        ('30000000-0000-4000-8000-000000000005'::uuid, 'PLENORA-BOTTLE-BRISA-01', 'Botella térmica Brisa', 'Mantiene tus bebidas a la temperatura ideal en un formato fácil de llevar.', 69900.00, 'COP', 'demo-accesorios', 'demo-plenora-living', 24)
)
INSERT INTO products (
    id,
    sku,
    name,
    description,
    price,
    currency,
    category_id,
    category_name,
    brand_id,
    brand_name,
    stock_quantity,
    active,
    created_at,
    updated_at
)
SELECT
    demo.id,
    demo.sku,
    demo.name,
    demo.description,
    demo.price,
    demo.currency,
    category.id,
    category.name,
    brand.id,
    brand.name,
    demo.stock_quantity,
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM demo_products demo
JOIN categories category ON category.slug = demo.category_slug
JOIN brands brand ON brand.slug = demo.brand_slug
ON CONFLICT (sku) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    price = EXCLUDED.price,
    currency = EXCLUDED.currency,
    category_id = EXCLUDED.category_id,
    category_name = EXCLUDED.category_name,
    brand_id = EXCLUDED.brand_id,
    brand_name = EXCLUDED.brand_name,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;
