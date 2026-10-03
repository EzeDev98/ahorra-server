package com.ahorra.feature.obligation.service.impl;

import com.ahorra.common.exception.BadRequestException;
import com.ahorra.common.money.MoneyUtils;
import com.ahorra.feature.obligation.dto.ObligationRequest;
import com.ahorra.feature.obligation.dto.ObligationResponse;
import com.ahorra.feature.obligation.dto.UpdateObligationRequest;
import com.ahorra.feature.obligation.model.Obligation;
import com.ahorra.feature.obligation.repository.ObligationRepository;
import com.ahorra.feature.obligation.service.ObligationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

import static com.ahorra.common.constants.GeneralConstants.MAX_AMOUNT;
import static com.ahorra.common.constants.GeneralConstants.MAX_OBLIGATIONS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObligationServiceImpl implements ObligationService {

    private final ObligationRepository obligationRepository;

    @Override
    public List<ObligationResponse> list(Long userId) {
        return obligationRepository.findAllByUserIdOrderByIdAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ObligationResponse create(Long userId, ObligationRequest request) {
        if (obligationRepository.countByUserId(userId) >= MAX_OBLIGATIONS) {
            throw new BadRequestException("Too many fixed obligations (max " + MAX_OBLIGATIONS + ")");
        }
        Obligation obligation = new Obligation();
        obligation.setUserId(userId);
        obligation.setName(request.name().trim());
        obligation.setAmount(validAmount(request.amount()));
        obligation.setCadence(request.cadence());
        obligation.setActive(true);
        return toResponse(obligationRepository.save(obligation));
    }

    @Override
    @Transactional
    public ObligationResponse update(Long userId, Long id, UpdateObligationRequest request) {
        Obligation obligation = find(userId, id);
        obligation.setName(request.name().trim());
        obligation.setAmount(validAmount(request.amount()));
        obligation.setCadence(request.cadence());
        obligation.setActive(request.active());
        return toResponse(obligationRepository.save(obligation));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        obligationRepository.delete(find(userId, id));
    }

    private Obligation find(Long userId, Long id) {
        return obligationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Obligation not found"));
    }

    private BigDecimal validAmount(BigDecimal amount) {
        BigDecimal normalized = MoneyUtils.normalize(amount);
        if (normalized.compareTo(MAX_AMOUNT) >= 0) {
            throw new BadRequestException("That amount is too large");
        }
        return normalized;
    }

    private ObligationResponse toResponse(Obligation o) {
        return new ObligationResponse(o.getId(), o.getName(), o.getAmount(), o.getCadence(), o.isActive());
    }
}
