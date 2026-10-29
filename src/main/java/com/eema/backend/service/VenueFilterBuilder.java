package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.service.ai.ParsedBrief;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueFilterBuilder {

    private static final int MAX_CANDIDATES = 30;

    private final EntityManager entityManager;

    public List<Venue> findCandidateVenues(ParsedBrief brief) {
        if (brief == null) {
            throw new IllegalArgumentException(
                    "Parsed brief must not be null"
            );
        }

        CriteriaBuilder cb =
                entityManager.getCriteriaBuilder();

        CriteriaQuery<Venue> cq =
                cb.createQuery(Venue.class);

        Root<Venue> venue =
                cq.from(Venue.class);

        List<Predicate> predicates =
                new ArrayList<>();

        addActiveFilter(
                cb,
                venue,
                predicates
        );

        addBudgetFilter(
                brief,
                cb,
                venue,
                predicates
        );

        addSizeFilter(
                brief,
                cb,
                venue,
                predicates
        );

        addNeighbourhoodFilter(
                brief,
                cb,
                venue,
                predicates
        );

        addMustHaveTagsFilter(
                brief,
                cb,
                venue,
                predicates
        );

        cq.where(
                predicates.toArray(
                        new Predicate[0]
                )
        );

        cq.orderBy(
                buildOrdering(cb, venue)
        );

        TypedQuery<Venue> query =
                entityManager.createQuery(cq);

        query.setMaxResults(
                MAX_CANDIDATES
        );

        List<Venue> candidates =
                query.getResultList();

        log.info(
                "Venue filtering completed: candidates={}",
                candidates.size()
        );

        return candidates;
    }

    private void addActiveFilter(
            CriteriaBuilder cb,
            Root<Venue> venue,
            List<Predicate> predicates
    ) {
        predicates.add(
                cb.equal(
                        venue.get("status"),
                        VenueStatus.ACTIVE
                )
        );
    }

    private void addBudgetFilter(
            ParsedBrief brief,
            CriteriaBuilder cb,
            Root<Venue> venue,
            List<Predicate> predicates
    ) {
        if (brief.budgetMaxEur() == null) {
            return;
        }

        predicates.add(
                cb.lessThanOrEqualTo(
                        venue.get("dayRateEur"),
                        brief.budgetMaxEur()
                )
        );
    }

    private void addSizeFilter(
            ParsedBrief brief,
            CriteriaBuilder cb,
            Root<Venue> venue,
            List<Predicate> predicates
    ) {
        if (brief.sizeM2Min() != null) {
            predicates.add(
                    cb.greaterThanOrEqualTo(
                            venue.get("sizeM2"),
                            brief.sizeM2Min()
                    )
            );
        }

        if (brief.sizeM2Max() != null) {
            predicates.add(
                    cb.lessThanOrEqualTo(
                            venue.get("sizeM2"),
                            brief.sizeM2Max()
                    )
            );
        }
    }

    private void addNeighbourhoodFilter(
            ParsedBrief brief,
            CriteriaBuilder cb,
            Root<Venue> venue,
            List<Predicate> predicates
    ) {
        List<String> options =
                brief.neighbourhoodOptions();

        if (options != null
                && !options.isEmpty()) {

            List<Predicate> optionPredicates =
                    options.stream()
                            .filter(this::hasText)
                            .map(this::normalize)
                            .map(option ->
                                    cb.equal(
                                            cb.lower(
                                                    venue.get(
                                                            "neighbourhood"
                                                    )
                                            ),
                                            option
                                    )
                            )
                            .toList();

            if (!optionPredicates.isEmpty()) {
                predicates.add(
                        cb.or(
                                optionPredicates.toArray(
                                        new Predicate[0]
                                )
                        )
                );
            }

            return;
        }

        if (hasText(brief.neighbourhood())) {
            predicates.add(
                    cb.equal(
                            cb.lower(
                                    venue.get(
                                            "neighbourhood"
                                    )
                            ),
                            normalize(
                                    brief.neighbourhood()
                            )
                    )
            );
        }
    }

    private void addMustHaveTagsFilter(
            ParsedBrief brief,
            CriteriaBuilder cb,
            Root<Venue> venue,
            List<Predicate> predicates
    ) {
        List<String> tags =
                brief.mustHaveTags();

        if (tags == null || tags.isEmpty()) {
            return;
        }

        for (String tag : tags) {
            if (!hasText(tag)) {
                continue;
            }

            predicates.add(
                    cb.isTrue(
                            cb.function(
                                    "array_contains",
                                    Boolean.class,
                                    venue.get("tags"),
                                    cb.literal(
                                            normalize(tag)
                                    )
                            )
                    )
            );
        }
    }

    private List<Order> buildOrdering(
            CriteriaBuilder cb,
            Root<Venue> venue
    ) {
        return List.of(
                cb.asc(
                        venue.get("dayRateEur")
                ),
                cb.desc(
                        venue.get("sizeM2")
                ),
                cb.asc(
                        venue.get("name")
                )
        );
    }

    private boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }

    private String normalize(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}