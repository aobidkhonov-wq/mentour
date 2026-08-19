package uz.tune.mentourBiz.exception;

public class PermissionForbidden extends RuntimeException{
    public PermissionForbidden(String message) {
        super(message);
    }
}
