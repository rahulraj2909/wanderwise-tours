package com.neovarsity.toursattractions.entity;

import com.neovarsity.toursattractions.entity.enums.IngestionChannel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionChannel ingestionChannel;

    /** Classpath mock feed path or API base URL in production */
    @Column(name = "feed_location", length = 300)
    private String feedLocation;

    @Column(nullable = false)
    private Boolean active;
}
