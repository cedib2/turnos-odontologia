-- Vínculo entre usuario de staff y profesional, para el ámbito «propio» de los permisos de V001
-- (agenda:leer y ficha_clinica:leer del odontólogo). Un profesional tiene a lo sumo un usuario.
-- Se numera después de V032 para aplicarse en bases que ya estén en V032 sin outOfOrder.
ALTER TABLE usuarios ADD COLUMN profesional_id BIGINT;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_profesional
    FOREIGN KEY (profesional_id) REFERENCES profesionales (id);
CREATE UNIQUE INDEX ux_usuarios_profesional ON usuarios (profesional_id) WHERE profesional_id IS NOT NULL;

-- Con el ámbito «propio» aplicado, el único ficha_clinica:leer del seed (ámbito propio) dejaría al administrador
-- sin acceso a fichas que no son de su profesional. La KB le asigna acceso completo (03_actores_y_roles:
-- Ficha clínica, Administrador C L A E): se agrega ficha_clinica:leer con ámbito «todos» solo para él.
INSERT INTO permisos (recurso, accion, ambito) VALUES ('ficha_clinica', 'leer', 'todos');
INSERT INTO rol_permiso (rol_id, permiso_id)
    SELECT r.id, p.id FROM roles r, permisos p
    WHERE r.codigo = 'administrador' AND p.recurso = 'ficha_clinica' AND p.accion = 'leer' AND p.ambito = 'todos';
