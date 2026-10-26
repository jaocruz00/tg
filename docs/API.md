# Contrato HTTP — TG

Todos os endpoints, exceto `GET /api/health` e `POST /api/auth/login`, exigem `Authorization: Bearer <token>`.
Erros têm `message`. Principais códigos: 400 validação, 401 sessão, 403 perfil, 404 ausente, 409 conflito, 413 arquivo grande, 502 Ollama.

| Método e rota | Entrada / finalidade | Perfil |
|---|---|---|
| POST /api/auth/login | `{ "email": "...", "senha": "..." }`; retorna token, expiraEm, id, nome, perfil | Público |
| GET /api/auth/me | Usuário atual sem senha | Ambos |
| POST /api/auth/logout | Revoga token atual | Ambos |
| PUT /api/auth/senha | `{ "atual": "...", "nova": "..." }`; revoga todas as sessões | Ambos |
| GET /api/clientes | Array de clientes | Ambos |
| GET /api/clientes/{id} | Cliente | Ambos |
| GET /api/clientes/cpf/{cpf} | CPF com ou sem máscara | Ambos |
| POST /api/clientes | ClienteDTO, nome e CPF obrigatórios | Ambos |
| PUT /api/clientes/{id} | ClienteDTO; salva revisão | Ambos |
| DELETE /api/clientes/{id} | Exclui se não houver vínculos | Ambos |
| POST /api/documentos/cliente | multipart `arquivo`; retorna ClienteEntity, inclusive para CPF existente | Ambos |
| GET /api/documentos/cliente/{id} | Array dos documentos do cliente (corrige retorno singular antigo) | Ambos |
| GET /api/documentos/{id} | Metadados, sem caminho físico | Ambos |
| GET /api/documentos/{id}/arquivo | Download autenticado | Ambos |
| POST /api/ocr/processar/{id} | Reprocessa documento; exige mesmo CPF do vínculo | Ambos |
| GET /api/ocr/documento/{id} | JSON e estado da última extração | Ambos |
| GET /api/modelos | Array dos publicados | Ambos |
| GET /api/modelos/campos | Catálogo de chaves permitidas | Ambos |
| POST /api/modelos | Entrada de modelo, cria rascunho | ADMIN |
| PUT /api/modelos/{id} | Entrada + `revisao` atual; volta a rascunho | ADMIN |
| POST /api/modelos/{id}/publicar | `{ "revisao": 0 }`; valida e cria versão | ADMIN |
| POST /api/modelos/{id}/inativar | Impede geração nova | ADMIN |
| POST /api/modelos/{id}/duplicar | `{ "codigo": "novo-codigo" }` | ADMIN |
| GET /api/modelos/{id}/versoes | Histórico publicado | ADMIN |
| POST /api/modelos/validar | `{ "conteudo": "..." }`; campos obrigatórios | ADMIN |
| POST /api/modelos/preview | `{ "conteudo": "..." }`; texto com valores fictícios | ADMIN |
| POST /api/minutas | `{ "clienteId": 1, "modeloCodigo": "procuracao" }` | Ambos |
| GET /api/minutas | Filtros opcionais `clienteId`, `busca` | Ambos |
| GET /api/minutas/{id} | Texto, revisão, situação, versão de origem | Ambos |
| PUT /api/minutas/{id} | `{ "conteudo": "texto revisado", "revisao": 0, "finalizar": true }` | Ambos |
| GET /api/minutas/{id}/arquivo | `formato=pdf`, `docx` ou `txt`; somente finalizados | Ambos |
| GET /api/admin/painel | Contagens e pendências | ADMIN |
| GET /api/admin/usuarios | Usuários sem hash de senha | ADMIN |
| POST /api/admin/usuarios | nome, email, perfil, oab, ativo, senha inicial | ADMIN |
| PUT /api/admin/usuarios/{id} | Mesmos campos; senha opcional, revoga sessões | ADMIN |
| GET /api/admin/modelos | Todos os modelos, inclusive rascunhos e inativos | ADMIN |
| GET /api/admin/auditoria | Page; filtros usuario, modulo, acao, inicio/fim ISO Instant, page, size até 100 | ADMIN |
| GET/PUT /api/admin/escritorio | Objeto com nome, endereco, telefone, email | ADMIN |
| GET /api/admin/ia | Modelo ativo e modelosPermitidos | ADMIN |
| PUT /api/admin/ia | `{ "modelo": "qwen2.5vl:latest" }` | ADMIN |
| POST /api/eventos | modulo, acao, registro; ator derivado da sessão | Ambos |

Modelo mínimo para teste (texto demonstrativo, sem validade jurídica):
```json
{"codigo":"procuracao","nome":"Procuração — exemplo acadêmico","categoria":"Procuração","area":"Cível","tags":"inicial","descricao":"Revisar texto antes de publicar","conteudo":"Eu, {{cliente.nome}}, CPF {{cliente.cpf}}, declaro ciência deste documento de demonstração. Data: {{documento.data}}."}
```

O valor `revisao` é controle de concorrência e deve ser devolvido exatamente como recebido. `numeroVersao` é a versão editorial publicada; são conceitos diferentes. Alterar modelo publicado o coloca em rascunho e suspende novas gerações até republicar; minutas antigas preservam seu snapshot.

Eventos frontend aceitos: `ABRIR_TELA`, `CANCELAR_EDICAO`, `ERRO_INTERFACE`; módulos `EXTRACAO`, `CLIENTES`, `DOCUMENTOS`, `MODELOS`, `USUARIOS`, `AUDITORIA`, `CONFIGURACOES`. Não envie conteúdo de formulários nem credenciais.
