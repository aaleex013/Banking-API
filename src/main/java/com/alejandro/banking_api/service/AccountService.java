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
            String codigoEntidad = generarDigitosAleatorios(4);
            String codigoOficina = generarDigitosAleatorios(4);
            String numeroCuenta = generarDigitosAleatorios(10);

            String digitosControlNacionales = calcularDigitosControlCcc(
                    codigoEntidad,
                    codigoOficina,
                    numeroCuenta
            );

            String ccc = codigoEntidad + codigoOficina + digitosControlNacionales + numeroCuenta;

            String digitosControlIban = calcularDigitosControlIban(ccc);


            iban = "ES" + digitosControlIban + ccc;


        } while (accountRepository.existsByAccountNumber(iban));

        return iban;
    }

    private String generarDigitosAleatorios(int longitud) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < longitud; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }


    private String calcularDigitosControlCcc(
            String codigoEntidad,
            String codigoOficina,
            String numeroCuenta
    ) {

        String primerBloque = "00" + codigoEntidad + codigoOficina;

        int primerDigitoControl = calcularUnDigitoControlEspanol(primerBloque);

        int segundoDigitoControl = calcularUnDigitoControlEspanol(numeroCuenta);

        return String.valueOf(primerDigitoControl) + segundoDigitoControl;
    }

    private int calcularUnDigitoControlEspanol(String diezDigitos) {


        int[] pesos = {1, 2, 4, 8, 5, 10, 9, 7, 3, 6};

        int suma = 0;

        for (int i = 0; i < 10; i++) {
            int digito = Character.getNumericValue(diezDigitos.charAt(i));
            suma += digito * pesos[i];
        }

        int resto = suma % 11;

        int digitoControl = 11 - resto;

        if (digitoControl == 11) {
            return 0;
        }

        if (digitoControl == 10) {
            return 1;
        }

        return digitoControl;
    }


    private String calcularDigitosControlIban(String ccc) {


        String codigoPaisNumerico = convertirPaisANumeros();


        String numeroReordenado = ccc + codigoPaisNumerico + "00";

        BigInteger numeroGigante = new BigInteger(numeroReordenado);

        int mod97 = numeroGigante.mod(BigInteger.valueOf(97)).intValue();

        int digitosControl = 98 - mod97;

        return String.format("%02d", digitosControl);
    }


    private String convertirPaisANumeros() {
        StringBuilder sb = new StringBuilder();

        for (char letra : "ES".toUpperCase().toCharArray()) {
            sb.append((letra - 'A') + 10);
        }

        return sb.toString();
    }
}
