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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class VenueFilterBuilder {

    public static final int CANDIDATE_BATCH_SIZE = 30;
    private static final Set<String> STRUCTURED_FEATURE_TAGS = Set.of(
            "natural-light", "daylight", "blackout", "loading", "loading-access",
            "parking", "accessible", "accessibility", "wheelchair-accessible",
            "lift", "elevator", "bathrooms", "sound-recording", "soundproof"
    );

    private final EntityManager entityManager;
    private final VenueHardRequirementEvaluator hardRequirementEvaluator;

    public List<Venue> findCandidateVenues(ParsedBrief brief) {
        return findCandidateBatch(brief, 0).candidates();
    }

    public CandidateBatch findCandidateBatch(ParsedBrief brief, int offset) {
        if (offset < 0) throw new IllegalArgumentException("Candidate offset must be nonnegative");
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

        addCrewCapacityFilter(brief, cb, venue, predicates);
        addProductionActivityFilter(brief, cb, venue, predicates);
        addNaturalLightFilter(brief, cb, venue, predicates);
        addBlackoutFilter(brief, cb, venue, predicates);
        addCeilingHeightFilter(brief, cb, venue, predicates);
        addLoadingFilter(brief, cb, venue, predicates);
        addParkingFilter(brief, cb, venue, predicates);
        addAccessibilityFilter(brief, cb, venue, predicates);
        addBathroomsFilter(brief, cb, venue, predicates);
        addSoundSuitabilityFilter(brief, cb, venue, predicates);

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

        List<Venue> rows = query.setFirstResult(offset).setMaxResults(CANDIDATE_BATCH_SIZE).getResultList();
        List<Venue> candidates = rows.stream()
                .filter(candidate -> hardRequirementEvaluator.isEligible(candidate, brief))
                .toList();

        log.info(
                "Venue filtering completed: candidates={}",
                candidates.size()
        );

        return new CandidateBatch(candidates, Math.addExact(offset, rows.size()), rows.size() == CANDIDATE_BATCH_SIZE);
    }

    public record CandidateBatch(List<Venue> candidates, int nextOffset, boolean hasMore) {}

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

            String normalizedTag = normalize(tag);
            if (STRUCTURED_FEATURE_TAGS.contains(normalizedTag)) {
                log.debug("Ignoring structured feature supplied as a must-have tag: {}", normalizedTag);
                continue;
            }

            predicates.add(
                    cb.isTrue(
                            cb.function(
                                    "array_contains",
                                    Boolean.class,
                                    venue.get("tags"),
                                    cb.literal(
                                            normalizedTag
                                    )
                            )
                    )
            );
        }
    }

    private void addCrewCapacityFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                       List<Predicate> predicates) {
        addMinimum(cb, venue, predicates, "maxCrewSize", brief.numPeople());
    }

    private void addProductionActivityFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                             List<Predicate> predicates) {
        if (brief.productionActivity() == null) return;
        Predicate permitted = cb.isTrue(cb.function("eema_activity_contains", Boolean.class,
                venue.get("permittedActivities"), cb.literal(brief.productionActivity().name())));
        Predicate notProhibited = cb.or(
                cb.isNull(venue.get("prohibitedActivities")),
                cb.isFalse(cb.function("eema_activity_contains", Boolean.class,
                        venue.get("prohibitedActivities"), cb.literal(brief.productionActivity().name())))
        );
        predicates.add(cb.and(cb.isNotNull(venue.get("permittedActivities")), permitted, notProhibited));
    }

    private void addNaturalLightFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                       List<Predicate> predicates) {
        addRequiredTrue(brief.requiresNaturalLight(), cb, venue, predicates, "naturalLight");
    }

    private void addBlackoutFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                   List<Predicate> predicates) {
        addRequiredTrue(brief.requiresBlackout(), cb, venue, predicates, "blackoutPossible");
    }

    private void addCeilingHeightFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                        List<Predicate> predicates) {
        addMinimum(cb, venue, predicates, "ceilingHeightM", brief.minimumCeilingHeightM());
    }

    private void addLoadingFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                  List<Predicate> predicates) {
        addRequiredTrue(brief.requiresLoadingAccess(), cb, venue, predicates, "loadingAccess");
    }

    private void addParkingFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                  List<Predicate> predicates) {
        if (Boolean.TRUE.equals(brief.requiresParking()) || brief.minimumParkingSpaces() != null) {
            addRequiredTrue(true, cb, venue, predicates, "parkingAvailable");
        }
        addMinimum(cb, venue, predicates, "parkingSpaces", brief.minimumParkingSpaces());
    }

    private void addAccessibilityFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                        List<Predicate> predicates) {
        addRequiredTrue(brief.requiresWheelchairAccessibility(), cb, venue, predicates, "wheelchairAccessible");
        addRequiredTrue(brief.requiresLift(), cb, venue, predicates, "liftAvailable");
    }

    private void addBathroomsFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                    List<Predicate> predicates) {
        addRequiredTrue(brief.requiresBathrooms(), cb, venue, predicates, "bathroomsAvailable");
    }

    private void addSoundSuitabilityFilter(ParsedBrief brief, CriteriaBuilder cb, Root<Venue> venue,
                                           List<Predicate> predicates) {
        addRequiredTrue(brief.requiresSoundRecordingSuitable(), cb, venue, predicates,
                "soundRecordingSuitable");
    }

    private void addRequiredTrue(Boolean required, CriteriaBuilder cb, Root<Venue> venue,
                                 List<Predicate> predicates, String field) {
        if (Boolean.TRUE.equals(required)) predicates.add(cb.isTrue(venue.get(field)));
    }

    private <T extends Comparable<? super T>> void addMinimum(CriteriaBuilder cb, Root<Venue> venue,
                                                               List<Predicate> predicates, String field, T minimum) {
        if (minimum != null) predicates.add(cb.greaterThanOrEqualTo(venue.get(field), minimum));
    }

    private List<Order> buildOrdering(
            CriteriaBuilder cb,
            Root<Venue> venue
    ) {
        return List.of(
                cb.asc(venue.get("hourlyRateEur")),
                cb.desc(
                        venue.get("sizeM2")
                ),
                cb.asc(
                        venue.get("name")
                ),
                cb.asc(venue.get("id"))
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
