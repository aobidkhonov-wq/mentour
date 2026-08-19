package uz.tune.mentourBiz.config.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.*;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.model.ErrorMessage;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSingleton messageSingleton;

    @ExceptionHandler(PermissionForbidden.class)
    public ResponseEntity<Object> handleException(PermissionForbidden ex) {
        // Attempt to translate the key passed in ex.getMessage(), fallback to generic ACCESS_DENIED
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(),
                MessageKey.ACCESS_DENIED.getKey());

        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 403), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<Object> handlePaymentRequired(PaymentRequiredException ex) {
        // Treat ex.getMessage() as key
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(), ex.getMessage());
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 402), HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleException(ValidationException ex) {
        // Treat ex.getMessage() as key
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(), ex.getMessage());
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 400), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(), ex.getMessage());
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 400), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleException(EntityNotFoundException ex) {
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(), ex.getMessage());
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 404), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedAccessException ex) {
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(), ex.getMessage());
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 401), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleException(NoResourceFoundException ex) {
        Logger.exception(ex);
        return new ResponseEntity<>(new ErrorMessage("Url not found", 404), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Object> handleException(JsonProcessingException ex) {
        return new ResponseEntity<>(new ErrorMessage("Invalid request format", 400), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleException(IllegalArgumentException ex) {
        // Log the actual argument error but return a clean message
        String localizedMessage = messageSingleton.getMessage(ex.getMessage(), "Invalid argument provided");
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 400), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<Object> handleException(AuthorizationDeniedException ex) {
        return new ResponseEntity<>(new ErrorMessage(MessageKey.ACCESS_DENIED.getKey(), 403), HttpStatus.FORBIDDEN);
    }

    // Handle global system exceptions (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        Logger.exception(ex); // Log the full stack trace for devs

        // Return a localized "Unknown Error" to the user
        String localizedMessage = MessageKey.UNKNOWN_ERROR.getKey();
        return new ResponseEntity<>(new ErrorMessage(localizedMessage, 500), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}