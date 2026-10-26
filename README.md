# TG — Plataforma de apoio documental à advocacia

Backend Java 21 / Spring Boot 4.1.1, PostgreSQL e Ollama. Continuação do pacote `com.tg` e dos endpoints do front `jaocruz00/Trabalho-de-Graduacao-Front` (consulta em 22/10/2026).

## Executar

1. Instale JDK 21 e PostgreSQL; use `./mvnw` (Linux) ou `mvnw.cmd` (Windows).
2. Crie o banco `tg`. Em banco existente, faça backup e execute `docs/sql/001-administracao.sql`. O script interrompe a migração se houver CPFs duplicados após normalização.
3. Configure `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_EMAIL` e `ADMIN_PASSWORD` (mínimo de 12 caracteres, máximo de 72 bytes). O primeiro administrador só é criado se a tabela de usuários estiver vazia. Remova as variáveis de bootstrap depois do primeiro início.
4. Configure `UPLOAD_DIR` para uma pasta privada e persistente; mova os arquivos antigos e ajuste seus caminhos no banco se necessário. Os caminhos Windows antigos não funcionam automaticamente em Linux.
5. Configure `OLLAMA_URL`, `OLLAMA_MODEL` e `OLLAMA_ALLOWED_MODELS` com um modelo **com visão** já instalado. Padrão: `qwen2.5vl:latest`. Para acesso remoto protegido, credenciais ficam nas variáveis `CF_ACCESS_CLIENT_ID` e `CF_ACCESS_CLIENT_SECRET`, apenas no servidor.
6. Configure `CORS_ORIGINS` com as origens exatas do front, separadas por vírgulas. Execute `./mvnw spring-boot:run`.

O padrão é `DDL_AUTO=validate`. Em banco descartável de desenvolvimento, `DDL_AUTO=update` permite criação automática; não use como substituto da migração de produção. Não há senha padrão ou endpoints de negócio anônimos.

## Fluxo implementado

- Login com senha BCrypt e token Bearer aleatório; apenas hash do token é persistido, validade de 8 horas, logout e alteração de senha revogam sessões.
- Perfis ADMIN e ADVOGADO; somente ADMIN mantém usuários, modelos, dados do escritório, configuração de modelo IA e consulta auditoria.
- Upload `arquivo` de até 5 MB (alinhado à tela atual), validação do conteúdo, JPG/PNG até 20 MP e PDF de até 5 páginas sem senha. PDF é renderizado em imagens antes do Ollama.
- Extração pelo `/api/generate`, sem streaming, JSON, timeout configurável e `keep_alive=2m`. Sem OpenRouter nem serviço de teste na execução.
- CPF normalizado e validado, datas ISO, `dd/MM/yyyy` e meses abreviados em português. Datas ilegíveis ficam pendentes de correção; nunca se inventam datas.
- Cliente, documento e JSON são gravados atomicamente após extração bem-sucedida. CPF existente atualiza o mesmo cadastro; valores OCR ausentes não apagam os existentes. Toda extração exige nova revisão.
- Salvar o formulário marca o cadastro como revisado. Campos complementares não enviados pelo front são preservados; string vazia permite limpá-los.
- Modelos com código, nome, categoria, área, tags, descrição, conteúdo, status, autor e versões publicadas imutáveis. Todos os marcadores usados são obrigatórios; catálogo controlado em `/api/modelos/campos`.
- Minuta com snapshot do texto preenchido e referência à versão. Revisão humana e finalização explícita antes de exportar TXT, DOCX ou PDF. Finalizados não podem ser sobrescritos.
- Auditoria das mutações de negócio e exportações; eventos de interface são identificados como `FRONTEND_NAO_VERIFICADO`, não como prova de operação no servidor. Sem senhas, tokens ou documentos completos nos logs.

## Integração das telas

Veja `docs/API.md` e `docs/frontend/tg-api.js`. O repositório front consultado possui upload e clientes funcionais; modelos e auditoria são links sem telas, e geração é um aviso sem chamada HTTP. **Adicionar autenticação exige adaptar o front**: o código antigo sem Bearer receberá 401. O backend não fornece uma tela de login.

O retorno de `POST /api/documentos/cliente` continua sendo o cliente diretamente. No front, atribua `clienteForm.dataset.clienteId = cliente.id` também após upload: atualmente isso não ocorre e o botão Salvar tenta criar um CPF já existente.

Para geração, carregue modelos publicados, use `codigo` (por exemplo `procuracao` ou `declaracao`) e o ID do cliente revisado. Apresente o texto em um editor para revisão e finalize antes de baixar. Renderize conteúdo como texto, não HTML arbitrário.

## Limites desta entrega

- Modelos são texto com parágrafos e marcadores. Formatação rica, logos, cabeçalhos e assinatura digital não estão implementados; a definição do padrão editorial continua pendente.
- A busca do acervo em `/api/minutas?busca=` é textual. Não é busca semântica por embeddings e não há refinamento por chat/IA.
- Configuração IA permite escolher somente os modelos autorizados no servidor. Não instala modelos nem modifica Tunnel, URL ou credenciais pelo navegador.
- Upload inicial é síncrono. Antes de existir um documento persistido, a falha é registrada na auditoria, não em uma fila de processamento. O reprocessamento de documento existente possui estado PROCESSANDO/PROCESSADO/ERRO.
- Exclusão física de documentos bloqueada para preservar o histórico. Exclusão de cliente com vínculos resulta em 409.
- Redefinição de senha pelo administrador é feita atualizando a senha do usuário; envio de e-mail de recuperação não está incluído.
- Escopo de um escritório: usuários autenticados compartilham clientes e documentos. Não há isolamento multiempresa.
- Paginação de auditoria implementada; listagens de clientes e minutas mantêm arrays para compatibilidade com o front e são adequadas ao MVP.

## Validação

`./mvnw test` executa testes com H2 e Ollama simulado, sem dados reais. Os testes exercitam acesso por perfil, revogação de sessão, upload/upsert, revisão, versionamento, geração e exportação. Testes reais com PostgreSQL e Ollama devem ser executados em homologação antes de produção.

Fontes de integração: https://docs.ollama.com/api/generate e https://docs.spring.io/spring-boot/reference/features/json.html . Mantido Spring Boot 4.1.1 do repositório; Jackson padronizado para `tools.jackson` (Jackson 3).
