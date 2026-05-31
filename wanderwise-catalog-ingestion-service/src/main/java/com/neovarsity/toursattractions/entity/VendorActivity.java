package com.neovarsity.toursattractions.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vendor_activities", indexes = {
        @Index(name = "idx_vendor_activity_attraction", columnList = "attraction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attraction_id", nullable = false)
    private Attraction attraction;

    @Column(name = "vendor_activity_code", nullable = false, length = 80)
    private String vendorActivityCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active;
}
