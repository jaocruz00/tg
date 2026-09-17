package com.tg.Controller;

import com.tg.DTO.OCRResponseDTO;
import com.tg.Service.OCRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OCRController {

    private final OCRService ocrService;

    @PostMapping("/processar/{documentoId}")
    public ResponseEntity<OCRResponseDTO> processarDocumento(
            @PathVariable Long documentoId
    ) throws IOException {

        OCRResponseDTO resultado =
                ocrService.processarDocumento(documentoId);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/documento/{documentoId}")
    public ResponseEntity<OCRResponseDTO> buscarResultado(
            @PathVariable Long documentoId
    ) {

        OCRResponseDTO resultado =
                ocrService.buscarResultado(documentoId);

        return ResponseEntity.ok(resultado);
    }
}