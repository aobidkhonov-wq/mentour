package uz.tune.mentourBiz.exception;


public class PaymentRequiredException extends RuntimeException {
    public PaymentRequiredException(String message) { super(message); }
}