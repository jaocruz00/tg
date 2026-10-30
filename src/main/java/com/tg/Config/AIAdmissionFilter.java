package com.tg.Config;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class AIAdmissionFilter extends OncePerRequestFilter {
 private final Semaphore slots=new Semaphore(4);
 protected void doFilterInternal(HttpServletRequest q,HttpServletResponse s,FilterChain c) throws ServletException,IOException {
   String p=q.getRequestURI();
   boolean limited="POST".equals(q.getMethod()) && (p.startsWith("/api/ocr/processar/") || p.startsWith("/api/documentos/cliente"));
   if(!limited) { c.doFilter(q,s);return; }
   if(!slots.tryAcquire()) { s.sendError(429);return; }
   try { c.doFilter(q,s); } finally { slots.release(); }
 }
}
