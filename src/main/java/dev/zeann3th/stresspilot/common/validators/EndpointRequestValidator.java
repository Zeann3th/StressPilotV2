package dev.zeann3th.stresspilot.common.validators;

import dev.zeann3th.stresspilot.dto.endpoint.EndpointDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EndpointRequestValidator implements ConstraintValidator<ValidEndpointRequest, EndpointDTO> {

    @Override
    public boolean isValid(EndpointDTO request, ConstraintValidatorContext context) {
        if (request.getType() == null) {
            return true;
        }
        boolean valid = true;
        switch (request.getType()) {
            case "gRPC":
                if (request.getGrpcServiceName() == null) valid = false;
                if (request.getGrpcMethodName() == null) valid = false;
                if (request.getGrpcStubPath() == null) valid = false;
                break;
            case "HTTP":
                if (request.getHttpMethod() == null) valid = false;
                if (request.getUrl() == null) valid = false;
                break;
            case "GraphQL":
                if (request.getGraphqlOperationType() == null) valid = false;
                break;
            default:
                valid = false;
                break;
        }
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Missing required field for type " + request.getType())
                    .addConstraintViolation();
        }
        return valid;
    }
}

