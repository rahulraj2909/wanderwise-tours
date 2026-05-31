package com.neovarsity.toursattractions.config;

import com.neovarsity.toursattractions.entity.*;
import com.neovarsity.toursattractions.entity.enums.AttractionType;
import com.neovarsity.toursattractions.entity.enums.UserRole;
import com.neovarsity.toursattractions.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@Profile({"dev", "h2", "full", "render"})
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "demo123";

    private final CityRepository cityRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AttractionRepository attractionRepository;
    private final PaxTypeRepository paxTypeRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (cityRepository.findByCodeIgnoreCase("CTSINGAP").isPresent()) {
            refreshCatalogImages();
            if (attractionRepository.count() > 0) {
                return;
            }
            System.out.println("=== Catalog has cities but no products — re-seeding attractions ===");
        }
        seedCatalog();
    }

    private void seedCatalog() {
        String encodedPassword = passwordEncoder.encode(DEMO_PASSWORD);
        User operator = ensureOperator(encodedPassword);

        Category adventure = ensureCategory("Adventure", "Thrilling outdoor experiences");
        Category culture = ensureCategory("Culture & Heritage", "Museums, monuments and guided walks");
        Category water = ensureCategory("Water Activities", "Cruises, diving and water parks");
        Category family = ensureCategory("Family Fun", "Parks, shows and kid-friendly tours");
        Category food = ensureCategory("Food & Nightlife", "Tasting tours, cruises and city nights");

        backfillLegacyCityCodes();

        Map<String, City> cities = seedCities();
        int before = (int) attractionRepository.count();

        seedAttractions(cities, adventure, culture, water, family, food, operator);

        int added = (int) attractionRepository.count() - before;
        seedTimeSlotsForNewAttractions(before);

        seedDefaultPaxTypes();
        System.out.println("=== Catalog seeded: " + added + " new attractions ===");
        System.out.println("Customer UI: http://localhost:8080/");
        System.out.println("Admin UI: http://localhost:8081/admin/login.html");
        System.out.println("Booking login: customer@tours.demo / " + DEMO_PASSWORD);
    }

    private void backfillLegacyCityCodes() {
        Map<String, String> legacy = Map.of(
                "Dubai", "CTDUBAI",
                "Paris", "CTPARIS",
                "Goa", "CTGOA");
        cityRepository.findAll().forEach(c -> {
            String code = legacy.get(c.getName());
            if (code != null) {
                c.setCode(code);
                cityRepository.save(c);
            }
        });
    }

    private Map<String, City> seedCities() {
        Map<String, City> map = new LinkedHashMap<>();
        map.put("CTDUBAI", saveCity("Dubai", "CTDUBAI", "UAE",
                "Luxury tours and desert adventures",
                "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800"));
        map.put("CTPARIS", saveCity("Paris", "CTPARIS", "France",
                "Museums, landmarks and Seine cruises",
                "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"));
        map.put("CTGOA", saveCity("Goa", "CTGOA", "India",
                "Beaches, water sports and heritage walks",
                "https://images.unsplash.com/photo-1518544889285-4e9f9c9c0c1e?w=800"));
        map.put("CTSINGAP", saveCity("Singapore", "CTSINGAP", "Singapore",
                "Gardens, Sentosa, river cruises and street food",
                "https://images.unsplash.com/photo-1525627279424-8e0c1d0a0a0a?w=800"));
        map.put("CTBANGKOK", saveCity("Bangkok", "CTBANGKOK", "Thailand",
                "Temples, floating markets and dinner cruises",
                "https://images.unsplash.com/photo-1563492065599-3520f775eeed?w=800"));
        map.put("CTLONDON", saveCity("London", "CTLONDON", "United Kingdom",
                "Royal sights, Thames cruises and West End",
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800"));
        map.put("CTBALI", saveCity("Bali", "CTBALI", "Indonesia",
                "Ubud culture, beaches and volcano sunrise treks",
                "https://images.unsplash.com/photo-1537996194471-e657df962ab4?w=800"));
        map.put("CTTOKYO", saveCity("Tokyo", "CTTOKYO", "Japan",
                "Anime districts, sushi tours and Mt Fuji day trips",
                "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800"));
        return map;
    }

    private City saveCity(String name, String code, String country, String description, String imageUrl) {
        return cityRepository.findByCodeIgnoreCase(code)
                .orElseGet(() -> cityRepository.findByNameIgnoreCase(name)
                        .map(existing -> {
                            existing.setCode(code);
                            existing.setCountry(country);
                            existing.setDescription(description);
                            existing.setImageUrl(imageUrl);
                            return cityRepository.save(existing);
                        })
                        .orElseGet(() -> cityRepository.save(City.builder()
                                .name(name)
                                .code(code)
                                .country(country)
                                .description(description)
                                .imageUrl(imageUrl)
                                .build())));
    }

    private Category ensureCategory(String name, String description) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .name(name)
                        .description(description)
                        .build()));
    }

    private User ensureOperator(String encodedPassword) {
        return userRepository.findByEmail("operator@tours.demo")
                .orElseGet(() -> userRepository.save(User.builder()
                        .name("Desert Trails Operator")
                        .email("operator@tours.demo")
                        .password(encodedPassword)
                        .role(UserRole.OPERATOR)
                        .build()));
    }

    private void seedAttractions(Map<String, City> cities, Category adventure, Category culture,
                                 Category water, Category family, Category food, User operator) {
        City dubai = cities.get("CTDUBAI");
        City paris = cities.get("CTPARIS");
        City goa = cities.get("CTGOA");
        City singapore = cities.get("CTSINGAP");
        City bangkok = cities.get("CTBANGKOK");
        City london = cities.get("CTLONDON");
        City bali = cities.get("CTBALI");
        City tokyo = cities.get("CTTOKYO");

        saveIfAbsent("Dubai Evening Desert Safari with BBQ Dinner", dubai, adventure, operator,
                AttractionType.ADVENTURE, "2499", 6, 15, "4.70", 128,
                "Dune bashing, camel ride, sandboarding and live entertainment with BBQ buffet.",
                "https://images.unsplash.com/photo-1451337516015-6b562e9e7b7c?w=800");
        saveIfAbsent("Burj Khalifa At the Top + Fountain Show", dubai, culture, null,
                AttractionType.ATTRACTION, "3899", 3, 25, "4.82", 512,
                "Level 124 observation deck with optional fountain lake show tickets.",
                "https://images.unsplash.com/photo-1518684079-3c830dcef090?w=800");
        saveIfAbsent("Dubai Marina Dhow Dinner Cruise", dubai, food, null,
                AttractionType.TOUR, "1999", 3, 40, "4.55", 203,
                "International buffet on a traditional dhow with skyline views.",
                "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800");
        saveIfAbsent("IMG Worlds of Adventure Day Pass", dubai, family, null,
                AttractionType.ACTIVITY, "4299", 8, 50, "4.48", 167,
                "Full-day access to Marvel, Cartoon Network and dinosaur zones.",
                "https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=800");
        saveIfAbsent("Morning Hot Air Balloon Flight - Dubai Desert", dubai, adventure, operator,
                AttractionType.ADVENTURE, "8999", 4, 12, "4.91", 89,
                "Sunrise balloon ride with falcon photo and gourmet breakfast.",
                productImg("dubai-hot-air-balloon"));
        saveIfAbsent("Abu Dhabi Grand Mosque & Louvre Day Trip", dubai, culture, null,
                AttractionType.DAY_TRIP, "5499", 10, 18, "4.76", 245,
                "Guided visit to Sheikh Zayed Mosque and Louvre Abu Dhabi.",
                "https://images.unsplash.com/photo-1518684079-3c830dcef090?w=800");

        saveIfAbsent("Eiffel Tower Skip-the-Line Guided Tour", paris, culture, null,
                AttractionType.ATTRACTION, "4599", 2, 20, "4.85", 342,
                "Priority access to 2nd floor with expert guide.",
                "https://images.unsplash.com/photo-1511739001481-6370c694c6a3?w=800");
        saveIfAbsent("Louvre Museum Masterpieces Tour", paris, culture, null,
                AttractionType.ATTRACTION, "3299", 3, 15, "4.79", 278,
                "Skip-the-line entry with guide to Mona Lisa and highlights.",
                "https://images.unsplash.com/photo-1499856871958-5b9627525d0b?w=800");
        saveIfAbsent("Seine River Dinner Cruise with Live Music", paris, food, null,
                AttractionType.TOUR, "5999", 2, 30, "4.72", 190,
                "Glass-roof boat, 3-course French dinner and live band.",
                "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800");
        saveIfAbsent("Versailles Palace & Gardens Half-Day", paris, culture, null,
                AttractionType.DAY_TRIP, "4999", 5, 22, "4.68", 156,
                "Audio-guided palace rooms and free time in gardens.",
                "https://images.unsplash.com/photo-1550340490-a6d60e3219fe?w=800");
        saveIfAbsent("Montmartre Walking Food Tour", paris, food, null,
                AttractionType.TOUR, "3799", 3, 12, "4.81", 94,
                "Cheese, pastries and wine tastings in hidden alleys.",
                "https://images.unsplash.com/photo-1509440159626-092c3eb9e5fe?w=800");

        saveIfAbsent("Grand Island Scuba Diving Experience - Goa", goa, water, null,
                AttractionType.ACTIVITY, "3499", 4, 8, "4.60", 89,
                "Beginner-friendly scuba with certified instructor and boat transfer.",
                "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800");
        saveIfAbsent("North Goa Beach Hop & Water Sports Combo", goa, water, null,
                AttractionType.ACTIVITY, "2799", 7, 10, "4.52", 134,
                "Parasailing, jet ski and banana boat at Baga and Calangute.",
                "https://images.unsplash.com/photo-1518544889285-4e9f9c9c0c1e?w=800");
        saveIfAbsent("Old Goa Heritage Walk with Spice Plantation", goa, culture, null,
                AttractionType.TOUR, "1899", 6, 16, "4.58", 67,
                "UNESCO churches plus traditional spice farm lunch.",
                "https://images.unsplash.com/photo-1585137139862-1a8b09f0c8e8?w=800");
        saveIfAbsent("Dudhsagar Falls Jeep Safari", goa, adventure, null,
                AttractionType.DAY_TRIP, "3999", 10, 14, "4.64", 112,
                "4x4 jungle ride to India's fifth tallest waterfall.",
                "https://images.unsplash.com/photo-1593693391250-4b2b0b0b0b0b?w=800");

        // Singapore — rich listing for pagination (12+ tours); unique Unsplash image per card
        saveIfAbsent("Gardens by the Bay + Cloud Forest & Flower Dome", singapore, family, null,
                AttractionType.ATTRACTION, "2199", 4, 30, "4.88", 891,
                "Combo tickets to Supertree Grove, Cloud Forest and Flower Dome.",
                "https://images.unsplash.com/photo-1528183429752-aca21b42c10c?w=800&q=80");
        saveIfAbsent("Universal Studios Singapore 1-Day Pass", singapore, family, null,
                AttractionType.ACTIVITY, "5899", 9, 40, "4.83", 1204,
                "All rides and shows including Transformers and Jurassic Park.",
                "https://images.unsplash.com/photo-1598110755015-1a2d6a3f4592?w=800&q=80");
        saveIfAbsent("Singapore River Cruise & Clarke Quay Night Tour", singapore, food, null,
                AttractionType.TOUR, "1699", 3, 25, "4.61", 445,
                "Bumboat cruise with optional riverside dinner stop.",
                "https://images.unsplash.com/photo-1565967513811-9b1c40e0b50a?w=800&q=80");
        saveIfAbsent("Sentosa Island Adventure Pass (Cable Car + Luge)", singapore, adventure, null,
                AttractionType.ACTIVITY, "4499", 7, 20, "4.74", 678,
                "Cable car, Skyline Luge and beach club access.",
                "https://images.unsplash.com/photo-1559628232-d1c427c07599?w=800&q=80");
        saveIfAbsent("Night Safari & Tram Ride with Priority Entry", singapore, family, null,
                AttractionType.ATTRACTION, "3299", 4, 28, "4.86", 956,
                "World's first nocturnal wildlife park with guided tram.",
                "https://images.unsplash.com/photo-1549368815-a8d0c96fdc798?w=800&q=80");
        saveIfAbsent("Marina Bay Sands SkyPark Observation Deck", singapore, culture, null,
                AttractionType.ATTRACTION, "2499", 2, 35, "4.80", 723,
                "Iconic infinity-edge deck views over the skyline.",
                "https://images.unsplash.com/photo-1613498802570-94a04eece583?w=800&q=80");
        saveIfAbsent("Chinatown Street Food Walking Tour", singapore, food, null,
                AttractionType.TOUR, "1999", 3, 14, "4.77", 312,
                "Hawker favourites: chicken rice, laksa and kaya toast.",
                "https://images.unsplash.com/photo-1559339352-11d035aa65de?w=800&q=80");
        saveIfAbsent("Singapore Flyer + Time Capsule Experience", singapore, family, null,
                AttractionType.ATTRACTION, "2799", 2, 32, "4.59", 198,
                "Asia's largest observation wheel with multimedia journey.",
                "https://images.unsplash.com/photo-1580667099867-1e34475e65e6?w=800&q=80");
        saveIfAbsent("Pulau Ubin Cycling & Mangrove Kayak", singapore, adventure, null,
                AttractionType.DAY_TRIP, "3599", 6, 12, "4.71", 87,
                "Rustic island bike trail and sheltered mangrove paddle.",
                "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&q=80");
        saveIfAbsent("ArtScience Museum Future World Exhibition", singapore, culture, null,
                AttractionType.ATTRACTION, "1899", 2, 40, "4.65", 401,
                "TeamLab-style immersive digital art installations.",
                "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=800&q=80");
        saveIfAbsent("Jewel Changi Canopy Park & Hedge Maze", singapore, family, null,
                AttractionType.ACTIVITY, "1599", 3, 50, "4.84", 534,
                "Sky nets, maze and Rain Vortex viewing at Changi Airport.",
                "https://images.unsplash.com/photo-1584017911766-9bf0591b3555?w=800&q=80");
        saveIfAbsent("Singapore Zoo + Breakfast with Orangutans", singapore, family, null,
                AttractionType.ATTRACTION, "4199", 5, 24, "4.90", 1102,
                "Open-concept zoo with optional jungle breakfast experience.",
                "https://images.unsplash.com/photo-1456274511679-8e10d964ed9d?w=800&q=80");

        saveIfAbsent("Grand Palace & Emerald Buddha Guided Tour", bangkok, culture, null,
                AttractionType.ATTRACTION, "1499", 3, 20, "4.73", 567,
                "Dress-code guided tour with river ferry option.",
                "https://images.unsplash.com/photo-1563492065599-3520f775eeed?w=800");
        saveIfAbsent("Chao Phraya Princess Dinner Cruise", bangkok, food, null,
                AttractionType.TOUR, "2299", 3, 35, "4.66", 289,
                "Buffet dinner with temple illuminations along the river.",
                "https://images.unsplash.com/photo-1563492065599-3520f775eeed?w=800");
        saveIfAbsent("Floating Market & Railway Market Day Trip", bangkok, culture, null,
                AttractionType.DAY_TRIP, "2799", 8, 16, "4.70", 178,
                "Damnoen Saduak and Maeklong market with hotel pickup.",
                "https://images.unsplash.com/photo-1552465011-b0e0d0d0d0d0?w=800");
        saveIfAbsent("Muay Thai Live Show at Asiatique", bangkok, family, null,
                AttractionType.ACTIVITY, "1999", 2, 40, "4.57", 142,
                "Stadium-style martial arts performance and night bazaar.",
                "https://images.unsplash.com/photo-1563492065599-3520f775eeed?w=800");

        saveIfAbsent("Tower of London & Crown Jewels Entry", london, culture, null,
                AttractionType.ATTRACTION, "4299", 3, 22, "4.81", 445,
                "Yeoman warder tour and Crown Jewels exhibition.",
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800");
        saveIfAbsent("Thames Hop-On Hop-Off River Pass - 24h", london, culture, null,
                AttractionType.TOUR, "2499", 8, 50, "4.62", 312,
                "Unlimited river rides between Westminster and Greenwich.",
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800");
        saveIfAbsent("Harry Potter Warner Bros Studio Tour", london, family, null,
                AttractionType.DAY_TRIP, "6999", 7, 18, "4.92", 1890,
                "Behind-the-scenes sets, costumes and Platform 9¾.",
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800");
        saveIfAbsent("West End Musical Matinee Ticket", london, culture, null,
                AttractionType.ACTIVITY, "5599", 3, 30, "4.75", 267,
                "Top musicals in Leicester Square theatres (subject to availability).",
                "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=800");

        saveIfAbsent("Ubud Rice Terrace & Monkey Forest Tour", bali, culture, null,
                AttractionType.TOUR, "1799", 6, 14, "4.69", 423,
                "Tegallalang terraces, sacred forest and coffee tasting.",
                "https://images.unsplash.com/photo-1537996194471-e657df962ab4?w=800");
        saveIfAbsent("Mount Batur Sunrise Trek with Breakfast", bali, adventure, null,
                AttractionType.ADVENTURE, "2999", 8, 12, "4.78", 356,
                "Guided volcano hike with eggs cooked on volcanic steam.",
                "https://images.unsplash.com/photo-1537996194471-e657df962ab4?w=800");
        saveIfAbsent("Nusa Penida Snorkelling Day Trip", bali, water, null,
                AttractionType.DAY_TRIP, "3499", 10, 16, "4.71", 198,
                "Crystal Bay and Manta Point with lunch on island.",
                "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800");
        saveIfAbsent("Uluwatu Kecak Fire Dance & Cliff Temple", bali, culture, null,
                AttractionType.ATTRACTION, "1299", 4, 25, "4.67", 289,
                "Sunset temple visit and traditional Kecak performance.",
                "https://images.unsplash.com/photo-1537996194471-e657df962ab4?w=800");

        saveIfAbsent("TeamLab Planets Tokyo Digital Art", tokyo, family, null,
                AttractionType.ATTRACTION, "3899", 2, 30, "4.89", 734,
                "Immersive barefoot art museum in Toyosu.",
                "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800");
        saveIfAbsent("Mt Fuji & Hakone Bullet Train Day Trip", tokyo, culture, null,
                AttractionType.DAY_TRIP, "7999", 12, 18, "4.84", 512,
                "Lake Ashi cruise, ropeway and Fuji 5th station photo stops.",
                "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800");
        saveIfAbsent("Shibuya & Harajuku Street Food Tour", tokyo, food, null,
                AttractionType.TOUR, "3299", 3, 10, "4.80", 201,
                "Takoyaki, ramen and crepe crawl with local guide.",
                "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800");
        saveIfAbsent("Tokyo Skytree Fast Ticket + Sumida Aquarium", tokyo, family, null,
                AttractionType.ATTRACTION, "3199", 4, 28, "4.76", 388,
                "Skip-the-line deck access plus aquarium combo.",
                "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?w=800");
    }

    private static String productImg(String seed) {
        String slug = seed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return "https://picsum.photos/seed/ww-" + slug + "/800/600";
    }

    /** Refresh all city and product image URLs to stable picsum seeds (on every startup when DB exists). */
    private void refreshCatalogImages() {
        cityRepository.findAll().forEach(c -> {
            if (c.getCode() == null || c.getCode().isBlank()) {
                return;
            }
            c.setImageUrl(productImg("city-" + c.getCode().toLowerCase(Locale.ROOT)));
            cityRepository.save(c);
        });
        attractionRepository.findAll().forEach(a -> {
            a.setImageUrl(productImg(imageSeedFromTitle(a.getTitle())));
            attractionRepository.save(a);
        });
        seedDefaultPaxTypes();
        System.out.println("=== Catalog images refreshed (all products & cities) ===");
    }

    /** Default ADULT/CHILD/INFANT tiers when vendor ingestion has not run yet. */
    private void seedDefaultPaxTypes() {
        attractionRepository.findAll().forEach(a -> {
            if (!paxTypeRepository.findByAttractionIdAndActiveTrueOrderByCodeAsc(a.getId()).isEmpty()) {
                return;
            }
            BigDecimal adult = a.getPrice();
            upsertPax(a, "ADULT", "Adult", 12, 99, adult);
            upsertPax(a, "CHILD", "Child", 3, 11, adult.multiply(new BigDecimal("0.75")));
            upsertPax(a, "INFANT", "Infant", 0, 2, BigDecimal.ZERO);
        });
    }

    private void upsertPax(Attraction attraction, String code, String label, int minAge, int maxAge, BigDecimal price) {
        paxTypeRepository.save(PaxType.builder()
                .attraction(attraction)
                .code(code)
                .label(label)
                .minAge(minAge)
                .maxAge(maxAge)
                .price(price)
                .currency(attraction.getCurrency())
                .active(true)
                .build());
    }

    private static boolean needsImageRefresh(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() || imageUrl.contains("images.unsplash.com/photo-");
    }

    private static String imageSeedFromTitle(String title) {
        return title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private void saveIfAbsent(String title, City city, Category category, User operator,
                              AttractionType type, String price, int hours, int maxGroup,
                              String rating, int reviews, String description, String imageUrl) {
        var existing = attractionRepository.findByTitleAndCity_Id(title, city.getId());
        if (existing.isPresent()) {
            Attraction a = existing.get();
            if (imageUrl != null && (needsImageRefresh(a.getImageUrl()) || !imageUrl.equals(a.getImageUrl()))) {
                a.setImageUrl(imageUrl);
                attractionRepository.save(a);
            }
            return;
        }
        attractionRepository.save(Attraction.builder()
                .title(title)
                .description(description)
                .type(type)
                .price(new BigDecimal(price))
                .currency("INR")
                .durationHours(hours)
                .maxGroupSize(maxGroup)
                .averageRating(new BigDecimal(rating))
                .reviewCount(reviews)
                .imageUrl(imageUrl)
                .active(true)
                .city(city)
                .category(category)
                .operator(operator)
                .build());
    }

    private void seedTimeSlotsForNewAttractions(int previousCount) {
        List<Attraction> all = attractionRepository.findAll();
        for (int i = previousCount; i < all.size(); i++) {
            Attraction a = all.get(i);
            if (timeSlotRepository.findByAttractionId(a.getId()).isEmpty()) {
                timeSlotRepository.save(TimeSlot.builder()
                        .attraction(a)
                        .slotDate(LocalDate.now().plusDays(2 + (i % 5)))
                        .startTime(LocalTime.of(9 + (i % 8), 0))
                        .totalSeats(a.getMaxGroupSize())
                        .availableSeats(a.getMaxGroupSize())
                        .build());
            }
        }
    }
}
