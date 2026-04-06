package com.alejandro.banking_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.AccountLockedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Este metodo captura los errores de validación que lanza Spring
    // cuando un DTO con @Valid no cumple alguna regla:
    // @NotBlank, @Email, @Size, etc.
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        // Aquí creamos un Map para devolver los errores en formato JSON.
        // La idea es guardar:
        // clave   -> nombre del campo
        // valor   -> mensaje de error de ese campo

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        // ex.getBindingResult() contiene el resultado de la validación.
        // getFieldErrors() devuelve la lista de errores de campos concretos.
        // Recorremos cada error y guardamos:
        // - error.getField() -> por ejemplo "email"
        // - error.getDefaultMessage() -> por ejemplo "El email no es válido"

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        // Devolvemos:
        // - código HTTP 400 Bad Request
        // - body con el mapa de errores
        //
        // Ejemplo de respuesta:
        // {
        //   "email": "El email no es válido",
        //   "password": "La contraseña debe tener al menos 6 caracteres"
        // }
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    // Este metodo captura la exception personalizada que tú lanzas
    // cuando alguien intenta registrarse con un email repetido.
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {

        Map<String, String> error = new HashMap<>();
        // Creamos un Map simple para devolver un único mensaje de error.

        error.put("message", ex.getMessage());
        // Guardamos el mensaje de la excepción con la clave "message".
        // ex.getMessage() será algo como:
        // "Email already exists"

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        // Devolvemos:
        // - código HTTP 409 Conflict
        // - body con el mensaje de error
        //
        // Ejemplo:
        // {
        //   "message": "Email already exists"
        // }
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    // Este metodo captura la excepción que lanzas cuando el login falla:
    // email incorrecto, password incorrecto o usuario inactivo.
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {

        Map<String, String> error = new HashMap<>();
        // Igual que antes, usamos un Map simple para devolver el mensaje.

        error.put("message", ex.getMessage());
        // Guardamos el texto del error.
        // Por ejemplo:
        // "Invalid email or password"

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        // Devolvemos:
        // - código HTTP 401 Unauthorized
        // - body con el mensaje
        //
        // Ejemplo:
        // {
        //   "message": "Invalid email or password"
        // }
    }

    @ExceptionHandler(Exception.class)
    // Este es un handler genérico.
    // Captura cualquier excepción no controlada por los handlers anteriores.
    // Sirve como "red de seguridad" para evitar respuestas feas o stacktraces.
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {

        Map<String, String> error = new HashMap<>();
        // Creamos un Map para devolver un mensaje genérico.

        error.put("message", "An unexpected error occurred");
        // Aquí no devolvemos ex.getMessage() al cliente
        // para no exponer detalles internos de la aplicación.

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        // Devolvemos:
        // - código HTTP 500 Internal Server Error
        // - body con mensaje genérico
    }

    @ExceptionHandler(UserNotfoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<Map<String, String>> handleInactiveuser(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleAccountNotFound(AccountNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    @ExceptionHandler
    public ResponseEntity<Map<String, String>> handleUnauthorizedAcces(AccountLockedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
