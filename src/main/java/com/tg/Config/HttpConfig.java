package com.tg.Config;
import java.time.Duration;
import java.io.*;
import java.util.concurrent.Semaphore;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.web.client.RestTemplate;
@Configuration
public class HttpConfig {
 @Bean RestTemplate restTemplate() {
   var f = new SimpleClientHttpRequestFactory();
   f.setConnectTimeout(Duration.ofSeconds(10)); f.setReadTimeout(Duration.ofSeconds(90));
   var client = new RestTemplate(f);
   var slots = new Semaphore(4);
   client.getInterceptors().add((req,body,execution) -> {
     if (!slots.tryAcquire()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"IA ocupada");
     try {
       ClientHttpResponse response=execution.execute(req,body);
       return new ClientHttpResponse() {
         private boolean closed;
         public HttpStatusCode getStatusCode() throws IOException { return response.getStatusCode(); }
         public String getStatusText() throws IOException { return response.getStatusText(); }
         public HttpHeaders getHeaders() { return response.getHeaders(); }
         public InputStream getBody() throws IOException {
           return new FilterInputStream(response.getBody()) {
             private long bytes;
             private void count(int n) throws IOException { if(n>0 && (bytes+=n)>2*1024*1024) throw new IOException("Resposta IA excede 2 MB"); }
             public int read() throws IOException { int n=super.read(); count(n<0?0:1); return n; }
             public int read(byte[] b,int off,int len) throws IOException { int n=in.read(b,off,len);count(n);return n; }
           };
         }
         public void close() { if(!closed) { closed=true; try { response.close(); } finally { slots.release(); } } }
       };
     } catch(IOException | RuntimeException e) { slots.release(); throw e; }
   });
   return client;
 }
}
