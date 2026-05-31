package com.neovarsity.toursattractions.entity;

import com.neovarsity.toursattractions.entity.enums.IngestionRunStatus;
import com.neovarsity.toursattractions.entity.enums.IngestionRunType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ingestion_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionRunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "run_type", length = 30)
    private IngestionRunType runType;

    @Column(name = "products_received")
    private Integer productsReceived;

    /** New attractions created in this run */
    @Column(name = "products_created")
    private Integer productsCreated;

    /** Existing attractions updated in this run */
    @Column(name = "products_updated")
    private Integer productsUpdated;

    @Column(name = "products_upserted")
    private Integer productsUpserted;

    @Column(name = "activities_upserted")
    private Integer activitiesUpserted;

    @Column(name = "pax_types_upserted")
    private Integer paxTypesUpserted;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
