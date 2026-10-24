package org.kirpich.smart_profile.services;

import lombok.RequiredArgsConstructor;
import org.kirpich.smart_profile.enums.BkiStatus;
import org.kirpich.smart_profile.enums.EgrulStatus;
import org.kirpich.smart_profile.enums.FinalVerdict;
import org.kirpich.smart_profile.models.Company;
import org.kirpich.smart_profile.models.Verification;
import org.kirpich.smart_profile.repositories.VerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationRepository verificationRepository;

    public Verification createVerification(Company company, long requestedAmount){
        List<String> reasons = new ArrayList<>();
        BkiStatus bki = creditHistoryCheck(company, requestedAmount, reasons);
        EgrulStatus egrul = EgrulStatus.EGRUL_NOT_CHECKED;

        if (bki == BkiStatus.BKI_PASSED) {
            egrul = egrulCheck(company, reasons);
        }
        boolean ok = bki == BkiStatus.BKI_PASSED && egrul == EgrulStatus.EGRUL_PASSED;

        return verificationRepository.save(Verification.builder()
                .bkiStatus(bki)
                .egrulStatus(egrul)
                .finalVerdict(ok ? FinalVerdict.VERIFICATION_APPROVED
                        : FinalVerdict.VERIFICATION_REJECTED)
                .reasons(String.join("; ", reasons))
                .issueDate(LocalDateTime.now())
                .build());
    }

    private BkiStatus creditHistoryCheck(Company c, long amount, List<String> reasons){
        if(c.getRevenue() == null || amount > c.getRevenue() / 2){
            reasons.add("Запрашиваемая сумма слишком большая в сравнении с запрашиваемой суммой");
            return BkiStatus.BKI_FAILED;
        }
        return BkiStatus.BKI_PASSED;
    }

    private EgrulStatus egrulCheck(Company c, List<String> reasons) {
        boolean ok = true;
        if (!isValidInn10(c.getInn())) {
            reasons.add("Ошибка в ИНН"); ok = false;
        }
        if (!isValidOgrn13(c.getOgrn())) {
            reasons.add("Ошибка в ОГРН"); ok = false;
        }
        return ok ? EgrulStatus.EGRUL_PASSED : EgrulStatus.EGRUL_FAILED;
    }


    static boolean isValidInn10(String inn) {
        if (inn == null || !inn.matches("\\d{10}")) return false;
        int[] k = {2, 4, 10, 3, 5, 9, 4, 6, 8};
        int sum = 0;
        for (int i = 0; i < 9; i++) sum += (inn.charAt(i) - '0') * k[i];
        return (sum % 11) % 10 == inn.charAt(9) - '0';
    }

    static boolean isValidOgrn13(String ogrn) {
        if (ogrn == null || !ogrn.matches("\\d{13}")) return false;
        long first12 = Long.parseLong(ogrn.substring(0, 12));
        return (first12 % 11) % 10 == ogrn.charAt(12) - '0';
    }
}
