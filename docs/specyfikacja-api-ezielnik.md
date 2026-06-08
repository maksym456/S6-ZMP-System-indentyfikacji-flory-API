# System identyfikacji flory (e-zielnik)
## Specyfikacja modułu serwerowego (REST API)

**Autor:** Maksym Wilk, nr albumu 43900
**Przedmiot:** Zespołowe metody programowania (ZMP), semestr VI
**Data:** 7 czerwca 2026

---

## 1. Wstęp

Moduł serwerowy projektu **e-zielnik** to centralne **REST API** cyfrowego zielnika roślin, z którego korzystają aplikacje klienckie projektu. Część serwerowa napisana jest w **Java + Spring Boot**, z bazą **PostgreSQL**. Aplikacja jest w pełni wdrożona i zahostowana.

Z perspektywy użytkownika system pozwala fotografować rośliny, rozpoznawać ich gatunek przez zewnętrzny serwis **PlantNet**, gromadzić je w nazwanych zielnikach, opisywać, udostępniać publicznie lub znajomym oraz otrzymywać powiadomienia. Część administracyjna udostępnia statystyki i narzędzia moderacyjne.

### Charakterystyka ilościowa

| Wskaźnik | Wartość |
|---|---|
| Kod produkcyjny Java | ok. 6 600 linii w 87 plikach |
| Kod testów | ok. 3 900 linii w 22 plikach |
| Pakiety dziedzinowe / encje | 10 modułów / 9 encji |
| Kontrolery REST | 8 (kilkadziesiąt punktów końcowych) |
| Próg pokrycia testami | 80% (JaCoCo w CI) |
| Historia repozytorium | 77 rewizji, marzec do czerwca 2026 |

---

## 2. Opis funkcjonalny

### 2.1 Przeznaczenie

API realizuje logikę biznesową cyfrowego zielnika: rejestrację i uwierzytelnianie użytkowników, zarządzanie zielnikami i roślinami, identyfikację gatunków ze zdjęć, przechowywanie i serwowanie plików graficznych, funkcje społecznościowe (znajomi), powiadomienia (w tym push) oraz panel administracyjny. Komunikacja z klientami odbywa się w formacie **JSON** przez **HTTP**, a stan sesji jest bezstanowy (uwierzytelnianie tokenem **JWT**).

### 2.2 Aktorzy systemu

- **Gość** – użytkownik niezalogowany. Może się zarejestrować, zalogować, zresetować hasło oraz przeglądać publiczne zielniki wraz z roślinami i zdjęciami.
- **Użytkownik** – konto zalogowane i ze zweryfikowanym e-mailem. Zarządza własnymi zielnikami i roślinami, dodaje rośliny przez identyfikację ze zdjęcia, nawiązuje znajomości, otrzymuje powiadomienia, może włączyć 2FA.
- **Administrator** – konto z flagą `is_admin`. Ma wgląd w statystyki oraz narzędzia moderacyjne (blokowanie kont, ostrzeżenia, usuwanie treści).

Rola administratora nie jest osobną encją, lecz wynika z pola logicznego `is_admin`. Status konta wyznaczają flagi `is_active` (aktywne) oraz `is_verified` (zweryfikowany e-mail), egzekwowane globalnie przez `AccountStatusInterceptor`.

### 2.3 Realizowane wymagania funkcjonalne

| Nr | Funkcjonalność | Realizacja w API |
|---|---|---|
| FLY-01 | Pierwszy administrator | Osadzany bezpośrednio w bazie; kolejni przez `/users/{userId}/make-admin` |
| FLY-02 | Logowanie administratora | `POST /users/login` |
| FLY-03 | Podgląd wszystkich zielników | `GET /stats/herbaria`, `GET /stats/herbaria/{id}` |
| FLY-04 | Usuwanie zielników użytkowników | `DELETE` zielnika (panel admina) |
| FLY-05 | Usuwanie roślin/zdjęć | `DELETE .../plants/{plantId}`, `.../photos/{photoId}` |
| FLY-06 | Usuwanie i blokowanie kont | `PATCH /users/{id}/ban`, `/unban`, `DELETE /users/{id}` |
| FLY-07 | Ostrzeżenia i push | `POST /users/{id}/warning` (e-mail + powiadomienie + FCM) |
| FLY-08 | Statystyki systemowe | `GET /stats/overview`, `/stats/users` |
| FLY-09 | Publiczne zielniki dla gościa | `GET /herbaria/public`, `GET /herbaria/{id}/plants` |
| FLY-10 | Rejestracja | `POST /users/register` |
| FLY-11 | Logowanie | `POST /users/login` |
| FLY-12 | Reset hasła | `POST /users/forgot-password`, `/users/reset-password` |
| FLY-13 | Dodawanie zielników | `POST /herbaria` |
| FLY-14 | Dodawanie zdjęć i wykrywanie rośliny | `POST /herbaria/{id}/plants/add` + `/confirm` |
| FLY-15 | Opisy roślin i zdjęć | `PATCH .../plants/{id}`, `PATCH .../photos/{id}` |
| FLY-16 | Lista własnych zielników i roślin | `GET /herbaria/me`, `GET .../plants` |
| FLY-17 | Zielnik jako publiczny | `PATCH /herbaria/{id}` (pole `isPublic`) |
| FLY-18 | Dodawanie znajomych | `POST /friends/request`, `/accept` |
| FLY-19 | Zielniki znajomych i publiczne | `GET /herbaria/user/{userId}` (kontrola `areFriends`) |
| FLY-20 | Przeglądanie roślin znajomych | `GET .../plants`, `.../plants/{id}` |
| FLY-21 | Wylogowanie | `POST /users/logout` (unieważnienie refresh tokenu) |
| FLY-22 | Język polski i angielski | Komunikaty API po angielsku; warstwa językowa po stronie klientów |

### 2.4 Kluczowe procesy biznesowe

**Uwierzytelnianie i bezpieczeństwo konta.** Rejestracja waliduje siłę hasła (min. 8 znaków, wielka litera, cyfra, znak specjalny), sprawdza unikalność e-maila i nazwy, zapisuje hash **BCrypt** i wysyła wiadomość weryfikacyjną przez **Brevo**. Logowanie akceptuje e-mail lub nazwę i zwraca parę tokenów: krótko żyjący token dostępu **JWT** (domyślnie 2 h, **HS512**) oraz długo żyjący, nieprzezroczysty **refresh token** (domyślnie 30 dni). Odświeżanie odbywa się z **rotacją** (stary token kasowany, nowy wydawany); ponowne użycie unieważnionego tokenu unieważnia wszystkie sesje użytkownika. Opcjonalne **2FA** korzysta z 6-cyfrowego kodu e-mail (token przejściowy `pre_auth` ważny 5 minut). Reset hasła używa tokenu JWT z wersją hasła (HMAC-SHA256 nad hashem), dzięki czemu link traci ważność po zmianie hasła bez przechowywania stanu w bazie.

**Identyfikacja roślin** (proces dwuetapowy):

1. Użytkownik wysyła zdjęcie (`POST /herbaria/{id}/plants/add`, multipart). API przekazuje obraz do PlantNet i klasyfikuje wynik:
   - **resolved** – gatunek rozpoznany, pasująca roślina już istnieje (po GBIF lub nazwie gatunku); zdjęcie dołączane od razu,
   - **recognized** – gatunek rozpoznany, ale rośliny pasują tylko częściowo; system zwraca rekomendacje i wymaga decyzji,
   - **unrecognized** – gatunek nierozpoznany; wymagane potwierdzenie.
2. Użytkownik potwierdza (`POST /herbaria/{id}/plants/confirm`), wybierając przypisanie do istniejącej rośliny (`existing`) lub utworzenie nowej (`new`).

W dwóch ostatnich przypadkach zdjęcie trafia tymczasowo do katalogu `pending`, a metadane do pamięci. Wpisy oczekujące wygasają po godzinie i są czyszczone zadaniem cyklicznym co 5 minut. Roślina przechowuje dane taksonomiczne z PlantNet: gatunek, identyfikator GBIF, rodzinę, rodzaj, nazwy zwyczajowe.

**Zielniki, zdjęcia i funkcje społecznościowe.** Zielnik to nazwany, domyślnie prywatny pojemnik na rośliny. Limity: maksymalnie 50 zielników na użytkownika, 200 roślin na zielnik, 50 zdjęć na roślinę. Nazwa zielnika unikalna w obrębie użytkownika (także na poziomie bazy). Usunięcie ostatniego zdjęcia rośliny usuwa samą roślinę. Znajomość ma status `PENDING` lub `ACCEPTED`; zaakceptowana daje obu stronom dostęp do prywatnych zielników, roślin i zdjęć. Zaproszenia i ich akceptacja generują powiadomienia w aplikacji oraz push (jeśli skonfigurowano Firebase).

**Panel administracyjny.** Administrator widzi statystyki zbiorcze (użytkownicy, zielniki, rośliny, znajomości), przegląda szczegóły kont, blokuje i odblokowuje konta, nadaje i odbiera rolę administratora, wysyła ostrzeżenia (e-mail + push) oraz usuwa treści. Usunięcie konta to **anonimizacja** (miękkie usunięcie): konto jest dezaktywowane, a e-mail i nazwa nadpisywane wzorcami `deleted-...`, co zachowuje integralność powiązań.

---

## 3. Opis technologiczny

### 3.1 Stos technologiczny

| Technologia | Wersja | Zastosowanie |
|---|---|---|
| Java | 21 | Język programowania (LTS) |
| Spring Boot | 4.0.6 | Szkielet aplikacji, autokonfiguracja |
| Spring Web (MVC) | wg BOM | Warstwa REST |
| Spring Data JPA / Hibernate | wg BOM | Mapowanie ORM |
| Spring Security + OAuth2 Resource Server | wg BOM | Uwierzytelnianie i autoryzacja JWT |
| Bean Validation | wg BOM | Walidacja danych wejściowych |
| PostgreSQL | wg BOM | Relacyjna baza danych |
| JJWT | 0.13.0 | Generowanie i podpisywanie JWT |
| springdoc-openapi | 3.0.2 | Dokumentacja OpenAPI / Swagger UI |
| Bucket4j | 8.17.0 | Ograniczanie liczby żądań (token bucket) |
| Caffeine | 3.2.4 | Pamięć podręczna dla rate limitingu |
| TwelveMonkeys imageio-webp | 3.12.0 | Dekodowanie obrazów WebP |
| Firebase Admin SDK | 9.9.0 | Powiadomienia push (FCM) |
| Apache HttpClient 5 | wg BOM | Klient HTTP |
| Testcontainers | 1.21.4 | Testy integracyjne na realnym PostgreSQL |
| JaCoCo | 0.8.14 | Pomiar pokrycia testami |

### 3.2 Architektura aplikacji

Klasyczna architektura warstwowa Spring Boot:

- **Kontrolery** (`*Controller`) – przyjmują żądania HTTP, odczytują tożsamość z JWT, delegują do serwisów,
- **Serwisy** (`*Service`) – logika biznesowa, walidacje, transakcje,
- **Repozytoria** (`*Repository`) – Spring Data JPA (zapytania pochodne i `@Query`),
- **Encje** (`@Entity`) – obiekty mapowane na tabele,
- **DTO** (`*Request` / `*Response`) – modele wejścia i wyjścia, odseparowane od encji.

Kod podzielony jest na pakiety dziedzinowe (jeden na obszar funkcjonalny):

```
com.ezielnik.api
|-- admin            panel administracyjny
|   |-- content_management   moderacja tresci (zielniki, rosliny, zdjecia)
|   '-- user_management      moderacja kont uzytkownikow
|-- auth             uwierzytelnianie
|   |-- refresh_token        rotacja tokenow odswiezajacych
|   '-- two_factor_auth      2FA
|-- config           bezpieczenstwo, rate limit, bledy
|-- fcm              powiadomienia push (FCM)
|-- friend           znajomi
|-- herbarium        zielniki
|-- notification     powiadomienia w aplikacji
|-- photo            przechowywanie i serwowanie zdjec
|-- plant            rosliny i identyfikacja (PlantNet)
|-- user             konta uzytkownikow (oraz endpointy auth)
'-- ApiApplication   klasa startowa
```

Punkty końcowe uwierzytelniania nie mają osobnego kontrolera, znajdują się w `UserController` pod prefiksem `/users`.

### 3.3 Model danych

Baza zawiera **9 encji**. Wszystkie klucze główne to **UUID** generowane po stronie aplikacji w `@PrePersist`, znaczniki czasu jako `OffsetDateTime`. Centralnym węzłem jest encja **User**.

| Encja | Tabela | Najważniejsze pola i powiązania |
|---|---|---|
| User | users | e-mail (unikalny), nazwa (unikalna), hash hasła, flagi `is_active`, `is_verified`, `is_admin`, `email_two_factor_enabled` |
| Herbarium | herbaria | nazwa, opis, `is_public`; FK `user_id`; unikalność pary (`user_id`, `name`) |
| Plant | plants | nazwa, `detected_species`, `species_id`, rodzina, rodzaj, nazwy zwyczajowe; FK `herbarium_id` |
| PlantPhoto | plant_photos | url, opis, confidence; FK `plant_id` |
| Friendship | friendships | status (PENDING/ACCEPTED); dwa FK do User: `requester_id`, `addressee_id` |
| Notification | notifications | title, message, `is_read`; FK `user_id` |
| RefreshToken | refresh_tokens | `token_hash` (unikalny), `expires_at`; FK `user_id` |
| TwoFactorCode | two_factor_codes | `code_hash`, `expires_at`, used; FK `user_id` |
| DeviceToken | device_tokens | token (unikalny, FCM); FK `user_id` |

**Relacje:**
- Główna hierarchia: **User 1:N Herbarium 1:N Plant 1:N PlantPhoto**,
- User 1:N dla Notification, RefreshToken, TwoFactorCode oraz DeviceToken,
- Friendship modeluje relację samozwrotną na User (dwa odrębne odwołania).

Schemat tworzony jest automatycznie przez Hibernate (`spring.jpa.hibernate.ddl-auto=update`); projekt nie zawiera ręcznych migracji.

### 3.4 Bezpieczeństwo

- **Hasła i kody** haszowane **BCrypt** (dotyczy także jednorazowych kodów 2FA).
- **Tokeny JWT** podpisywane symetrycznie **HS512** (dekodowanie `NimbusJwtDecoder`). Typy tokenów (dostępu, `pre_auth`, weryfikacji e-mail, resetu hasła) rozróżnia deklaracja `purpose`, co zapobiega użyciu w niewłaściwym kontekście.
- **Refresh tokeny** przechowywane wyłącznie jako skrót **SHA-256**, z rotacją i wykrywaniem ponownego użycia.
- **Konfiguracja bezstanowa** (`SessionCreationPolicy.STATELESS`), wyłączone CSRF, Basic i logowanie formularzowe. Niestandardowy `BearerTokenResolver` pomija parsowanie tokenu dla listy ścieżek publicznych.
- **Kontrola stanu konta** (`AccountStatusInterceptor`) blokuje konta nieaktywne i niezweryfikowane oraz pilnuje, że `pre_auth` działa tylko na ścieżkach 2FA.
- **Rate limiting** (`RateLimitFilter`, Bucket4j + Caffeine), per adres IP (domyślnie 30 żądań/min); po przekroczeniu `429 Too Many Requests` z nagłówkiem `Retry-After`.
- **Kontrola dostępu na poziomie obiektu** sprawdza własność zasobu, a przy odczycie również relację znajomości i status publiczny. Operacje na plikach zabezpieczone przed **path traversal**.

### 3.5 Integracje zewnętrzne

- **PlantNet** (`my-api.plantnet.org/v2/identify/all`) – identyfikacja roślin. Zdjęcie wysyłane metodą POST (multipart) z `nb-results=1` i `lang=en`; przetwarzany najlepszy wynik. Błędy wywołania obsługiwane bezpiecznie (degradacja do ścieżki „gatunek nierozpoznany”).
- **Brevo** – transakcyjna poczta e-mail (weryfikacja, reset hasła, kody 2FA, ostrzeżenia), wywoływana przez `RestClient`.
- **Firebase Cloud Messaging** – push jako wiadomość multicast do wszystkich tokenów urządzeń użytkownika. Inicjalizacja opcjonalna; przy braku konfiguracji powiadomienia w aplikacji nadal działają, push jest cicho wyłączany, a nieaktualne tokeny urządzeń są usuwane automatycznie.

### 3.6 Przechowywanie zdjęć

Zdjęcia obsługuje `PhotoStorageService`. Każdy obraz (także WebP, dekodowany przez TwelveMonkeys) jest konwertowany do **JPEG** z jakością 0,85 i zapisywany pod losową nazwą `UUID.jpeg` (z podkatalogiem `pending` na pliki oczekujące). Serwowanie (`GET /photos/{filename}`) ustawia długą pamięć podręczną (`Cache-Control: public, max-age=31536000, immutable`), co jest bezpieczne dzięki niezmiennym nazwom plików.

### 3.7 Interfejs REST

Najważniejsze punkty końcowe w podziale na kontrolery.

**UserController (uwierzytelnianie i konto), prefiks `/users`**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| POST | /users/register | publiczny |
| POST | /users/login | publiczny |
| POST | /users/verify-2fa | token pre_auth |
| POST | /users/2fa/email/enable | zalogowany |
| POST | /users/2fa/disable | zalogowany |
| POST | /users/2fa/send-email-code | token pre_auth |
| GET | /users/me | zalogowany |
| DELETE | /users/me | zalogowany |
| GET | /users/verify | publiczny |
| POST | /users/resend-verification | publiczny |
| POST | /users/forgot-password | publiczny |
| GET | /users/reset-password | publiczny (formularz HTML) |
| POST | /users/reset-password | publiczny (JSON lub formularz) |
| POST | /users/refresh | publiczny (refresh token) |
| POST | /users/logout | publiczny (refresh token) |
| POST | /users/me/fcm-token | zalogowany |
| DELETE | /users/me/fcm-token | zalogowany |

**HerbariumController, prefiks `/herbaria`**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| POST | /herbaria | zalogowany |
| GET | /herbaria/me | zalogowany |
| GET | /herbaria/public | publiczny |
| GET | /herbaria/user/{userId} | zalogowany |
| GET | /herbaria/{id} | właściciel/publiczny/znajomy/admin |
| PATCH | /herbaria/{id} | właściciel |
| DELETE | /herbaria/{id} | właściciel |

**PlantController, prefiks `/herbaria/{herbariumId}/plants`**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| POST | /.../plants/add | właściciel |
| POST | /.../plants/confirm | właściciel |
| GET | /.../plants | publiczny/znajomy/właściciel |
| GET | /.../plants/{plantId} | publiczny/znajomy/właściciel |
| PATCH | /.../plants/{plantId} | właściciel |
| DELETE | /.../plants/{plantId} | właściciel |
| GET | /.../plants/{plantId}/photos/{photoId} | publiczny/znajomy/właściciel |
| PATCH | /.../plants/{plantId}/photos/{photoId} | właściciel |
| DELETE | /.../plants/{plantId}/photos/{photoId} | właściciel |
| POST | /.../plants/{plantId}/photos/{photoId}/move | właściciel |

**PhotoController**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| GET | /photos/{filename} | publiczny/znajomy/właściciel |

**FriendshipController, prefiks `/friends`**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| POST | /friends/request | zalogowany |
| POST | /friends/{id}/accept | adresat |
| DELETE | /friends/{id} | strona relacji |
| GET | /friends | zalogowany |
| GET | /friends/requests | zalogowany |
| GET | /friends/requests/sent | zalogowany |

**NotificationController, prefiks `/notifications`**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| GET | /notifications | zalogowany |
| GET | /notifications/unread | zalogowany |
| PATCH | /notifications/{id}/read | właściciel |
| PATCH | /notifications/read-all | zalogowany |

**Kontrolery administracyjne (`/stats`, `/users`)**

| Metoda | Ścieżka | Dostęp |
|---|---|---|
| GET | /stats/overview | administrator |
| GET | /stats/users | administrator |
| GET | /stats/users/{userId} | administrator |
| GET | /stats/users/{userId}/friends | administrator |
| GET | /stats/herbaria | administrator |
| GET | /stats/herbaria/{herbariumId} | administrator |
| PATCH | /users/{userId}/ban | administrator |
| PATCH | /users/{userId}/unban | administrator |
| PATCH | /users/{userId}/make-admin | administrator |
| PATCH | /users/{userId}/remove-admin | administrator |
| POST | /users/{userId}/warning | administrator |
| DELETE | /users/{userId} | administrator |
| DELETE | /users/{userId}/herbaria/{herbariumId} | administrator |
| DELETE | /users/{userId}/herbaria/{herbariumId}/plants/{plantId} | administrator |
| DELETE | /users/{userId}/.../plants/{plantId}/photos/{photoId} | administrator |

Pełna, interaktywna dokumentacja generowana jest automatycznie (springdoc-openapi) pod `/swagger-ui.html` oraz `/v3/api-docs`.

### 3.8 Testy i jakość kodu

Strategia dwupoziomowa:

- **Testy integracyjne** (sufiks `IT`, 14 plików) uruchamiają pełny kontekst Spring Boot na losowym porcie, na prawdziwym PostgreSQL przez Testcontainers. Klasa bazowa `IntegrationTestBase` czyści bazę przed każdym testem i dostarcza metody pomocnicze. Usługi zewnętrzne (Brevo, PlantNet, magazyn plików) podmieniane na atrapy (`@MockitoBean`).
- **Testy jednostkowe** (sufiks `Test`, 4 pliki) działają bez kontekstu Springa, na JUnit 5 i Mockito (logika JWT, rotacja refresh tokenów, konwersja zdjęć, mechanizm roślin oczekujących).

Pokrycie mierzone wtyczką **JaCoCo** z progiem **80% instrukcji** (z wyłączeniem pakietu konfiguracyjnego i klas DTO). Testy obejmują m.in. kontrolę dostępu, rate limiting, walidację danych, odporność na awarię poczty i limit rozmiaru pliku. Pokrycie jest weryfikowane automatycznie w CI (GitHub Actions) przy każdym push i pull request.

---

## 4. Podsumowanie

Zrealizowany moduł API to kompletny, samodzielny serwer aplikacji e-zielnik. Pokrywa wszystkie wymagania funkcjonalne warstwy serwerowej: od uwierzytelniania i zarządzania kontami, przez identyfikację roślin z użyciem PlantNet, zarządzanie zielnikami, roślinami i zdjęciami, funkcje społecznościowe i powiadomienia, aż po panel administracyjny.

Technicznie projekt korzysta z aktualnego stosu **Java 21 i Spring Boot**, stosuje sprawdzone wzorce (architektura warstwowa, bezstanowe JWT, rozdzielenie DTO od encji), dba o bezpieczeństwo (BCrypt, rotacja tokenów, rate limiting, kontrola dostępu) oraz o jakość kodu (testy jednostkowe i integracyjne na realnej bazie, próg pokrycia 80% weryfikowany w CI). Aplikacja jest wdrożona i zahostowana.
