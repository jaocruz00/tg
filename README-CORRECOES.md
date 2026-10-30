# Execucao

- Requer JDK 21; Maven Wrapper incluido. Execute `./mvnw test` (Windows: `mvnw.cmd test`).
- Configure as variaveis de ambiente de `.env.example`; Spring nao carrega `.env` automaticamente.
- As credenciais de banco e OpenRouter do ZIP original estavam expostas: revogue e substitua ambas antes da implantacao. Remover do codigo nao as revoga.
- Banco novo: aplique `database/baseline.sql`. Banco existente com todas as tabelas originais: backup, depois `database/upgrade-existing.sql`; revise eventuais diferencas antes da execucao. DDL padrao e `validate`.
- `baseline.sql` foi gerado do mapeamento JPA. A sintaxe foi preparada para PostgreSQL; os testes de persistencia usam H2 e nao substituem a validacao em PostgreSQL.
- Para criar o primeiro administrador, informe BOOTSTRAP_ADMIN_EMAIL e BOOTSTRAP_ADMIN_PASSWORD (minimo 12 caracteres, maximo 72 bytes UTF-8), com tabela usuarios vazia. Remova essas variaveis depois da primeira inicializacao.
- Login: POST `/api/auth/login`, corpo `{"email":"...","senha":"..."}`. Use o token em `Authorization: Bearer ...`. Logout: POST `/api/auth/logout`. Alterar senha: PUT `/api/auth/senha`, corpo `{"atual":"...","nova":"..."}`.
- Rotas administrativas exigem ADMIN; demais APIs exigem ADMIN ou ADVOGADO. A API usa bearer, sem cookies de autenticacao. CORS aberto foi removido; o frontend deve usar a mesma origem ou um proxy da aplicacao.
- Listagens de clientes, usuarios, modelos e documentos agora retornam Page (campo `content`), com no maximo 50 registros e parametro `page`. Adapte consumidores que esperavam array ou documento unico.
- POST `/api/documentos/cliente/{clienteId}` salva documento; POST `/api/documentos/cliente` extrai cliente via OpenRouter. Extracao aceita PNG/JPEG; armazenamento tambem aceita PDF. Limite: 5 MiB.
- Configure UPLOAD_DIR e migre arquivos antigos para esse diretorio, atualizando caminho_arquivo no banco. Leitura/exclusao fora da raiz e bloqueada, inclusive links simbolicos que apontam para fora.
- Download usa attachment e nao retorna caminho interno nem relacionamento lazy de cliente. Entidades de minuta/versao nao serializam relacionamentos.
- Arquivos `.pending-delete` podem permanecer se a JVM parar entre a movimentacao e o commit/exclusao fisica. Confira banco e backup antes de recuperar/remover esses arquivos.
- OCR continua usando Ollama; extracao de cliente continua usando OpenRouter. Ambos tem timeouts de 10s/90s, limite de resposta de 2 MiB e admissao de ate quatro requisicoes de IA por instancia. Chamadas externas reais nao sao feitas pelos testes.
- Nao foram incluidos historico Git, configuracao da IDE, target, documentos enviados nem credenciais originais.

# Problemas corrigidos

- Compilacao: Security/Validation ausentes, TemplateEngine inexistente e ClienteEntity sem revisado; dependencias adicionadas, engine de marcadores permitido e campos JPA implementados.
- Jackson 2 fixado junto de Jackson 3: removida dependencia manual, imports padronizados para tools.jackson e ObjectMapper injetado. Referencia: https://docs.spring.io/spring-boot/4.0/reference/features/json.html
- Segredos fixos e dados pessoais/base64 nos logs: variaveis de ambiente e remocao das impressoes.
- Sem cadeia de autorizacao nem bean PasswordEncoder: SecurityFilterChain, BCrypt, sessao bearer com expiracao, revogacao, perfis e limpeza de sessoes expiradas. Referencia: https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html
- Upload: InputStream aberto, MIME sem verificacao, caminho pessoal fixo, limite divergente e arquivo orfao em erro de persistencia: try-with-resources, assinatura basica, raiz configuravel, limite unificado e compensacao.
- Download: caminho fisico exposto e nome concatenado no header: JsonIgnore, confinamento pela raiz real e ContentDisposition. A assinatura inicial nao equivale a um antivirus nem a validacao integral do formato.
- Cliente existente retornava null; datas/nulls da IA causavam excecao e dados revisados eram sobrescritos: retorno do cliente existente, tratamento de campos ausentes, parser estrito e preservacao do cadastro existente.
- Clientes sem validacao/transacao: Bean Validation nos DTOs, transacoes de escrita e controle otimista. CPF de entrada usa 11 digitos; normalize registros legados antes de usar a nova API.
- Busca de documentos por cliente retornava um unico registro: Page e teste com dois documentos.
- Painel carregava tabelas inteiras; configuracao de escritorio fazia quatro SELECTs; listas sem limite: COUNT no banco, consulta em lote e paginacao.
- Relacionamentos EAGER em cascata carregavam dados extras: LAZY e exclusao da serializacao; sessao usa EntityGraph para carregar usuario em uma consulta.
- IA sem timeout/limite/concurrencia: cliente HTTP compartilhado com limites; chamadas remotas fora das transacoes de persistencia.
- JSON malformado deixava OCR PROCESSANDO e chamadas simultaneas disputavam o mesmo resultado: marcacao ERRO, claim transacional com lock, revisao otimista e janela de recuperacao de cinco minutos.
- Configuracao de modelo ignorada pelo OCR e allowed-models ausente: OCR consulta ConfiguracaoService e propriedades completas.
- Exclusao de documento com resultado OCR falhava por FK e podia apagar arquivo antes de falhar no banco: exclusao do resultado na mesma transacao e movimentacao temporaria com restauracao em falha.
- Template desconhecido/sem dados podia produzir minuta incompleta: validacao de marcadores e exigencia de dados preenchidos, sem execucao de expressoes.
