package com.dede.ticketbooking.config;

import com.dede.ticketbooking.model.*;
import com.dede.ticketbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final EventCategoryRepository categoryRepository;
    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final SectionRepository sectionRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Data already initialized, skipping.");
            return;
        }

        log.info("=== Initializing seed data ===");

        // Roles
        Role adminRole = roleRepository.save(Role.builder().roleName("ADMIN").description("Quản trị viên").isSystemRole(1).build());
        Role organizerRole = roleRepository.save(Role.builder().roleName("ORGANIZER").description("Tổ chức sự kiện").isSystemRole(1).build());
        Role scannerRole = roleRepository.save(Role.builder().roleName("SCANNER").description("Soát vé").isSystemRole(1).build());
        Role customerRole = roleRepository.save(Role.builder().roleName("CUSTOMER").description("Khách hàng").isSystemRole(1).build());

        // Admin user
        String encodedPassword = passwordEncoder.encode("password123");
        User admin = userRepository.save(User.builder()
                .username("admin").passwordHash(encodedPassword).email("admin@dede.vn")
                .fullName("Admin Hệ Thống").phoneNumber("0901000001").gender("MALE").build());
        userRoleRepository.save(UserRole.builder().userId(admin.getUserId()).roleId(adminRole.getRoleId()).build());

        // Organizer
        User organizer = userRepository.save(User.builder()
                .username("organizer01").passwordHash(encodedPassword).email("organizer@dede.vn")
                .fullName("Nguyễn Văn Tổ Chức").phoneNumber("0901000002").gender("MALE").build());
        userRoleRepository.save(UserRole.builder().userId(organizer.getUserId()).roleId(organizerRole.getRoleId()).build());

        // Scanner
        User scanner = userRepository.save(User.builder()
                .username("scanner01").passwordHash(encodedPassword).email("scanner@dede.vn")
                .fullName("Trần Thị Soát Vé").phoneNumber("0901000003").gender("FEMALE").build());
        userRoleRepository.save(UserRole.builder().userId(scanner.getUserId()).roleId(scannerRole.getRoleId()).build());

        // Customer
        User customer = userRepository.save(User.builder()
                .username("customer01").passwordHash(encodedPassword).email("customer01@gmail.com")
                .fullName("Lê Văn Khách").phoneNumber("0901000004").gender("MALE").build());
        userRoleRepository.save(UserRole.builder().userId(customer.getUserId()).roleId(customerRole.getRoleId()).build());
        customerRepository.save(Customer.builder().userId(customer.getUserId()).customerCode("CUST-000001").build());

        // Event Categories
        EventCategory concert = categoryRepository.save(EventCategory.builder().categoryName("Concert").description("Buổi biểu diễn âm nhạc").build());
        EventCategory workshop = categoryRepository.save(EventCategory.builder().categoryName("Workshop").description("Hội thảo thực hành").build());
        EventCategory conference = categoryRepository.save(EventCategory.builder().categoryName("Conference").description("Hội nghị chuyên đề").build());

        // Venues
        Venue venue1 = venueRepository.save(Venue.builder()
                .venueName("Nhà hát Hòa Bình").address("240 Đường 3 Tháng 2, Quận 10")
                .city("TP.HCM").capacity(2500).venueType("INDOOR").hotline("028-3865-5050").build());
        Venue venue2 = venueRepository.save(Venue.builder()
                .venueName("Sân vận động Phú Thọ").address("1 Lữ Gia, Quận 11")
                .city("TP.HCM").capacity(50000).venueType("STADIUM").hotline("028-3855-7777").build());

        // Sample Event: Concert 50,000 vé
        LocalDateTime now = LocalDateTime.now();
        Event event1 = eventRepository.save(Event.builder()
                .category(concert).venue(venue2).organizerId(organizer.getUserId())
                .eventName("Dề Dê Music Festival 2026")
                .description("Lễ hội âm nhạc lớn nhất năm do Dề Dê tổ chức tại Sân vận động Phú Thọ")
                .startDatetime(now.plusDays(30)).endDatetime(now.plusDays(30).plusHours(6))
                .saleStart(now.minusDays(1)).saleEnd(now.plusDays(29))
                .status("ON_SALE").totalCapacity(50000).isSeatingChart(1).build());

        // Sections for event1
        Section vipSection = sectionRepository.save(Section.builder()
                .eventId(event1.getEventId()).sectionName("Khu VIP").totalSeats(500)
                .colorCode("#FFD700").sectionType("SEATED").positionX(0).positionY(0).build());
        Section sectionA = sectionRepository.save(Section.builder()
                .eventId(event1.getEventId()).sectionName("Khu A").totalSeats(2000)
                .colorCode("#4CAF50").sectionType("SEATED").positionX(100).positionY(0).build());
        Section standing = sectionRepository.save(Section.builder()
                .eventId(event1.getEventId()).sectionName("Standing").totalSeats(47500)
                .colorCode("#2196F3").sectionType("STANDING").positionX(0).positionY(100).build());

        // Ticket Types
        ticketTypeRepository.save(TicketType.builder()
                .eventId(event1.getEventId()).sectionId(vipSection.getSectionId())
                .typeName("VIP").price(BigDecimal.valueOf(2000000)).quantityTotal(500)
                .maxPerOrder(4).saleStart(now.minusDays(1)).saleEnd(now.plusDays(29)).build());
        ticketTypeRepository.save(TicketType.builder()
                .eventId(event1.getEventId()).sectionId(sectionA.getSectionId())
                .typeName("Khu A").price(BigDecimal.valueOf(1000000)).quantityTotal(2000)
                .maxPerOrder(6).saleStart(now.minusDays(1)).saleEnd(now.plusDays(29)).build());
        ticketTypeRepository.save(TicketType.builder()
                .eventId(event1.getEventId()).sectionId(standing.getSectionId())
                .typeName("Standing").price(BigDecimal.valueOf(500000)).quantityTotal(47500)
                .maxPerOrder(10).saleStart(now.minusDays(1)).saleEnd(now.plusDays(29)).build());

        // Second event: Workshop smaller
        Event event2 = eventRepository.save(Event.builder()
                .category(workshop).venue(venue1).organizerId(organizer.getUserId())
                .eventName("Workshop: Khởi Nghiệp 4.0")
                .description("Workshop thực hành về khởi nghiệp trong kỷ nguyên số")
                .startDatetime(now.plusDays(14)).endDatetime(now.plusDays(14).plusHours(4))
                .saleStart(now.minusDays(1)).saleEnd(now.plusDays(13))
                .status("ON_SALE").totalCapacity(200).isSeatingChart(0).build());

        Section workshopGA = sectionRepository.save(Section.builder()
                .eventId(event2.getEventId()).sectionName("General Admission").totalSeats(200)
                .colorCode("#9C27B0").sectionType("STANDING").positionX(0).positionY(0).build());

        ticketTypeRepository.save(TicketType.builder()
                .eventId(event2.getEventId()).sectionId(workshopGA.getSectionId())
                .typeName("Standard").price(BigDecimal.valueOf(300000)).quantityTotal(200)
                .maxPerOrder(5).saleStart(now.minusDays(1)).saleEnd(now.plusDays(13)).build());

        log.info("=== Seed data initialized: 2 events, 4 users, 4 roles ===");
    }
}
