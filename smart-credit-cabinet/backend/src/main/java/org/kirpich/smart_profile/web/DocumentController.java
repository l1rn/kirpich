package org.kirpich.smart_profile.web;

import org.kirpich.smart_profile.model.AppUser;
import org.kirpich.smart_profile.model.CreditApplication;
import org.kirpich.smart_profile.model.Document;
import org.kirpich.smart_profile.security.CurrentUser;
import org.kirpich.smart_profile.service.BankApplicationService;
import org.kirpich.smart_profile.service.ClientApplicationService;
import org.kirpich.smart_profile.service.DocumentStorage;
import org.kirpich.smart_profile.service.NotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

/** Скачивание документа: клиент — по своим заявкам, кредитный специалист — по любым поданным. */
@RestController
@Tag(name = "Документы")
public class DocumentController {

    private final CurrentUser currentUser;
    private final ClientApplicationService clientService;
    private final BankApplicationService bankService;
    private final DocumentStorage storage;

    public DocumentController(CurrentUser currentUser, ClientApplicationService clientService, BankApplicationService bankService,
            DocumentStorage storage) {
        this.currentUser = currentUser;
        this.clientService = clientService;
        this.bankService = bankService;
        this.storage = storage;
    }

    @GetMapping("/api/v1/applications/{id}/documents/{documentId}")
    public ResponseEntity<Resource> download(@AuthenticationPrincipal Jwt token, @PathVariable Long id, @PathVariable Long documentId) {
        AppUser user = currentUser.from(token);
        CreditApplication app = user.getRole().isClient() ? clientService.get(user, id) : bankService.get(id);
        Document document = app.getDocuments().stream()
                .filter(d -> d.getId().equals(documentId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Документ не найден"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFilename(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(storage.load(document.getStoragePath()));
    }
}
