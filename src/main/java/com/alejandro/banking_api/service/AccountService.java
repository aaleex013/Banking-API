package com.alejandro.banking_api.service;

import com.alejandro.banking_api.dto.AccountResponse;
import com.alejandro.banking_api.dto.CreateAccountRequest;
import com.alejandro.banking_api.entity.Account;
import com.alejandro.banking_api.entity.User;
import com.alejandro.banking_api.exception.*;
import com.alejandro.banking_api.repository.AccountRepository;
import com.alejandro.banking_api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

@Service
@AllArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    private static final SecureRandom random = new SecureRandom();

    public AccountResponse createAccount(String email, CreateAccountRequest createAccountRequest) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new InactiveUserException("User not active");
        }
        Account account = Account.builder()
                .accountNumber(generarIbanEspanol())
                .accountType(createAccountRequest.accountType())
                .balance(BigDecimal.ZERO)
                .active(true)
                .user(user)
                .build();
        Account savedAccount = accountRepository.save(account);

        return mapToResponse(savedAccount);
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.isActive(),
                account.getCreatedAt()
        );
    }

    public List<AccountResponse> getMyAccounts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse getMyAccountById(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedAccessException("User not authorized");
        }
        return mapToResponse(account);

    }

    /*
     * ========================================================================
     *  GENERACIÓN DEL IBAN ESPAÑOL
     * ========================================================================
     *
     *
     * Un IBAN español tiene esta estructura:
     *
     * ES + 2 dígitos de control IBAN + 20 dígitos del CCC
     *
     * El CCC (Código Cuenta Cliente) español se compone de:
     *
     * - 4 dígitos de entidad
     * - 4 dígitos de oficina
     * - 2 dígitos de control nacionales
     * - 10 dígitos de número de cuenta
     *
     * Es decir:
     *
     * ESkk bbbb oooo dd cccccccccc
     *
     * donde:
     * - ES = país
     * - kk = control IBAN
     * - bbbb = banco
     * - oooo = oficina
     * - dd = control nacional
     * - cccccccccc = número de cuenta
     *
     * Lo que vamos a hacer es:
     *
     * 1. Generar aleatoriamente:
     *    - entidad
     *    - oficina
     *    - número de cuenta
     *
     * 2. Calcular los 2 dígitos de control nacionales del CCC
     *
     * 3. Montar el CCC completo de 20 dígitos
     *
     * 4. Calcular los 2 dígitos de control del IBAN
     *
     * 5. Montar el IBAN final
     *
     * 6. Comprobar que no exista ya en base de datos
     */
    private String generarIbanEspanol() {
        String iban;

        do {
            /*
             * Paso 1: generar las partes básicas del CCC
             *
             * - codigoEntidad: 4 dígitos
             * - codigoOficina: 4 dígitos
             * - numeroCuenta: 10 dígitos
             *
             * Ejemplo inventado:
             * entidad = 1234
             * oficina = 5678
             * numeroCuenta = 1234567890
             */
            String codigoEntidad = generarDigitosAleatorios(4);
            String codigoOficina = generarDigitosAleatorios(4);
            String numeroCuenta = generarDigitosAleatorios(10);

            /*
             * Paso 2: calcular los 2 dígitos de control nacionales del CCC
             *
             * El CCC español no son solo 20 números puestos al azar.
             * Tiene 2 dígitos de control intermedios que validan el número.
             */
            String digitosControlNacionales = calcularDigitosControlCcc(
                    codigoEntidad,
                    codigoOficina,
                    numeroCuenta
            );

            /*
             * Paso 3: construir el CCC completo
             *
             * Quedará así:
             * entidad + oficina + control nacional + numeroCuenta
             *
             * Ejemplo:
             * 1234 + 5678 + 90 + 1234567890
             * = 12345678901234567890
             */
            String ccc = codigoEntidad + codigoOficina + digitosControlNacionales + numeroCuenta;

            /*
             * Paso 4: calcular los 2 dígitos de control del IBAN
             *
             * Estos son los 2 números que van después de "ES".
             */
            String digitosControlIban = calcularDigitosControlIban(ccc);

            /*
             * Paso 5: montar el IBAN final
             *
             * Ejemplo:
             * ES + 67 + 12345678901234567890
             *
             * Resultado:
             * ES6712345678901234567890
             */
            iban = "ES" + digitosControlIban + ccc;

            /*
             * Paso 6: comprobar si ese IBAN ya existe en la base de datos
             *
             * Si existe, repetimos el proceso y generamos otro.
             * Esto evita colisiones con la restricción unique de accountNumber.
             */

        } while (accountRepository.existsByAccountNumber(iban));

        return iban;
    }

    /*
     * Genera una cadena de dígitos aleatorios de la longitud indicada.
     *
     * Ejemplos:
     * generarDigitosAleatorios(4)  -> "4821"
     * generarDigitosAleatorios(10) -> "9384726150"
     *
     * Este metodo lo usamos para crear:
     * - código de entidad
     * - código de oficina
     * - número de cuenta
     */
    private String generarDigitosAleatorios(int longitud) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }

    /*
     * Calcula los 2 dígitos de control nacionales del CCC español.
     *
     * En el CCC español hay 2 dígitos de control:
     *
     * - el primero se calcula usando:
     *   "00" + entidad + oficina
     *
     * - el segundo se calcula usando:
     *   número de cuenta (10 dígitos)
     *
     * Luego se unen ambos.
     *
     * Ejemplo:
     * primer dígito = 9
     * segundo dígito = 0
     * resultado = "90"
     */
    private String calcularDigitosControlCcc(
            String codigoEntidad,
            String codigoOficina,
            String numeroCuenta
    ) {
        /*
         * Para el primer dígito de control se usa:
         * 00 + entidad + oficina
         *
         * Ejemplo:
         * 00 + 1234 + 5678 = 0012345678
         */
        String primerBloque = "00" + codigoEntidad + codigoOficina;

        /*
         * Calculamos el primer dígito de control usando el algoritmo español
         */
        int primerDigitoControl = calcularUnDigitoControlEspanol(primerBloque);

        /*
         * Calculamos el segundo dígito de control usando directamente
         * los 10 dígitos del número de cuenta
         */
        int segundoDigitoControl = calcularUnDigitoControlEspanol(numeroCuenta);

        /*
         * Unimos ambos dígitos y los devolvemos como String
         */
        return String.valueOf(primerDigitoControl) + segundoDigitoControl;
    }

    /*
     * Calcula 1 solo dígito de control nacional español a partir de 10 dígitos.
     *
     * Este algoritmo usa una serie fija de pesos:
     *
     * Posición:  1  2  3  4  5  6   7  8  9 10
     * Peso:      1, 2, 4, 8, 5, 10, 9, 7, 3, 6
     *
     * Qué se hace:
     *
     * 1. Cada dígito se multiplica por su peso
     * 2. Se suman todos los resultados
     * 3. Se hace módulo 11
     * 4. Se calcula 11 - resto
     * 5. Si da 11 -> el dígito final es 0
     * 6. Si da 10 -> el dígito final es 1
     * 7. En cualquier otro caso, el resultado es ese número
     */
    private int calcularUnDigitoControlEspanol(String diezDigitos) {

        /*
         * Pesos oficiales del algoritmo del CCC español
         */
        int[] pesos = {1, 2, 4, 8, 5, 10, 9, 7, 3, 6};

        int suma = 0;

        /*
         * Recorremos los 10 dígitos y multiplicamos cada uno por su peso
         */
        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(diezDigitos.charAt(i));
            suma += digito * pesos[i];
        }

        /*
         * Obtenemos el resto de dividir entre 11
         */
        int resto = suma % 11;

        /*
         * Regla del algoritmo: dígito = 11 - resto
         */
        int digitoControl = 11 - resto;

        /*
         * Casos especiales del algoritmo:
         * - si da 11, se convierte en 0
         * - si da 10, se convierte en 1
         */
        if (digitoControl == 11) {
            return 0;
        }

        if (digitoControl == 10) {
            return 1;
        }

        return digitoControl;
    }

    /*
     * Calcula los 2 dígitos de control del IBAN.
     *
     * El algoritmo del IBAN funciona así:
     *
     * 1. Cogemos el número nacional (en nuestro caso, el CCC)
     * 2. Añadimos al final el país convertido a números
     * 3. Añadimos "00" como control provisional
     * 4. Calculamos mod 97
     * 5. Hacemos 98 - mod97
     * 6. El resultado son los 2 dígitos de control del IBAN
     *
     * Ejemplo conceptual:
     *
     * CCC = 12345678901234567890
     * País = ES -> 1428
     *
     * Se construye:
     * 12345678901234567890 + 1428 + 00
     */
    private String calcularDigitosControlIban(String ccc) {

        /*
         * Convertimos el país a su forma numérica
         *
         * E -> 14
         * S -> 28
         *
         * "ES" -> "1428"
         */
        String codigoPaisNumerico = convertirPaisANumeros("ES");

        /*
         * Construimos el número con el que se calcula el control IBAN:
         *
         * ccc + país en números + "00"
         *
         * Ese "00" es un control provisional.
         */
        String numeroReordenado = ccc + codigoPaisNumerico + "00";

        /*
         * Lo convertimos a BigInteger porque el número es muy largo
         * y puede no caber en tipos enteros normales.
         */
        BigInteger numeroGigante = new BigInteger(numeroReordenado);

        /*
         * Calculamos mod 97, que es la base del algoritmo IBAN
         */
        int mod97 = numeroGigante.mod(BigInteger.valueOf(97)).intValue();

        /*
         * Fórmula del control IBAN:
         * 98 - mod97
         */
        int digitosControl = 98 - mod97;

        /*
         * Formateamos siempre a 2 cifras.
         *
         * Si sale 7 -> "07"
         * Si sale 67 -> "67"
         */
        return String.format("%02d", digitosControl);
    }

    /*
     * Convierte el código de país a números según la regla del IBAN.
     *
     * Cada letra se convierte así:
     * A = 10
     * B = 11
     * C = 12
     * ...
     * Z = 35
     *
     * Ejemplo:
     * E = 14
     * S = 28
     *
     * "ES" -> "1428"
     */
    private String convertirPaisANumeros(String codigoPais) {
        StringBuilder sb = new StringBuilder();

        for (char letra : codigoPais.toUpperCase().toCharArray()) {
            sb.append((letra - 'A') + 10);
        }

        return sb.toString();
    }
}
