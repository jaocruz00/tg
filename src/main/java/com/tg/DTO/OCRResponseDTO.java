package com.tg.DTO;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OCRResponseDTO {

  private Long id;

  private Long documentoId;

  private String resultadoJson;

  private String status;

  private LocalDateTime dataProcessamento;
}
