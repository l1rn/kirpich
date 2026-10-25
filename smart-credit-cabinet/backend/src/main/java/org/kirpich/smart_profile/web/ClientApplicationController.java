package org.kirpich.smart_profile.web;

import java.util.List;

import org.kirpich.smart_profile.security.CurrentUser;
import org.kirpich.smart_profile.service.ClientApplicationService;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.AcceptOfferResponse;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.ApplicationPatch;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.ApplicationRequest;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.DetailView;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.InfoResponseRequest;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.SignRequest;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.SubmitRequest;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.SummaryView;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Кабинет клиента")
public class ClientApplicationController {

    private final ClientApplicationService service;
    private final CurrentUser currentUser;
    private final ApplicationViews views;

    public ClientApplicationController(ClientApplicationService service, CurrentUser currentUser, ApplicationViews views) {
        this.service = service;
        this.currentUser = currentUser;
        this.views = views;
    }

    @GetMapping
    public List<SummaryView> list(@AuthenticationPrincipal Jwt token) {
        return service.list(currentUser.from(token)).stream().map(views::summary).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать черновик заявки")
    public DetailView create(@AuthenticationPrincipal Jwt token, @Valid @RequestBody ApplicationRequest request) {
        return views.detail(service.create(currentUser.from(token), request));
    }

    @GetMapping("/{id}")
    public DetailView get(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return views.detail(service.get(currentUser.from(token), id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Изменить черновик")
    public DetailView update(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @Valid @RequestBody ApplicationPatch patch) {
        return views.detail(service.update(currentUser.from(token), id, patch));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Отправить заявку в банк — запускает конвейер проверок")
    public DetailView submit(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @RequestBody SubmitRequest request) {
        return views.detail(service.submit(currentUser.from(token), id, request));
    }

    @PostMapping("/{id}/cancel")
    public DetailView cancel(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return views.detail(service.cancel(currentUser.from(token), id));
    }

    @PostMapping(path = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить документ (в черновике или по запросу банка)")
    public DetailView upload(@AuthenticationPrincipal Jwt token, @PathVariable Long id,
            @RequestParam("docType") String docType, @RequestPart("file") MultipartFile file) {
        var user = currentUser.from(token);
        service.upload(user, id, docType, file);
        return views.detail(service.get(user, id));
    }

    @PostMapping("/{id}/info-requests/{requestId}/response")
    @Operation(summary = "Ответить на запрос банка")
    public DetailView respond(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @PathVariable Long requestId,
            @RequestBody InfoResponseRequest request) {
        return views.detail(service.respond(currentUser.from(token), id, requestId, request.answers()));
    }

    @PostMapping("/{id}/offer/accept")
    @Operation(summary = "Принять предложение — возвращает код подтверждения для подписания")
    public AcceptOfferResponse accept(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        String code = service.acceptOffer(currentUser.from(token), id);
        return new AcceptOfferResponse(code,
                "Демо: код показан на экране. В банке он пришёл бы push-уведомлением внутри приложения.");
    }

    @PostMapping("/{id}/offer/sign")
    @Operation(summary = "Подписать договор кодом (только руководитель) — имитация выдачи")
    public DetailView sign(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @Valid @RequestBody SignRequest request) {
        return views.detail(service.signOffer(currentUser.from(token), id, request.code()));
    }

    @PostMapping("/{id}/offer/reject")
    public DetailView reject(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return views.detail(service.rejectOffer(currentUser.from(token), id));
    }
}
