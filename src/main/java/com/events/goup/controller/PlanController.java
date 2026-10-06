package com.events.goup.controller;

import com.events.goup.dto.plan.PlanResponse;
import com.events.goup.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Só leitura: apresenta Free e Premium. Não existe endpoint de compra ou de troca de plano;
 * quem é Premium é definido direto no banco.
 */
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> findAll() {
        return ResponseEntity.ok(planService.findAll());
    }
}