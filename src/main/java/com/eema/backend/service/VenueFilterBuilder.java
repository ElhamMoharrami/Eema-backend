package com.eema.backend.service;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.VenueStatus;
import com.eema.backend.service.ai.ParsedBrief;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reusable component that builds a database-level query from a ParsedBrief.
 *
 * Example usage:
 * <pre>
 *   @Service
 *   @RequiredArgsConstructor
 *   public class VenueFilterBuilder {
 *
 *       private final VenueRepository venueRepository;
 *       private final EntityManager entityManager; // for CriteriaBuilder
 *       private static final Logger log = LoggerFactory.getLogger(VenueFilterBuilder.class);
 *
 *       public List<Venue> findCandidateVenues(ParsedBrief brief) {
 *           // 1. Build Specification<Venue> spec = Specification.where(...)
 *           //    – add each non‑null brief field as a predicate.
 *           // 2. Apply sorting: Sort.by(Sort.Order.asc("dayRateEur"),
 *           //                        Sort.Order.desc("sizeM2"),
 *           //                        Sort.Order.asc("name"));
 *           // 3. Pageable page = PageRequest.of(0, 15, sort);
 *           // 4. List<Venue> result = venueRepository.findAll(spec, page).getContent();
 *           // 5. Log if result.isEmpty().
 *           // 6. Return result.
 *       }
 *   }
 * </pre>
 * <p>
 * The actual implementation uses the JPA Criteria API directly for flexibility.
 */
@Service
@RequiredArgsConstructor
public class VenueFilterBuilder {

    private static final Logger log = LoggerFactory.getLogger(VenueFilterBuilder.class);

    private final EntityManager entityManager;

    public List<Venue> findCandidateVenues(ParsedBrief brief) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Venue> cq = cb.createQuery(Venue.class);
        Root<Venue> root = cq.from(Venue.class);

        List<Predicate> predicates = new ArrayList<>();

        // Only consider active venues
        predicates.add(cb.equal(root.get("status"), VenueStatus.ACTIVE));

        // Budget: dayRateEur <= budgetPerDay (if budgetPerDay is not null)
        if (brief.budgetPerDay() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("dayRateEur"), brief.budgetPerDay()));
        }

        // Neighbourhood: neighbourhood ILIKE '%{value}%' (case-insensitive contains)
        if (brief.neighbourhood() != null && !brief.neighbourhood().isBlank()) {
            String neighbourhoodPattern = "%" + brief.neighbourhood().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("neighbourhood")), neighbourhoodPattern));
        }

        // Size: sizeM2 >= sizeM2 (if sizeM2 is not null)
        if (brief.sizeM2() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("sizeM2"), brief.sizeM2()));
        }

        // Indoor/Outdoor: Not directly in Venue. We'll skip for now, but note that we might need to add a field to Venue.
        // For now, we ignore.

        // Tags: For each tag in brief.tags(), we require that the venue's tags list contains the tag.
        // Since tags are stored as a PostgreSQL array (text[]), we can use the 'array_contains' function.
        if (brief.tags() != null && !brief.tags().isEmpty()) {
            for (String tag : brief.tags()) {
                // Check that the tags array contains the given tag using PostgreSQL's array_contains function
                predicates.add(cb.equal(
                        cb.function("array_contains", Boolean.class, root.get("tags"), cb.literal(tag)),
                        true));
            }
        }

        // Other fields: we can add more as needed (e.g., indoorOutdoor, aesthetic, etc.) if they are mapped to Venue fields.

        // Add all predicates to the criteria query
        cq.where(predicates.toArray(new Predicate[0]));

        // Ordering: lower dayRateEur (cheaper first), larger sizeM2 (bigger spaces first), then name
        List<Order> orders = new ArrayList<>();
        orders.add(cb.asc(root.get("dayRateEur")));
        orders.add(cb.desc(root.get("sizeM2")));
        orders.add(cb.asc(root.get("name")));
        cq.orderBy(orders);

        // Create the query
        TypedQuery<Venue> query = entityManager.createQuery(cq);
        // Limit to 15 results (set first result to 0 and max results to 15)
        query.setFirstResult(0);
        query.setMaxResults(15);

        List<Venue> result = query.getResultList();

        if (result.isEmpty()) {
            log.warn("No venues found matching the brief: {}", brief);
        }

        return result;
    }
}