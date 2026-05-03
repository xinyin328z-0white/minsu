package com.example.minsumgr.service;

import com.example.minsumgr.domain.Booking;
import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.domain.Route;
import com.example.minsumgr.domain.UserPreference;
import com.example.minsumgr.dto.ForYouRecommendationResponse;
import com.example.minsumgr.dto.UserPreferenceRequest;
import com.example.minsumgr.repository.BookingRepository;
import com.example.minsumgr.repository.HomestayRepository;
import com.example.minsumgr.repository.RouteRepository;
import com.example.minsumgr.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final List<String> STYLE_KEYWORDS = List.of(
            "温泉", "休闲", "养生", "户外", "骑行", "漂流", "冒险", "文化",
            "古镇", "亲子", "山水", "美食", "安静", "河景", "山景"
    );

    private final UserPreferenceRepository userPreferenceRepository;
    private final BookingRepository bookingRepository;
    private final HomestayRepository homestayRepository;
    private final RouteRepository routeRepository;

    public RecommendationService(UserPreferenceRepository userPreferenceRepository,
                                 BookingRepository bookingRepository,
                                 HomestayRepository homestayRepository,
                                 RouteRepository routeRepository) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.bookingRepository = bookingRepository;
        this.homestayRepository = homestayRepository;
        this.routeRepository = routeRepository;
    }

    public Optional<UserPreference> getPreference(Long userId) {
        return userPreferenceRepository.findByUserId(userId);
    }

    public UserPreference savePreference(Long userId, UserPreferenceRequest request) {
        LocalDateTime now = LocalDateTime.now();
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(UserPreference::new);

        if (preference.getId() == null) {
            preference.setUserId(userId);
            preference.setCreatedAt(now);
        }
        preference.setBudgetLevel(trimToNull(request.getBudgetLevel()));
        preference.setTravelStyles(joinValues(request.getTravelStyles()));
        preference.setPreferredLocations(joinValues(request.getPreferredLocations()));
        preference.setMinPrice(request.getMinPrice());
        preference.setMaxPrice(request.getMaxPrice());
        preference.setCompanionType(trimToNull(request.getCompanionType()));
        preference.setNotes(trimToNull(request.getNotes()));
        preference.setUpdatedAt(now);

        return userPreferenceRepository.save(preference);
    }

    public ForYouRecommendationResponse recommendForUser(Long userId) {
        List<Homestay> homestays = homestayRepository.findAll();
        List<Route> routes = routeRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<UserPreference> preferences = userPreferenceRepository.findAll();

        Map<Long, Homestay> homestayById = homestays.stream()
                .collect(Collectors.toMap(Homestay::getId, Function.identity()));
        Map<Long, List<Booking>> bookingsByUser = bookings.stream()
                .filter(b -> b.getUserId() != null)
                .collect(Collectors.groupingBy(Booking::getUserId));
        Map<Long, UserPreference> preferenceByUser = preferences.stream()
                .filter(p -> p.getUserId() != null)
                .collect(Collectors.toMap(UserPreference::getUserId, Function.identity()));

        UserPreference targetPreference = preferenceByUser.get(userId);
        Map<String, Double> targetVector = buildUserVector(
                targetPreference,
                bookingsByUser.getOrDefault(userId, List.of()),
                homestayById
        );

        Map<Long, Double> homestayScores = new HashMap<>();
        Map<Long, Double> homestayCfScores = new HashMap<>();
        Map<Long, Double> routeScores = new HashMap<>();
        Map<Long, Double> routeCfScores = new HashMap<>();

        homestays.forEach(h -> homestayScores.put(h.getId(), 0.0));
        routes.forEach(r -> routeScores.put(r.getId(), 0.0));

        for (Long otherUserId : collectCandidateUserIds(bookingsByUser, preferenceByUser)) {
            if (Objects.equals(otherUserId, userId)) {
                continue;
            }

            UserPreference otherPreference = preferenceByUser.get(otherUserId);
            List<Booking> otherBookings = bookingsByUser.getOrDefault(otherUserId, List.of());
            Map<String, Double> otherVector = buildUserVector(otherPreference, otherBookings, homestayById);
            double similarity = cosineSimilarity(targetVector, otherVector);
            if (similarity <= 0) {
                continue;
            }

            for (Booking booking : otherBookings) {
                Homestay homestay = homestayById.get(booking.getHomestayId());
                if (homestay == null) {
                    continue;
                }
                double value = similarity * bookingWeight(booking);
                addScore(homestayScores, homestay.getId(), value);
                addScore(homestayCfScores, homestay.getId(), value);
            }

            for (Route route : routes) {
                double affinity = scoreRouteAgainstPreference(route, otherPreference)
                        + scoreRouteAgainstBookings(route, otherBookings, homestayById) * 0.45;
                if (affinity > 0) {
                    double value = similarity * affinity;
                    addScore(routeScores, route.getId(), value);
                    addScore(routeCfScores, route.getId(), value);
                }
            }
        }

        Map<Long, Long> homestayPopularity = bookings.stream()
                .filter(b -> b.getHomestayId() != null)
                .collect(Collectors.groupingBy(Booking::getHomestayId, Collectors.counting()));
        long maxPopularity = homestayPopularity.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1L);

        for (Homestay homestay : homestays) {
            double preferenceScore = scoreHomestayAgainstPreference(homestay, targetPreference);
            double popularityScore = homestayPopularity.getOrDefault(homestay.getId(), 0L) * 0.8 / maxPopularity;
            addScore(homestayScores, homestay.getId(), preferenceScore + popularityScore);
        }

        for (Route route : routes) {
            double preferenceScore = scoreRouteAgainstPreference(route, targetPreference);
            addScore(routeScores, route.getId(), preferenceScore);
        }

        List<ForYouRecommendationResponse.HomestayRecommendation> homestayRecommendations = homestays.stream()
                .sorted(Comparator
                        .comparingDouble((Homestay h) -> homestayScores.getOrDefault(h.getId(), 0.0))
                        .reversed()
                        .thenComparing(h -> Optional.ofNullable(h.getPricePerNight()).orElse(BigDecimal.ZERO)))
                .limit(4)
                .map(h -> toHomestayRecommendation(
                        h,
                        homestayScores.getOrDefault(h.getId(), 0.0),
                        homestayCfScores.getOrDefault(h.getId(), 0.0),
                        targetPreference,
                        homestayPopularity.getOrDefault(h.getId(), 0L) > 0
                ))
                .collect(Collectors.toList());

        List<ForYouRecommendationResponse.RouteRecommendation> routeRecommendations = routes.stream()
                .sorted(Comparator
                        .comparingDouble((Route r) -> routeScores.getOrDefault(r.getId(), 0.0))
                        .reversed())
                .limit(4)
                .map(r -> toRouteRecommendation(
                        r,
                        routeScores.getOrDefault(r.getId(), 0.0),
                        routeCfScores.getOrDefault(r.getId(), 0.0),
                        targetPreference
                ))
                .collect(Collectors.toList());

        ForYouRecommendationResponse response = new ForYouRecommendationResponse();
        response.setPreferenceMissing(targetPreference == null);
        response.setPreference(targetPreference);
        response.setAlgorithm("用户协同过滤：根据用户偏好和订单行为构建向量，使用余弦相似度寻找相似用户，再结合相似用户选择、路线标签匹配和自身偏好进行排序。");
        response.setSummary(buildSummary(targetPreference, homestayRecommendations, routeRecommendations));
        response.setHomestays(homestayRecommendations);
        response.setRoutes(routeRecommendations);
        return response;
    }

    private Set<Long> collectCandidateUserIds(Map<Long, List<Booking>> bookingsByUser,
                                              Map<Long, UserPreference> preferenceByUser) {
        Set<Long> ids = new HashSet<>();
        ids.addAll(bookingsByUser.keySet());
        ids.addAll(preferenceByUser.keySet());
        return ids;
    }

    private Map<String, Double> buildUserVector(UserPreference preference,
                                                List<Booking> bookings,
                                                Map<Long, Homestay> homestayById) {
        Map<String, Double> vector = new HashMap<>();

        if (preference != null) {
            addFeature(vector, "budget:" + normalize(preference.getBudgetLevel()), 2.0);
            addFeature(vector, "companion:" + normalize(preference.getCompanionType()), 1.2);
            for (String style : splitValues(preference.getTravelStyles())) {
                addFeature(vector, "style:" + normalize(style), 3.0);
            }
            for (String location : splitValues(preference.getPreferredLocations())) {
                addFeature(vector, "location:" + normalize(location), 2.0);
            }
            addStyleFeatures(vector, preference.getNotes(), 1.0);
        }

        for (Booking booking : bookings) {
            Homestay homestay = homestayById.get(booking.getHomestayId());
            if (homestay == null) {
                continue;
            }
            double weight = bookingWeight(booking);
            addFeature(vector, "homestay:" + homestay.getId(), weight * 3.5);
            addFeature(vector, "location:" + normalize(homestay.getLocation()), weight * 1.2);
            addFeature(vector, "price:" + priceBand(homestay.getPricePerNight()), weight);
            addStyleFeatures(vector, joinText(homestay.getName(), homestay.getLocation(), homestay.getDescription(), homestay.getRoomNotes()), weight);
        }

        return vector;
    }

    private double scoreHomestayAgainstPreference(Homestay homestay, UserPreference preference) {
        if (preference == null) {
            return 0.6;
        }

        double score = 0.0;
        String text = joinText(homestay.getName(), homestay.getLocation(), homestay.getDescription(), homestay.getRoomNotes());

        for (String style : splitValues(preference.getTravelStyles())) {
            if (containsIgnoreCase(text, style)) {
                score += 2.0;
            }
        }

        for (String location : splitValues(preference.getPreferredLocations())) {
            if (containsIgnoreCase(homestay.getLocation(), location)
                    || containsIgnoreCase(location, homestay.getLocation())) {
                score += 1.8;
            }
        }

        if (matchesBudget(homestay.getPricePerNight(), preference)) {
            score += 1.4;
        }

        if (containsAny(text, splitValues(preference.getNotes()))) {
            score += 0.8;
        }

        return score;
    }

    private double scoreRouteAgainstPreference(Route route, UserPreference preference) {
        if (preference == null) {
            return 0.5;
        }

        double score = 0.0;
        String text = joinText(route.getName(), route.getTags(), route.getDescription());

        for (String style : splitValues(preference.getTravelStyles())) {
            if (containsIgnoreCase(text, style)) {
                score += 2.0;
            }
        }

        for (String location : splitValues(preference.getPreferredLocations())) {
            if (containsIgnoreCase(text, location)) {
                score += 1.0;
            }
        }

        if (containsAny(text, splitValues(preference.getNotes()))) {
            score += 0.8;
        }

        return score;
    }

    private double scoreRouteAgainstBookings(Route route,
                                             List<Booking> bookings,
                                             Map<Long, Homestay> homestayById) {
        String routeText = joinText(route.getName(), route.getTags(), route.getDescription());
        double score = 0.0;
        for (Booking booking : bookings) {
            Homestay homestay = homestayById.get(booking.getHomestayId());
            if (homestay == null) {
                continue;
            }
            String homestayText = joinText(homestay.getName(), homestay.getLocation(), homestay.getDescription());
            for (String keyword : STYLE_KEYWORDS) {
                if (containsIgnoreCase(routeText, keyword) && containsIgnoreCase(homestayText, keyword)) {
                    score += bookingWeight(booking) * 0.5;
                }
            }
        }
        return score;
    }

    private ForYouRecommendationResponse.HomestayRecommendation toHomestayRecommendation(
            Homestay homestay,
            double score,
            double cfScore,
            UserPreference preference,
            boolean popular
    ) {
        ForYouRecommendationResponse.HomestayRecommendation item = new ForYouRecommendationResponse.HomestayRecommendation();
        item.setId(homestay.getId());
        item.setName(homestay.getName());
        item.setLocation(homestay.getLocation());
        item.setDescription(homestay.getDescription());
        item.setPricePerNight(homestay.getPricePerNight());
        item.setAvailableRooms(homestay.getAvailableRooms());
        item.setImageUrl(homestay.getImageUrl());
        item.setScore(roundScore(score));
        item.setReason(buildHomestayReason(homestay, cfScore, preference, popular));
        return item;
    }

    private ForYouRecommendationResponse.RouteRecommendation toRouteRecommendation(
            Route route,
            double score,
            double cfScore,
            UserPreference preference
    ) {
        ForYouRecommendationResponse.RouteRecommendation item = new ForYouRecommendationResponse.RouteRecommendation();
        item.setId(route.getId());
        item.setName(route.getName());
        item.setDescription(route.getDescription());
        item.setTags(route.getTags());
        item.setScore(roundScore(score));
        item.setReason(buildRouteReason(route, cfScore, preference));
        return item;
    }

    private String buildHomestayReason(Homestay homestay,
                                       double cfScore,
                                       UserPreference preference,
                                       boolean popular) {
        List<String> reasons = new ArrayList<>();
        if (cfScore > 0) {
            reasons.add("相似偏好的用户选择过这类民宿");
        }
        List<String> matchedStyles = matchedValues(
                joinText(homestay.getName(), homestay.getLocation(), homestay.getDescription(), homestay.getRoomNotes()),
                preference == null ? "" : preference.getTravelStyles()
        );
        if (!matchedStyles.isEmpty()) {
            reasons.add("匹配偏好：" + String.join("、", matchedStyles));
        }
        List<String> matchedLocations = matchedValues(homestay.getLocation(), preference == null ? "" : preference.getPreferredLocations());
        if (!matchedLocations.isEmpty()) {
            reasons.add("位置符合：" + String.join("、", matchedLocations));
        }
        if (preference != null && matchesBudget(homestay.getPricePerNight(), preference)) {
            reasons.add("价格在你的预算范围内");
        }
        if (popular) {
            reasons.add("近期订单热度较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("根据整体热度和资源信息排序推荐");
        }
        return String.join("；", reasons);
    }

    private String buildRouteReason(Route route, double cfScore, UserPreference preference) {
        List<String> reasons = new ArrayList<>();
        if (cfScore > 0) {
            reasons.add("相似偏好的用户更可能选择这类线路");
        }
        List<String> matchedStyles = matchedValues(
                joinText(route.getName(), route.getTags(), route.getDescription()),
                preference == null ? "" : preference.getTravelStyles()
        );
        if (!matchedStyles.isEmpty()) {
            reasons.add("线路标签匹配：" + String.join("、", matchedStyles));
        }
        if (reasons.isEmpty()) {
            reasons.add("结合路线标签、描述和用户偏好综合推荐");
        }
        return String.join("；", reasons);
    }

    private String buildSummary(UserPreference preference,
                                List<ForYouRecommendationResponse.HomestayRecommendation> homestays,
                                List<ForYouRecommendationResponse.RouteRecommendation> routes) {
        if (preference == null) {
            return "请先完善偏好，系统当前按订单热度和基础资源做兜底推荐。";
        }
        String topHomestay = homestays.isEmpty() ? "暂无民宿" : homestays.get(0).getName();
        String topRoute = routes.isEmpty() ? "暂无线路" : routes.get(0).getName();
        return "已根据你的偏好和相似用户行为推荐：" + topHomestay + " 与 " + topRoute + "。";
    }

    private double cosineSimilarity(Map<String, Double> left, Map<String, Double> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }

        double dot = 0.0;
        for (Map.Entry<String, Double> entry : left.entrySet()) {
            dot += entry.getValue() * right.getOrDefault(entry.getKey(), 0.0);
        }
        double leftNorm = Math.sqrt(left.values().stream().mapToDouble(v -> v * v).sum());
        double rightNorm = Math.sqrt(right.values().stream().mapToDouble(v -> v * v).sum());
        if (leftNorm == 0 || rightNorm == 0) {
            return 0.0;
        }
        return dot / (leftNorm * rightNorm);
    }

    private double bookingWeight(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return 2.0;
        }
        return switch (booking.getStatus()) {
            case "COMPLETED" -> 5.0;
            case "CONFIRMED" -> 4.0;
            case "PENDING" -> 2.0;
            case "CANCELED" -> 0.4;
            default -> 1.0;
        };
    }

    private boolean matchesBudget(BigDecimal price, UserPreference preference) {
        if (price == null || preference == null) {
            return false;
        }

        BigDecimal minPrice = preference.getMinPrice() == null ? null : BigDecimal.valueOf(preference.getMinPrice());
        BigDecimal maxPrice = preference.getMaxPrice() == null ? null : BigDecimal.valueOf(preference.getMaxPrice());
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }

        String level = normalize(preference.getBudgetLevel());
        if (level.isBlank()) {
            return true;
        }
        return switch (level) {
            case "economy" -> price.compareTo(BigDecimal.valueOf(300)) <= 0;
            case "moderate" -> price.compareTo(BigDecimal.valueOf(260)) >= 0
                    && price.compareTo(BigDecimal.valueOf(420)) <= 0;
            case "luxury" -> price.compareTo(BigDecimal.valueOf(350)) >= 0;
            default -> true;
        };
    }

    private String priceBand(BigDecimal price) {
        if (price == null) {
            return "unknown";
        }
        if (price.compareTo(BigDecimal.valueOf(300)) <= 0) {
            return "economy";
        }
        if (price.compareTo(BigDecimal.valueOf(420)) <= 0) {
            return "moderate";
        }
        return "luxury";
    }

    private void addStyleFeatures(Map<String, Double> vector, String text, double weight) {
        for (String keyword : STYLE_KEYWORDS) {
            if (containsIgnoreCase(text, keyword)) {
                addFeature(vector, "style:" + normalize(keyword), weight);
            }
        }
    }

    private List<String> matchedValues(String text, String values) {
        return splitValues(values).stream()
                .filter(value -> containsIgnoreCase(text, value))
                .collect(Collectors.toList());
    }

    private boolean containsAny(String text, List<String> values) {
        return values.stream().anyMatch(value -> containsIgnoreCase(text, value));
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT).trim());
    }

    private void addFeature(Map<String, Double> vector, String key, double value) {
        if (key == null || key.endsWith(":") || value == 0) {
            return;
        }
        vector.merge(key, value, Double::sum);
    }

    private void addScore(Map<Long, Double> scores, Long id, double value) {
        if (id == null || value == 0) {
            return;
        }
        scores.merge(id, value, Double::sum);
    }

    private Double roundScore(double score) {
        return BigDecimal.valueOf(score)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private String joinValues(List<String> values) {
        if (values == null) {
            return null;
        }
        String joined = values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));
        return joined.isBlank() ? null : joined;
    }

    private List<String> splitValues(String values) {
        if (values == null || values.isBlank()) {
            return List.of();
        }
        return Arrays.stream(values.split("[,，\\s]+"))
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private String joinText(String... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
