package org.kirpich.smart_profile.web;

import java.util.List;

import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.security.CurrentUser;
import org.kirpich.smart_profile.service.BankApplicationService;
import org.kirpich.smart_profile.web.dto.BankDtos.BankDecisionRequest;
import org.kirpich.smart_profile.web.dto.BankDtos.BankDetailView;
import org.kirpich.smart_profile.web.dto.BankDtos.BankInfoRequest;
import org.kirpich.smart_profile.web.dto.BankDtos.RowView;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/bank/applications")
@Tag(name = "Кабинет кредитного специалиста")
public class BankApplicationController {

    private final BankApplicationService service;
    private final CurrentUser currentUser;
    private final ApplicationViews views;

    public BankApplicationController(BankApplicationService service, CurrentUser currentUser, ApplicationViews views) {
        this.service = service;
        this.currentUser = currentUser;
        this.views = views;
    }

    @GetMapping
    @Operation(summary = "Очередь заявок; без status — все поданные")
    public List<RowView> list(@RequestParam(required = false) ApplicationStatus status) {
        return service.list(status).stream().map(views::row).toList();
    }

    @GetMapping("/{id}")
    public BankDetailView get(@PathVariable Long id) {
        return views.bankDetail(service.get(id));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Взять заявку в работу")
    public BankDetailView assign(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return views.bankDetail(service.assign(currentUser.from(token), id));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Решение по заявке на ручной проверке")
    public BankDetailView decide(@AuthenticationPrincipal Jwt token, @PathVariable Long id,
            @Valid @RequestBody BankDecisionRequest request) {
        return views.bankDetail(service.decide(currentUser.from(token), id, request));
    }

    @PostMapping("/{id}/info-requests")
    @Operation(summary = "Запросить у клиента документы или пояснения")
    public BankDetailView requestInfo(@AuthenticationPrincipal Jwt token, @PathVariable Long id,
            @Valid @RequestBody BankInfoRequest request) {
        return views.bankDetail(service.requestInfo(currentUser.from(token), id, request));
    }
}
