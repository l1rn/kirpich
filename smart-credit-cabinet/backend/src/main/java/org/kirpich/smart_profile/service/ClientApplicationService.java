package org.kirpich.smart_profile.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.kirpich.smart_profile.config.JsonCodec;
import org.kirpich.smart_profile.domain.Actor;
import org.kirpich.smart_profile.domain.ApplicationStatus;
import org.kirpich.smart_profile.domain.Finance;
import org.kirpich.smart_profile.domain.UserRole;
import org.kirpich.smart_profile.domain.policy.CreditPolicy;
import org.kirpich.smart_profile.domain.policy.Option;
import org.kirpich.smart_profile.domain.policy.Product;
import org.kirpich.smart_profile.model.AppUser;
import org.kirpich.smart_profile.model.CreditApplication;
import org.kirpich.smart_profile.model.Document;
import org.kirpich.smart_profile.model.InfoRequest;
import org.kirpich.smart_profile.model.Offer;
import org.kirpich.smart_profile.repository.CreditApplicationRepository;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.ApplicationPatch;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.ApplicationRequest;
import org.kirpich.smart_profile.web.dto.ApplicationDtos.SubmitRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Действия клиента с заявкой: черновик, отправка, ответы на запросы банка, предложение и подписание. */
@Service
@Transactional
public class ClientApplicationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CreditApplicationRepository applications;
    private final CreditPolicy policy;
    private final ApplicationWorkflow workflow;
    private final DocumentStorage storage;
    private final JsonCodec json;

    public ClientApplicationService(
            CreditApplicationRepository applications,
            CreditPolicy policy,
            ApplicationWorkflow workflow,
            DocumentStorage storage,
            JsonCodec json) {
        this.applications = applications;
        this.policy = policy;
        this.workflow = workflow;
        this.storage = storage;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<CreditApplication> list(AppUser user) {
        return applications.findByCompanyIdOrderByIdDesc(user.getCompany().getId());
    }

    @Transactional(readOnly = true)
    public CreditApplication get(AppUser user, Long id) {
        return owned(user, id);
    }

    public CreditApplication create(AppUser user, ApplicationRequest request) {
        List<String> errors = termErrors(request.productCode(), request.purpose(), request.amount(), request.termMonths());
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        CreditApplication app = new CreditApplication();
        app.setCompany(user.getCompany());
        app.setCreatedBy(user);
        app.setProductCode(request.productCode());
        app.setPurpose(request.purpose());
        app.setAmount(request.amount());
        app.setTermMonths(request.termMonths());
        app.setRevenue12m(request.revenue12m());
        app.setExistingMonthlyPayments(request.existingMonthlyPayments() == null ? 0 : request.existingMonthlyPayments());
        applications.save(app);
        app.setNumber("%d-%06d".formatted(Year.now().getValue(), app.getId()));
        workflow.log(app, "created", "Создан черновик заявки", Actor.CLIENT, user.getId());
        return app;
    }

    public CreditApplication update(AppUser user, Long id, ApplicationPatch patch) {
        CreditApplication app = owned(user, id);
        if (app.getStatus() != ApplicationStatus.DRAFT) {
            throw new ValidationFailedException("Изменить можно только черновик");
        }
        Optional.ofNullable(patch.productCode()).ifPresent(app::setProductCode);
        Optional.ofNullable(patch.purpose()).ifPresent(app::setPurpose);
        Optional.ofNullable(patch.amount()).ifPresent(app::setAmount);
        Optional.ofNullable(patch.termMonths()).ifPresent(app::setTermMonths);
        Optional.ofNullable(patch.revenue12m()).ifPresent(app::setRevenue12m);
        Optional.ofNullable(patch.existingMonthlyPayments()).ifPresent(app::setExistingMonthlyPayments);
        List<String> errors = termErrors(app.getProductCode(), app.getPurpose(), app.getAmount(), app.getTermMonths());
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return app;
    }

    public CreditApplication submit(AppUser user, Long id, SubmitRequest request) {
        CreditApplication app = owned(user, id);
        List<String> errors = termErrors(app.getProductCode(), app.getPurpose(), app.getAmount(), app.getTermMonths());
        if (app.getRevenue12m() == null || app.getRevenue12m() <= 0) {
            errors.add("Укажите выручку за последние 12 месяцев");
        }
        if (!request.bkiConsent()) {
            errors.add("Нужно согласие на запрос в бюро кредитных историй");
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        Instant now = Instant.now();
        app.setBkiConsentAt(now);
        app.setSubmittedAt(now);
        workflow.transition(app, ApplicationStatus.CHECKING, Actor.CLIENT, user.getId(), "Заявка отправлена в банк");
        workflow.restartChecks(app);
        return app;
    }

    public CreditApplication cancel(AppUser user, Long id) {
        CreditApplication app = owned(user, id);
        workflow.transition(app, ApplicationStatus.CANCELLED, Actor.CLIENT, user.getId(), "Клиент отозвал заявку");
        workflow.currentOffer(app).filter(o -> o.getStatus() != Offer.Status.SIGNED).ifPresent(o -> o.setStatus(Offer.Status.REJECTED));
        return app;
    }

    public Document upload(AppUser user, Long id, String docType, MultipartFile file) {
        CreditApplication app = owned(user, id);
        if (app.getStatus() != ApplicationStatus.DRAFT && app.getStatus() != ApplicationStatus.NEEDS_INFO) {
            throw new ValidationFailedException("Загрузить документ можно в черновике или по запросу банка");
        }
        if (file == null || file.isEmpty()) {
            throw new ValidationFailedException("Файл пустой");
        }
        Optional<InfoRequest> openRequest = workflow.openInfoRequest(app);
        Set<String> requestKeys = new HashSet<>();
        openRequest.ifPresent(r -> workflow.items(r).forEach(item -> requestKeys.add(item.key())));
        boolean known = policy.documents().stream().map(Option::code).anyMatch(docType::equals);
        if (!known && !requestKeys.contains(docType)) {
            throw new ValidationFailedException("Неизвестный тип документа: " + docType);
        }

        DocumentStorage.StoredFile stored = storage.store(app.getId(), file);
        Document document = new Document();
        document.setApplication(app);
        document.setDocType(docType);
        document.setFilename(file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
        document.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        document.setSizeBytes(stored.size());
        document.setSha256(stored.sha256());
        document.setStoragePath(stored.path());
        document.setUploadedBy(user.getId());
        if (requestKeys.contains(docType)) {
            document.setInfoRequestId(openRequest.get().getId());
        }
        app.getDocuments().add(document);
        workflow.log(app, "document_uploaded", "Загружен документ: " + document.getFilename(), Actor.CLIENT, user.getId());
        return document;
    }

    public CreditApplication respond(AppUser user, Long id, Long requestId, Map<String, String> answers) {
        CreditApplication app = owned(user, id);
        InfoRequest request = workflow.openInfoRequest(app)
                .filter(r -> r.getId().equals(requestId))
                .orElseThrow(() -> new NotFoundException("Открытый запрос не найден"));
        Map<String, String> given = answers == null ? Map.of() : answers;
        List<String> missing = new ArrayList<>();
        for (InfoItem item : workflow.items(request)) {
            boolean answered = item.isFile()
                    ? app.getDocuments().stream().anyMatch(d -> d.getDocType().equals(item.key()))
                    : !given.getOrDefault(item.key(), "").isBlank();
            if (!answered) {
                missing.add((item.isFile() ? "Загрузите: " : "Ответьте: ") + item.label());
            }
        }
        if (!missing.isEmpty()) {
            throw new ValidationFailedException(missing);
        }
        request.setStatus(InfoRequest.Status.ANSWERED);
        request.setResponse(json.write(given));
        request.setAnsweredAt(Instant.now());
        ApplicationStatus target = request.getReturnStatus();
        workflow.transition(app, target, Actor.CLIENT, user.getId(), "Клиент ответил на запрос банка");
        if (target == ApplicationStatus.CHECKING) {
            workflow.restartChecks(app);
        }
        return app;
    }

    /** @return код подтверждения для подписания (в демо показывается на экране) */
    public String acceptOffer(AppUser user, Long id) {
        CreditApplication app = owned(user, id);
        Offer offer = activeOffer(app, Offer.Status.ACTIVE, "Нет предложения, которое можно принять");
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        offer.setStatus(Offer.Status.ACCEPTED);
        offer.setAcceptedAt(Instant.now());
        offer.setSigningCodeHash(sha256(code));
        workflow.log(app, "offer_accepted", "Клиент принял предложение", Actor.CLIENT, user.getId());
        return code;
    }

    public CreditApplication signOffer(AppUser user, Long id, String code) {
        if (user.getRole() != UserRole.DIRECTOR) {
            throw new ForbiddenActionException("Подписать договор может только руководитель компании");
        }
        CreditApplication app = owned(user, id);
        Offer offer = activeOffer(app, Offer.Status.ACCEPTED, "Сначала примите предложение");
        if (!MessageDigest.isEqual(sha256(code.trim()).getBytes(StandardCharsets.UTF_8),
                offer.getSigningCodeHash().getBytes(StandardCharsets.UTF_8))) {
            throw new ValidationFailedException("Неверный код подтверждения");
        }
        offer.setStatus(Offer.Status.SIGNED);
        offer.setSignedAt(Instant.now());
        workflow.transition(app, ApplicationStatus.ISSUED, Actor.CLIENT, user.getId(),
                "Договор подписан, %s зачислены на расчётный счёт".formatted(Finance.formatRub(offer.getAmount())));
        return app;
    }

    public CreditApplication rejectOffer(AppUser user, Long id) {
        CreditApplication app = owned(user, id);
        Offer offer = workflow.currentOffer(app)
                .filter(o -> o.getStatus() == Offer.Status.ACTIVE || o.getStatus() == Offer.Status.ACCEPTED)
                .orElseThrow(() -> new ValidationFailedException("Нет предложения, от которого можно отказаться"));
        offer.setStatus(Offer.Status.REJECTED);
        workflow.transition(app, ApplicationStatus.CANCELLED, Actor.CLIENT, user.getId(), "Клиент отказался от предложения");
        return app;
    }

    List<String> termErrors(String productCode, String purpose, long amount, int termMonths) {
        List<String> errors = new ArrayList<>();
        Optional<Product> found = policy.findProduct(productCode);
        if (found.isEmpty()) {
            errors.add("Неизвестный продукт: " + productCode);
            return errors;
        }
        Product product = found.get();
        Optional<String> purposeLabel = policy.purposeLabel(purpose);
        if (purposeLabel.isEmpty()) {
            errors.add("Неизвестная цель кредита: " + purpose);
        } else if (!product.purposes().contains(purpose)) {
            errors.add("«%s» не подходит для цели «%s»".formatted(product.name(), purposeLabel.get()));
        }
        if (amount < product.minAmount() || amount > product.maxAmount()) {
            errors.add("Сумма для «%s» — от %s до %s".formatted(
                    product.name(), Finance.formatRub(product.minAmount()), Finance.formatRub(product.maxAmount())));
        }
        if (termMonths < product.minTerm() || termMonths > product.maxTerm()) {
            errors.add(product.minTerm() == product.maxTerm()
                    ? "Срок для «%s» — %d мес.".formatted(product.name(), product.minTerm())
                    : "Срок для «%s» — от %d до %d мес.".formatted(product.name(), product.minTerm(), product.maxTerm()));
        }
        return errors;
    }

    private CreditApplication owned(AppUser user, Long id) {
        return applications.findById(id)
                .filter(app -> app.getCompany().getId().equals(user.getCompany().getId()))
                .orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private Offer activeOffer(CreditApplication app, Offer.Status status, String error) {
        if (app.getStatus() != ApplicationStatus.OFFERED) {
            throw new ValidationFailedException(error);
        }
        return workflow.currentOffer(app).filter(o -> o.getStatus() == status)
                .orElseThrow(() -> new ValidationFailedException(error));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
