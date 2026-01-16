-- ============================================
-- SCHEMA INICIAL PARA NOTIFICATION SERVICE
-- Compatível com entidades JPA + YAML config
-- ============================================

-- Desabilite constraints temporariamente
SET CONSTRAINTS ALL DEFERRED;

-- 1. TABELA users_cache (entidade User - PanacheEntityBase)
CREATE TABLE IF NOT EXISTS users_cache (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    phone VARCHAR(50),
    roles_json JSONB,
    last_sync TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. TABELA notifications (entidade Notification - PanacheEntity)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    channel VARCHAR(255) NOT NULL,
    payload_json JSONB,
    status VARCHAR(255) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE
);

-- 3. TABELA projects (para RecipientResolverServiceTest)
CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    owner_name VARCHAR(255)
);

-- 4. TABELA notification_templates (opcional)
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    body_html TEXT,
    body_text TEXT,
    variables_json JSONB DEFAULT '[]',
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- ÍNDICES
-- ============================================
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notifications_event_type ON notifications(event_type);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);

CREATE INDEX IF NOT EXISTS idx_users_cache_email ON users_cache(email);
CREATE INDEX IF NOT EXISTS idx_users_cache_last_sync ON users_cache(last_sync);

-- ============================================
-- DADOS INICIAIS (APENAS PARA DESENVOLVIMENTO)
-- ============================================
INSERT INTO users_cache (id, email, name, phone, roles_json, last_sync, created_at) 
VALUES 
('user-001', 'admin@apporte.com', 'Administrador', '+5511999999999', '["admin", "user"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user-002', 'user@apporte.com', 'Usuário Normal', '+5511888888888', '["user"]', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

INSERT INTO projects (id, owner_id, owner_email, owner_name) 
VALUES 
('proj-001', 'user-001', 'admin@apporte.com', 'Administrador'),
('proj-123', 'user-002', 'user@apporte.com', 'Usuário Teste')
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- RESETA SEQUENCES (importante após inserts manuais)
-- ============================================
SELECT setval('notifications_id_seq', COALESCE((SELECT MAX(id) FROM notifications), 1), true);
SELECT setval('notification_templates_id_seq', COALESCE((SELECT MAX(id) FROM notification_templates), 1), true);