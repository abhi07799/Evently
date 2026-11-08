package com.abhishek.evently.exception;

import com.abhishek.evently.dto.response.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class CustomGlobalExceptionHandler
{
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResourceAlreadyExistException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleResourceAlreadyExistException(ResourceAlreadyExistException ex, WebRequest request)
    {
        ErrorResponseDTO ErrorResponseDTO = com.abhishek.evently.dto.response.ErrorResponseDTO.builder().status(HttpStatus.CONFLICT).timestamp(LocalDateTime.now()).message(ex.getMessage()).build();
        return new ResponseEntity<>(ErrorResponseDTO, HttpStatus.CONFLICT);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request)
    {
        ErrorResponseDTO ErrorResponseDTO = com.abhishek.evently.dto.response.ErrorResponseDTO.builder().status(HttpStatus.NOT_FOUND).timestamp(LocalDateTime.now()).message(ex.getMessage()).build();
        return new ResponseEntity<>(ErrorResponseDTO, HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleValidationException(MethodArgumentNotValidException ex, WebRequest request)
    {
        Map<String, Object> errors = new HashMap<>();
        List<ObjectError> errorObjects = ex.getBindingResult().getAllErrors();
        for (ObjectError errorObject : errorObjects)
        {
            FieldError fieldError = (FieldError) errorObject;
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponseDTO ErrorResponseDTO = com.abhishek.evently.dto.response.ErrorResponseDTO.builder().status(HttpStatus.BAD_REQUEST).timestamp(LocalDateTime.now()).message("Validation Error").details(errors).build();
        return new ResponseEntity<>(ErrorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(CustomException.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleCustomException(CustomException ex, WebRequest request)
    {
        ErrorResponseDTO ErrorResponseDTO = com.abhishek.evently.dto.response.ErrorResponseDTO.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).timestamp(LocalDateTime.now()).message(ex.getMessage()).build();
        return new ResponseEntity<>(ErrorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ErrorResponseDTO> handleAnyException(Exception ex, WebRequest request)
    {
        ErrorResponseDTO ErrorResponseDTO = com.abhishek.evently.dto.response.ErrorResponseDTO.builder().status(HttpStatus.INTERNAL_SERVER_ERROR).timestamp(LocalDateTime.now()).message("Something went wrong! Please try again later!").debugMessage(ex.getMessage()).build();
        return new ResponseEntity<>(ErrorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
