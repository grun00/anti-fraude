-- As contas da lista negra para a demo. MERGE: rodar de novo não duplica.
MERGE INTO lista_negra (conta, motivo) KEY (conta) VALUES ('6666-666', 'conta laranja denunciada');
MERGE INTO lista_negra (conta, motivo) KEY (conta) VALUES ('9999-999', 'golpe do falso boleto');
