package dev.zeann3th.stresspilot.service.impl;

import dev.zeann3th.stresspilot.common.Constants;
import dev.zeann3th.stresspilot.common.enums.ErrorCode;
import dev.zeann3th.stresspilot.common.mappers.EnvironmentVariableMapper;
import dev.zeann3th.stresspilot.dto.environment.EnvironmentVariableDTO;
import dev.zeann3th.stresspilot.dto.environment.UpdateEnvironmentRequestDTO;
import dev.zeann3th.stresspilot.entity.EnvironmentVariableEntity;
import dev.zeann3th.stresspilot.exception.CommandExceptionBuilder;
import dev.zeann3th.stresspilot.repository.EnvironmentVariableRepository;
import dev.zeann3th.stresspilot.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnvironmentServiceImpl implements EnvironmentService {

    private final EnvironmentVariableRepository envVarRepository;
    private final EnvironmentVariableMapper envVarMapper;

    @Override
    public List<EnvironmentVariableDTO> getEnvironmentVariables(Long environmentId) {
        List<EnvironmentVariableEntity> currentVars = envVarRepository.findAllByEnvironmentId(environmentId);
        return currentVars.stream()
                .map(envVarMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public void updateEnvironmentVariables(Long environmentId, UpdateEnvironmentRequestDTO dto) {
        List<EnvironmentVariableEntity> currentVars = envVarRepository.findAllByEnvironmentId(environmentId);

        Map<Long, EnvironmentVariableEntity> currentById = currentVars.stream()
                .collect(Collectors.toMap(EnvironmentVariableEntity::getId, Function.identity()));

        Set<String> currentKeys = currentVars.stream()
                .map(EnvironmentVariableEntity::getKey)
                .collect(Collectors.toSet());

        handleRemove(environmentId, currentKeys, currentById, dto);
        handleUpdate(environmentId, currentKeys, currentById, dto);
        handleAdd(environmentId, currentKeys, dto);
    }

    private void handleRemove(
            Long environmentId,
            Set<String> currentKeys,
            Map<Long, EnvironmentVariableEntity> currentById,
            UpdateEnvironmentRequestDTO dto
    ) {
        if (dto.getRemoved() == null || dto.getRemoved().isEmpty()) return;

        // Validate that all removed IDs exist
        for (Long id : dto.getRemoved()) {
            if (!currentById.containsKey(id)) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "Variable id " + id + " does not belong to environment " + environmentId));
            }
        }

        // Remove safely
        for (Long id : dto.getRemoved()) {
            EnvironmentVariableEntity entity = currentById.get(id);
            if (entity != null) {
                currentKeys.remove(entity.getKey());
                currentById.remove(id);
            }
        }

        envVarRepository.deleteAllById(dto.getRemoved());
    }

    private void handleUpdate(
            Long environmentId,
            Set<String> currentKeys,
            Map<Long, EnvironmentVariableEntity> currentById,
            UpdateEnvironmentRequestDTO dto
    ) {
        if (dto.getUpdated() == null || dto.getUpdated().isEmpty()) return;

        List<EnvironmentVariableEntity> updatedEntities = new ArrayList<>();

        for (UpdateEnvironmentRequestDTO.Update update : dto.getUpdated()) {
            EnvironmentVariableEntity variable = currentById.get(update.getId());
            if (variable == null) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "Variable id " + update.getId() + " does not belong to environment " + environmentId));
            }

            String oldKey = variable.getKey();
            String newKey = update.getKey();

            if (!Objects.equals(oldKey, newKey) && currentKeys.contains(newKey)) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "Duplicate key: " + newKey));
            }

            variable.setKey(newKey);
            variable.setValue(update.getValue());
            variable.setIsActive(update.isActive());

            currentKeys.remove(oldKey);
            currentKeys.add(newKey);

            updatedEntities.add(variable);
        }

        envVarRepository.saveAll(updatedEntities);
    }

    private void handleAdd(Long environmentId, Set<String> currentKeys, UpdateEnvironmentRequestDTO dto) {
        if (dto.getAdded() == null || dto.getAdded().isEmpty()) return;

        List<EnvironmentVariableEntity> toSave = new ArrayList<>();

        for (UpdateEnvironmentRequestDTO.Add add : dto.getAdded()) {
            if (currentKeys.contains(add.getKey())) {
                throw CommandExceptionBuilder.exception(ErrorCode.BAD_REQUEST,
                        Map.of(Constants.REASON, "Duplicate key: " + add.getKey()));
            }

            EnvironmentVariableEntity entity = EnvironmentVariableEntity.builder()
                    .environmentId(environmentId)
                    .key(add.getKey())
                    .value(add.getValue())
                    .isActive(true)
                    .build();

            toSave.add(entity);
            currentKeys.add(add.getKey());
        }

        envVarRepository.saveAll(toSave);
    }
}