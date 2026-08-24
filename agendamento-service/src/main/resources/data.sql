-- INSERT IGNORE porque o data.sql roda a CADA start da aplicacao.
-- Sem isso, o segundo start estoura violacao de unicidade.

INSERT IGNORE INTO paciente (id, nome, telefone, email, criado_em, atualizado_em)
VALUES (1, 'Maria Silva', '91999998888', 'maria@exemplo.com', NOW(6), NOW(6)),
       (2, 'Joao Souza',  '91988887777', NULL,                NOW(6), NOW(6));

INSERT IGNORE INTO profissional (id, nome, cargo, especialidade, registro_conselho, criado_em, atualizado_em)
VALUES (1, 'Dra. Helena Prado', 'MEDICO',     'Cardiologia', 'CRM-PA 12345',   NOW(6), NOW(6)),
       (2, 'Carlos Nogueira',   'ENFERMEIRO', NULL,          'COREN-PA 54321', NOW(6), NOW(6));

-- Senha de todos: 123456
INSERT IGNORE INTO usuario (id, login, senha, role, paciente_id, profissional_id, criado_em, atualizado_em)
VALUES (1, 'medico',     '$2b$10$x/nbAA6OAda6c74Ux7JcReHvDFt22ZzQH5ehPaMiiWcJeTktARADC', 'MEDICO',     NULL, 1,    NOW(6), NOW(6)),
       (2, 'enfermeiro', '$2b$10$x/nbAA6OAda6c74Ux7JcReHvDFt22ZzQH5ehPaMiiWcJeTktARADC', 'ENFERMEIRO', NULL, 2,    NOW(6), NOW(6)),
       (3, 'paciente',   '$2b$10$x/nbAA6OAda6c74Ux7JcReHvDFt22ZzQH5ehPaMiiWcJeTktARADC', 'PACIENTE',   1,    NULL, NOW(6), NOW(6)),
       (4, 'paciente2',  '$2b$10$x/nbAA6OAda6c74Ux7JcReHvDFt22ZzQH5ehPaMiiWcJeTktARADC', 'PACIENTE',   2,    NULL, NOW(6), NOW(6));

INSERT IGNORE INTO procedimento (id, nome, duracao_minutos)
VALUES (1, 'Consulta cardiologica', 30),
       (2, 'Consulta de rotina',    20);

-- 'Particular' e 'Cartao desconto' sao linhas normais aqui: nao existe
-- agendamento sem convenio, entao sem estas duas nao se agenda nada.
INSERT IGNORE INTO convenio (id, nome)
VALUES (1, 'Particular'),
       (2, 'Cartao desconto'),
       (3, 'Unimed'),
       (4, 'SulAmerica');