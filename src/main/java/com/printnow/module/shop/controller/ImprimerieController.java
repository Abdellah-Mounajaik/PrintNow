package com.printnow.module.shop.controller;

import com.printnow.module.shop.dto.ImprimerieRequestDTO;
import com.printnow.module.shop.dto.ImprimerieResponseDTO;
import com.printnow.module.shop.service.ImprimerieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imprimeries")
@RequiredArgsConstructor
public class ImprimerieController {

    private final ImprimerieService imprimerieService;

    @PostMapping
    public ResponseEntity<ImprimerieResponseDTO> createImprimerie(@RequestBody ImprimerieRequestDTO dto) {
        return new ResponseEntity<>(imprimerieService.createImprimerie(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ImprimerieResponseDTO>> getAllActiveImprimeries() {
        return ResponseEntity.ok(imprimerieService.getAllActiveImprimeries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImprimerieResponseDTO> getImprimerieById(@PathVariable Long id) {
        return ResponseEntity.ok(imprimerieService.getImprimerieById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImprimerieResponseDTO> updateImprimerie(@PathVariable Long id, @RequestBody ImprimerieRequestDTO dto) {
        return ResponseEntity.ok(imprimerieService.updateImprimerie(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImprimerie(@PathVariable Long id) {
        imprimerieService.deleteImprimerie(id);
        return ResponseEntity.noContent().build();
    }
}