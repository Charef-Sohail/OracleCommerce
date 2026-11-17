package com.enset.oraclecommerce.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author pc
 **/
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        return buildErrorPage(
                HttpStatus.NOT_FOUND,
                "Ressource introuvable",
                ex.getMessage(),
                "error/404"
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex) {
        return buildErrorPage(
                HttpStatus.BAD_REQUEST,
                "Opération impossible",
                ex.getMessage(),
                "error/400"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(
            DataIntegrityViolationException ex) {

        logger.warn("Violation de contrainte en base de données", ex);

        return buildErrorPage(
                HttpStatus.CONFLICT,
                "Conflit de données",
                "Cette opération viole une contrainte de la base de données.",
                "error/409"
        );
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnexpected(Exception ex) {
        logger.error("Une erreur inattendue est survenue", ex);

        return buildErrorPage(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur interne",
                "Une erreur inattendue est survenue. Veuillez réessayer.",
                "error/500"
        );
    }

    private ModelAndView buildErrorPage(
            HttpStatus status,
            String title,
            String message,
            String viewName) {

        ModelAndView modelAndView = new ModelAndView(viewName);
        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());
        modelAndView.addObject("title", title);
        modelAndView.addObject("message", message);

        return modelAndView;
    }
}
