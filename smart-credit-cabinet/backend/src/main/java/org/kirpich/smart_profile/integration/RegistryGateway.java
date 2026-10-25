package org.kirpich.smart_profile.integration;

import org.kirpich.smart_profile.domain.sources.BkiReport;
import org.kirpich.smart_profile.domain.sources.EgrulCompany;
import org.kirpich.smart_profile.domain.sources.FnsBusiness;

/**
 * Внешние источники данных о заёмщике. Контракты упрощены, но смысл интеграции сохранён:
 * временный сбой ({@link SourceUnavailableException}) повторяем, «не найдено» ({@link SourceNotFoundException}) — нет.
 */
public interface RegistryGateway {

    EgrulCompany egrulCompany(String inn);

    FnsBusiness fnsBusiness(String inn);

    BkiReport bkiReport(String inn, String consentId);
}
