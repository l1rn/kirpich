package org.kirpich.smart_profile.seed;

import java.time.LocalDate;

import org.kirpich.smart_profile.domain.UserRole;
import org.kirpich.smart_profile.model.AppUser;
import org.kirpich.smart_profile.model.Company;
import org.kirpich.smart_profile.repository.CompanyRepository;
import org.kirpich.smart_profile.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Синтетические демо-данные. Компании совпадают со сценариями заглушек в mocks/ (по ИНН):
 * у каждой своя история — одобрение, контрпредложение, ручная проверка, отказ, сбой БКИ.
 * Пароль у всех тестовых пользователей одинаковый — DEMO_PASSWORD (только для демо-стенда).
 */
@Component
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "demo";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final CompanyRepository companies;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(CompanyRepository companies, UserRepository users, PasswordEncoder passwordEncoder) {
        this.companies = companies;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return;
        }
        Company romashka = company("7701123451", "ООО «Ромашка»", "OOO", "1197746123450", "47.11", "Иванова Анна Сергеевна",
                LocalDate.of(2019, 4, 15));
        Company petrov = company("771234567859", "ИП Петров Пётр Петрович", "IP", "324774600123456", "43.32",
                "Петров Пётр Петрович", LocalDate.of(2024, 3, 11));
        Company stroy = company("5003123458", "ООО «СтройМонтаж»", "OOO", "1185003012345", "41.20", "Кузнецов Игорь Викторович",
                LocalDate.of(2018, 6, 1));
        Company zakat = company("7801123455", "ООО «Закат»", "OOO", "1177847123456", "56.10", "Соколов Дмитрий Андреевич",
                LocalDate.of(2017, 2, 20));
        Company tishina = company("6623123450", "ООО «Тишина»", "OOO", "1216600123456", "62.01", "Морозова Елена Игоревна",
                LocalDate.of(2021, 9, 1));

        user("romashka", "Иванова Анна Сергеевна", UserRole.DIRECTOR, romashka);
        user("romashka-buh", "Смирнова Ольга Петровна", UserRole.ACCOUNTANT, romashka);
        user("petrov", "Петров Пётр Петрович", UserRole.DIRECTOR, petrov);
        user("stroy", "Кузнецов Игорь Викторович", UserRole.DIRECTOR, stroy);
        user("zakat", "Соколов Дмитрий Андреевич", UserRole.DIRECTOR, zakat);
        user("tishina", "Морозова Елена Игоревна", UserRole.DIRECTOR, tishina);
        user("underwriter", "Кредитова Мария Андреевна", UserRole.UNDERWRITER, null);
        log.info("Загружены демо-данные: 5 компаний, 7 пользователей (логины — в README)");
    }

    private Company company(String inn, String name, String legalForm, String ogrn, String okved, String director, LocalDate registeredAt) {
        Company company = new Company();
        company.setInn(inn);
        company.setName(name);
        company.setLegalForm(legalForm);
        company.setOgrn(ogrn);
        company.setOkved(okved);
        company.setDirectorName(director);
        company.setRegisteredAt(registeredAt);
        return companies.save(company);
    }

    private void user(String login, String fullName, UserRole role, Company company) {
        AppUser user = new AppUser();
        user.setLogin(login);
        user.setFullName(fullName);
        user.setRole(role);
        user.setCompany(company);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        users.save(user);
    }
}
