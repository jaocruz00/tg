package com.tg.Config;
import com.tg.Repositories.SessaoRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@Configuration @EnableScheduling @RequiredArgsConstructor
public class SessionCleanup {
 private final SessaoRepository sessoes;
 @Scheduled(fixedDelay=3600000,initialDelay=3600000) @Transactional
 public void removerExpiradas() { sessoes.removerExpiradas(Instant.now()); }
}
