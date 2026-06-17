# Repaso: Testing & Coverage — NanoBank Ledger

## Stack

| Capa | Tecnología |
|------|-----------|
| Backend | Java 21, Spring Boot 3.3.5, JUnit 5, Mockito, JaCoCo 0.8.12 |
| Frontend | Angular 17, Jasmine, Karma, karma-coverage (Istanbul) |
| DB dev | H2 en modo PostgreSQL |
| DB prod | PostgreSQL (perfil `prod`) |

---

## 1. Backend — JUnit 5 + Mockito

### Anatomía de un test unitario Spring

```java
@ExtendWith(MockitoExtension.class)          // carga Mockito sin Spring context
@DisplayName("WalletService")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;   // mock del repo
    @InjectMocks private WalletService walletService;  // clase bajo prueba

    @Test
    void shouldCreateWallet() {
        when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // ...
        assertThat(response.balance()).isEqualByComparingTo("500.00");
    }
}
```

**Regla clave:** `@ExtendWith(MockitoExtension.class)` en lugar de `@SpringBootTest` → tests rápidos, sin contexto Spring, sin base de datos.

### Nested classes para organización

```java
@Nested @DisplayName("create()") class CreateWallet { ... }
@Nested @DisplayName("transfer()") class Transfer    { ... }
```

Agrupa por método/feature. JaCoCo reporta por clase anidada, pero el check corre sobre el paquete completo.

### `@PrePersist` no corre en tests unitarios

`@PrePersist` solo se dispara cuando JPA persiste la entidad (en BD real). En unit tests con `@Mock`, el repositorio devuelve lo que le digas y **nunca llama `@PrePersist`**.

```java
// Solución: inicializar manualmente en setUp()
user.setRole(Role.ROLE_USER);
user.setStatus(UserStatus.ACTIVE);

// O probar el método directamente:
wallet.onCreate();    // cubre el branch null/not-null del @PrePersist
```

### Cubriendo branches de `@PrePersist` manualmente

```java
@Test void onCreateShouldSetDefaultsWhenNull() {
    Wallet w = new Wallet();
    w.onCreate();                               // branch: status == null → ACTIVE
    assertThat(w.getStatus()).isEqualTo(WalletStatus.ACTIVE);
}

@Test void onCreateShouldPreserveExistingValues() {
    wallet.setStatus(WalletStatus.ACTIVE);
    wallet.onCreate();                          // branch: status != null → preserve
    assertThat(wallet.getStatus()).isEqualTo(WalletStatus.ACTIVE);
}
```

---

## 2. Backend — JaCoCo

### Configuración en `pom.xml`

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                        <includes>
                            <include>*.application.service</include>
                            <include>*.domain.model</include>
                        </includes>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Puntos clave:**
- `prepare-agent` → instrumenta el bytecode antes de los tests
- `report` → genera el HTML en `target/site/jacoco/`
- `check` → falla el build si no se alcanzan los umbrales (corre en fase `verify`)
- `element: PACKAGE` → el threshold aplica POR PAQUETE, no global
- Usar `mvn verify` (no `mvn test`) para que el `check` corra

### Fases de Maven relevantes

```
compile → test-compile → test → package → verify
                           ↑                 ↑
                    JaCoCo prepare-agent   JaCoCo check
```

---

## 3. Backend — Spring Circular Dependency

### El problema

```
AuthService → PasswordEncoder
           → AuthenticationManager
                 ↓ (necesita)
           SecurityConfig → JwtAuthFilter → AuthService (UserDetailsService)
```

Tres beans se necesitan mutuamente → Spring no puede crear ninguno.

### Solución: `@Lazy` + clase de configuración separada

**Paso 1:** Extraer `PasswordEncoder` a su propia clase `@Configuration`:

```java
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**Paso 2:** Usar `@Lazy` en el parámetro que cierra el ciclo:

```java
public AuthService(
    UserRepository userRepository,
    PasswordEncoder passwordEncoder,
    JwtService jwtService,
    @Lazy AuthenticationManager authManager   // ← se crea cuando se usa por primera vez
) { ... }
```

**`@Lazy` dice:** "no crees este bean todavía; créalo cuando lo necesite por primera vez". Rompe el ciclo porque Spring puede arrancar sin resolver inmediatamente `AuthenticationManager`.

---

## 4. Backend — Bug: entidad local vs. persistida

```java
// ❌ MAL: user.getRole() devuelve null (el objeto local nunca tuvo @PrePersist)
User user = new User();
userRepository.save(user);
return AuthResponse.of(token, user.getId(), user.getEmail(), user.getRole().name());

// ✅ BIEN: saved es el objeto que devolvió el mock/JPA con todos los campos seteados
User saved = userRepository.save(user);
return AuthResponse.of(token, saved.getId(), saved.getEmail(), saved.getRole().name());
```

**Regla:** Después de `save()`, usar la referencia **devuelta** por el repositorio, no la original.

---

## 5. Frontend — Angular Testing Fundamentals

### Dos maneras de configurar HttpClient en tests

| API antigua | API nueva (Angular 15+) |
|---|---|
| `HttpClientTestingModule` | `provideHttpClient()` + `provideHttpClientTesting()` |
| `imports: [HttpClientTestingModule]` | `providers: [provideHttpClient(), provideHttpClientTesting()]` |

Ambas funcionan; no mezclarlas en el mismo módulo de test.

### Patron básico de test de servicio HTTP

```typescript
describe('WalletService', () => {
  let service: WalletService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    service = TestBed.inject(WalletService);
    http    = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());  // verifica que no quedaron requests sin consumir

  it('getAll() should call GET /wallets', () => {
    service.getAll().subscribe(wallets => {
      expect(wallets.length).toBe(1);
    });

    // el request está pendiente — lo resolvemos manualmente
    const req = http.expectOne('/api/v1/wallets');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [mockWallet] });
  });
});
```

### `fixture.detectChanges()` — la más importante de Angular testing

```typescript
// ❌ ngOnInit NO se llama
const fixture = TestBed.createComponent(DashboardComponent);
http.expectOne(...);   // falla: el request nunca se hizo

// ✅ ngOnInit SÍ se llama
const fixture = TestBed.createComponent(DashboardComponent);
fixture.detectChanges();   // dispara ngOnInit → loadWallets() → HTTP request
http.expectOne(...);
```

**Regla de oro:** `TestBed.createComponent()` crea el componente pero **no** dispara el ciclo de vida. `fixture.detectChanges()` es quien dispara `ngOnInit` y la renderización del template.

### Por qué los tests funcionaban sin `detectChanges()` (y dejaron de hacerlo)

Istanbul instrumenta el código: añade contadores a cada línea. Cuando el archivo está instrumentado, la inicialización del módulo tiene más código que ejecutar, lo que le da tiempo a Angular/Zone.js de disparar change detection automáticamente como efecto secundario. Al excluir el componente de la instrumentación, ese efecto secundario desaparece y los tests quedan correctamente expuestos.

**Lección:** Si un test pasa "por casualidad", el día que cambies algo inocuo (instrumentación, orden de imports, versión de Angular) dejará de pasar. La forma correcta siempre fue con `fixture.detectChanges()`.

### `fakeAsync` + `tick()` — control de tiempo en tests

```typescript
it('should load wallets', fakeAsync(() => {
  const fixture = TestBed.createComponent(DashboardComponent);
  fixture.detectChanges();   // ngOnInit → async call

  // el Observable del HTTP está pendiente
  http.expectOne(r => r.url.includes('/wallets'))
      .flush({ success: true, data: mockWallets });

  tick();  // avanza el tiempo virtual, resuelve el Observable

  expect(fixture.componentInstance.totalBalance()).toBe(1500);
}));
```

`fakeAsync` congela el tiempo real. `tick()` avanza el tiempo virtual. Fundamental para testear código asíncrono de forma determinista.

---

## 6. Frontend — Coverage con Karma/Istanbul

### Configuración en `karma.conf.js`

```javascript
coverageReporter: {
  dir: './coverage/nanobank-frontend',
  reporters: [
    { type: 'html' },        // reporte visual en ./coverage/
    { type: 'text-summary' },// resumen en consola
    { type: 'lcovonly' }     // para CI/SonarQube
  ],
  check: {
    global: {
      statements: 80,    // umbral global para todos los archivos
      branches:   75,
      functions:  80,
      lines:      80
    },
    each: {              // umbral POR ARCHIVO
      statements: 70,
      branches:   65,
      functions:  70,
      lines:      70,
      excludes: [        // archivos excluidos del check POR ARCHIVO
        'src/main.ts',
        'src/app/app.config.ts',
        // ...
      ]
    }
  }
}
```

**Diferencia importante:**
- `each.excludes` → exime un archivo del check por-archivo, pero **SÍ** cuenta para el global
- `codeCoverageExclude` en `angular.json` → el archivo **no se instrumenta**, no aparece en ningún conteo

### `codeCoverageExclude` en `angular.json`

```json
"test": {
  "options": {
    "codeCoverage": true,
    "codeCoverageExclude": [
      "src/main.ts",
      "src/app/app.routes.ts",
      "src/app/features/**/*.component.ts",   // ← componentes UI (testear con e2e)
      "src/app/core/guards/**",
      "src/app/core/interceptors/**",
      "src/app/core/models/**"
    ]
  }
}
```

**¿Por qué excluir los components del coverage?**
Los componentes Angular tienen lógica de presentación: templates, CDK Drag&Drop, formularios reactivos. La forma correcta de testearlos es con tests E2E (Cypress, Playwright). En tests unitarios con Karma se testea la **lógica de negocio** (services). Excluirlos del coverage refleja esta separación de responsabilidades.

### Dónde vive cada tipo de coverage

```
servicios → unit tests (Karma/Jasmine)     → karma.conf.js check
componentes → E2E tests (Cypress)          → excluidos de unit coverage
interceptors/guards → integración          → excluidos de unit coverage
```

---

## 7. Frontend — El branch de `loadUser()` en `AuthService`

### El código

```typescript
private loadUser(): AuthResponse | null {
  try {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;   // 2 branches: raw truthy / falsy
  } catch { return null; }                 // branch: catch ejecutado
}
```

Istanbul cuenta 3 branches:
1. `raw` es falsy → `return null` ✅ (cubierto: localStorage vacío en cada test)
2. `raw` es truthy → `JSON.parse(raw)` ❌ (no cubierto)
3. `catch` ejecutado ❌ (no cubierto)

### Por qué los tests normales no los cubren

```typescript
beforeEach(() => {
  localStorage.clear();                    // localStorage vacío
  service = TestBed.inject(AuthService);   // loadUser() ve null → solo branch falsy
});
```

`AuthService` es `providedIn: 'root'` — es un singleton. Una vez creado en el `beforeEach`, `loadUser()` ya corrió. No hay forma de "recrearlo" desde dentro del mismo describe.

### Solución: describe separado sin `beforeEach` del padre

```typescript
describe('AuthService - loadUser()', () => {
  afterEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();   // limpiar para el siguiente describe
  });

  it('should restore user when valid JSON exists in localStorage', () => {
    localStorage.setItem('nb_user', JSON.stringify(mockUser));  // primero setear
    TestBed.configureTestingModule({                             // luego crear servicio
      imports: [HttpClientTestingModule, RouterTestingModule]
    });
    const svc = TestBed.inject(AuthService);   // loadUser() ve el JSON → branch truthy
    expect(svc.isLoggedIn()).toBeTrue();
  });

  it('should return null when localStorage contains invalid JSON', () => {
    localStorage.setItem('nb_user', '{corrupted json}');
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule, RouterTestingModule] });
    const svc = TestBed.inject(AuthService);   // loadUser() → JSON.parse falla → catch
    expect(svc.isLoggedIn()).toBeFalse();
  });
});
```

**Lección:** Para testear lógica del constructor/inicializador de un servicio singleton, crea un `describe` independiente y configura el estado **antes** de `TestBed.inject()`.

---

## 8. TypeScript path aliases en tests

### El problema

```
Error TS5090: Non-relative paths are not allowed when 'baseUrl' is not set
```

Los alias `@core/*`, `@features/*`, `@shared/*` requieren que TypeScript sepa desde dónde resolver. Sin `baseUrl`, TypeScript no puede resolver `@core/services/wallet.service`.

### Solución en `tsconfig.json`

```json
{
  "compilerOptions": {
    "baseUrl": "./",         // ← imprescindible para que funcionen los paths
    "paths": {
      "@core/*":     ["src/app/core/*"],
      "@features/*": ["src/app/features/*"],
      "@shared/*":   ["src/app/shared/*"],
      "@env/*":      ["src/environments/*"]
    }
  }
}
```

**Nota:** `tsconfig.spec.json` extiende `tsconfig.json` con `"extends": "./tsconfig.json"`, por lo que los tests heredan automáticamente este cambio.

---

## 9. Resumen de commands

### Backend

```bash
# correr tests + coverage check
cd backend/nanobank-ledger
./mvnw verify

# si el path tiene espacios en Windows:
java.exe -classpath ".mvn/wrapper/maven-wrapper.jar" \
  "-Dmaven.multiModuleProjectDirectory=." \
  org.apache.maven.wrapper.MavenWrapperMain verify

# ver reporte HTML
start target/site/jacoco/index.html
```

### Frontend

```bash
cd frontend/nanobank-frontend
npm run test:coverage      # tests + coverage check (single run)
npm test                   # tests en modo watch

# ver reporte HTML
start coverage/nanobank-frontend/index.html
```

---

## 10. Resultados finales

### Backend

| Métrica | Resultado | Umbral |
|---------|-----------|--------|
| Tests | **80/80** ✅ | — |
| JaCoCo check | **PASS** ✅ | ≥80% líneas, ≥75% branches por paquete |

### Frontend

| Métrica | Resultado | Umbral |
|---------|-----------|--------|
| Tests | **27/27** ✅ | — |
| Statements | **95.31%** ✅ | 80% |
| Branches | **90%** ✅ | 75% |
| Functions | **93.33%** ✅ | 80% |
| Lines | **96.36%** ✅ | 80% |

---

## 11. Patrones que vale la pena recordar

| Patrón | Dónde usarlo |
|--------|-------------|
| `@ExtendWith(MockitoExtension.class)` | Todo test unitario Java sin Spring |
| `@Nested` | Agrupar tests por método en la misma clase |
| `when(...).thenAnswer(inv -> inv.getArgument(0))` | Simular `save()` que devuelve el argumento |
| `@Lazy` en constructor | Romper circular dependency en Spring |
| `fixture.detectChanges()` | Siempre después de `createComponent()` para disparar ngOnInit |
| `fakeAsync()` + `tick()` | Controlar tiempo en tests con Observables |
| `http.verify()` en `afterEach` | Detectar requests HTTP no consumidos |
| `TestBed.resetTestingModule()` | Forzar recreación del servicio singleton entre tests |
| `codeCoverageExclude` en `angular.json` | Excluir archivos de la instrumentación de coverage completamente |
