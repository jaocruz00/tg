/* Adaptador opcional. Importe como módulo; não armazena credenciais em disco.
 * O token é mantido apenas na memória da página e deve ser obtido pela tela de login.
 */
export function criarApiTG(base = "http://localhost:8080") {
  let token = null;
  async function request(path, { method = "GET", body, arquivo = false } = {}) {
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    if (body && !arquivo) headers["Content-Type"] = "application/json";
    const response = await fetch(base + path, { method, headers, body: body ? (arquivo ? body : JSON.stringify(body)) : undefined });
    if (!response.ok) {
      if (response.status === 401) token = null;
      const erro = await response.json().catch(() => ({}));
      throw new Error(erro.message || `Erro HTTP ${response.status}`);
    }
    if (response.headers.get("content-type")?.includes("application/json")) return response.json();
    return response.blob();
  }
  return {
    async login(email, senha) { const dados = await request("/api/auth/login", {method:"POST",body:{email,senha}}); token=dados.token; return dados; },
    async logout() { try { await request("/api/auth/logout",{method:"POST"}); } finally { token=null; } },
    me: () => request("/api/auth/me"),
    clientes: () => request("/api/clientes"),
    cliente: id => request(`/api/clientes/${id}`),
    salvarCliente: (id, dados) => request(id ? `/api/clientes/${id}` : "/api/clientes", {method:id?"PUT":"POST",body:dados}),
    extrair: arquivo => { const body = new FormData(); body.append("arquivo",arquivo); return request("/api/documentos/cliente",{method:"POST",body,arquivo:true}); },
    modelos: () => request("/api/modelos"),
    gerar: (clienteId, modeloCodigo) => request("/api/minutas",{method:"POST",body:{clienteId,modeloCodigo}}),
    revisar: (id,conteudo,revisao,finalizar=false) => request(`/api/minutas/${id}`,{method:"PUT",body:{conteudo,revisao,finalizar}}),
    async baixar(id, formato="pdf") {
      const blob = await request(`/api/minutas/${id}/arquivo?formato=${encodeURIComponent(formato)}`);
      const url=URL.createObjectURL(blob); const a=document.createElement("a");a.href=url;a.download=`documento-${id}.${formato}`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    },
    request
  };
}
